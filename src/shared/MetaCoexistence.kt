// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element

internal fun metaPermissionRoute(pkg: String, value: String): String? {
    if (pkg !in setOf("com.facebook.katana", "com.facebook.orca")) return null
    return when (value) {
        "com.facebook.permission.prod.FB_APP_COMMUNICATION" -> "$pkg.dtinh.permission.prod.FB_APP_COMMUNICATION"
        "com.facebook.permission.%s.FB_APP_COMMUNICATION" -> "$pkg.dtinh.permission.%s.FB_APP_COMMUNICATION"
        "com.facebook.receiver.permission.ACCESS" -> "$pkg.dtinh.receiver.permission.ACCESS"
        else -> null
    }
}

internal val metaCoexistenceManifest = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val pkg = packageMetadata.packageName
            if (pkg !in setOf("com.facebook.katana", "com.facebook.orca")) return@use
            val all = doc.getElementsByTagName("*")
            for (i in 0 until all.length) {
                val node = all.item(i) as Element
                if (node.tagName in setOf("permission", "uses-permission", "uses-permission-sdk-23")) {
                    metaPermissionRoute(pkg, node.getAttribute("android:name"))?.let { node.setAttribute("android:name", it) }
                }
                for (attribute in listOf("android:permission", "android:readPermission", "android:writePermission")) {
                    metaPermissionRoute(pkg, node.getAttribute(attribute))?.let { node.setAttribute(attribute, it) }
                }
            }
            // Keep signature protection, component names and provider authorities unchanged.
            val declared = doc.getElementsByTagName("permission")
            val names = (0 until declared.length).map { (declared.item(it) as Element).getAttribute("android:name") }
            require(listOf("$pkg.dtinh.permission.prod.FB_APP_COMMUNICATION", "$pkg.dtinh.receiver.permission.ACCESS").all { it in names }) {
                "Missing Meta permission declarations; use the supported original or previously routed APK."
            }
        }
    }
}

internal val metaCoexistence = bytecodePatch(default = false) {
    dependsOn(metaCoexistenceManifest)
    execute {
        val pkg = packageMetadata.packageName
        if (pkg !in setOf("com.facebook.katana", "com.facebook.orca")) return@execute
        classDefForEach { original ->
            val edits = original.methods.mapNotNull { method ->
                val changes = method.implementation?.instructions?.mapIndexedNotNull { index, instruction ->
                    val value = ((instruction as? ReferenceInstruction)?.reference as? StringReference)?.string
                        ?: return@mapIndexedNotNull null
                    val replacement = metaPermissionRoute(pkg, value) ?: return@mapIndexedNotNull null
                    require(instruction.opcode in setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO))
                    Triple(index, (instruction as OneRegisterInstruction).registerA, replacement)
                }.orEmpty()
                if (changes.isEmpty()) null else method to changes
            }
            val fields = original.staticFields.filter { metaPermissionRoute(pkg, (it.initialValue as? StringEncodedValue)?.value ?: "") != null }
            if (edits.isEmpty() && fields.isEmpty()) return@classDefForEach
            val mutable = mutableClassDefBy(original)
            for ((source, changes) in edits) {
                val target = mutable.methods.single { it.name == source.name && it.parameterTypes == source.parameterTypes && it.returnType == source.returnType }
                for ((index, register, replacement) in changes) target.replaceInstruction(index,
                    BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, register, ImmutableStringReference(replacement)))
            }
            for (source in fields) {
                val value = mutable.staticFields.single { it.name == source.name && it.type == source.type }.initialValue as MutableStringEncodedValue
                value.setValue(metaPermissionRoute(pkg, value.value)!!)
            }
        }
    }
}
