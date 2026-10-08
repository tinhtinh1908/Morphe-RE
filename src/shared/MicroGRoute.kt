// Copyright 2026. SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.encodedValue.MutableStringEncodedValue
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element
import java.security.MessageDigest
import java.util.logging.Logger

private const val VENDOR = "app.revanced"
private const val MICROG = "$VENDOR.android.gms"
private val log = Logger.getLogger("DTinh.FcmMicroG")
private val fcmNames = listOf(
    "com.google.android.c2dm.intent.REGISTER",
    "com.google.android.c2dm.intent.UNREGISTER",
    "com.google.android.c2dm.intent.REGISTRATION",
    "com.google.android.c2dm.intent.RECEIVE",
    "com.google.android.c2dm.permission.RECEIVE",
    "com.google.android.c2dm.permission.SEND",
    "com.google.android.gcm.intent.SEND",
    "com.google.iid.TOKEN_REQUEST"
)
private val redirects = fcmNames.associateWith { it.replaceFirst("com.google", VENDOR) } +
    mapOf("com.google.android.gms" to MICROG)

internal fun redirect(value: String): String? = redirects[value]

private var hasExistingRoute = false

// Resource dependency is unnamed so the user selects one public patch only.
private val manifestPatch = resourcePatch {


    execute {
        val original = packageMetadata.packageName
        require((original == "com.facebook.orca" && packageMetadata.versionName == "573.0.0.44.88") ||
            (original == "com.zing.zalo" && packageMetadata.versionName == "26.08.01")) {
            "Only the supported Messenger and Zalo versions have dedicated patches."
        }
        require(original != "com.google.android.gms" && original != MICROG) {
            "Patch a CLIENT app, not Google Play services or MicroG."
        }
        // Reuse the original spoofed signer when input was already routed; never replace it with a patched APK signer.
        var savedSignature: String? = null
        hasExistingRoute = false
        document("AndroidManifest.xml").use { doc ->
            val nodes = doc.getElementsByTagName("meta-data")
            val values = (0 until nodes.length).associate {
                val e = nodes.item(it) as Element
                e.getAttribute("android:name") to e.getAttribute("android:value")
            }
            if (values["app.revanced.MICROG_PACKAGE_NAME"] == MICROG) {
                val signature = values["$MICROG.SPOOFED_PACKAGE_SIGNATURE"]
                require(values["$MICROG.SPOOFED_PACKAGE_NAME"] == original &&
                    signature?.matches(Regex("[a-fA-F0-9]{40}")) == true) {
                    "Existing microG routing has missing or invalid original signer metadata. Use the original input APK."
                }
                savedSignature = signature
                hasExistingRoute = true
            }
        }
        val digest = savedSignature ?: run {
            val candidates = packageMetadata.signingCertificates.values.flatten().map {
                MessageDigest.getInstance("SHA-1").digest(it.encoded)
                    .joinToString("") { b -> "%02x".format(b.toInt() and 255) }
            }.distinct()
            require(candidates.size == 1) {
                "Cannot select one original signer. Use an original signed APK or an input with valid original signer metadata."
            }
            candidates.single()
        }

        document("AndroidManifest.xml").use { doc ->
            val root = doc.documentElement
            val app = doc.getElementsByTagName("application").item(0) as? Element
                ?: error("No application element")
            val all = doc.getElementsByTagName("*")
            var receiverCount = 0
            // Only routing names and sender permissions. Never rewrite component class names,
            // Firebase's MESSAGING_EVENT, resource identifiers, API keys or project/sender IDs.
            for (i in 0 until all.length) {
                val node = all.item(i) as Element
                val name = node.getAttribute("android:name")
                if (node.tagName in setOf("action", "uses-permission", "permission", "package")) {
                    redirect(name)?.let { replacement ->
                        node.setAttribute("android:name", replacement)
                        if (name == "com.google.android.c2dm.intent.RECEIVE") receiverCount++
                    }
                }
                redirect(node.getAttribute("android:permission"))?.let {
                    node.setAttribute("android:permission", it)
                }
            }
            var queries = doc.getElementsByTagName("queries").item(0) as? Element
            if (queries == null) {
                queries = doc.createElement("queries")
                root.appendChild(queries)
            }
            val packages = queries.getElementsByTagName("package")
            if ((0 until packages.length).none {
                    (packages.item(it) as Element).getAttribute("android:name") == MICROG
                }) {
                val pkg = doc.createElement("package")
                pkg.setAttribute("android:name", MICROG)
                queries.appendChild(pkg)
            }
            fun metadata(key: String, value: String) {
                val nodes = app.getElementsByTagName("meta-data")
                val existing = (0 until nodes.length).map { nodes.item(it) as Element }
                    .firstOrNull { it.getAttribute("android:name") == key }
                val node = existing ?: doc.createElement("meta-data").also { app.appendChild(it) }
                node.setAttribute("android:name", key)
                node.removeAttribute("android:resource")
                node.setAttribute("android:value", value)
            }
            metadata("$MICROG.SPOOFED_PACKAGE_NAME", original)
            metadata("$MICROG.SPOOFED_PACKAGE_SIGNATURE", digest)
            metadata("app.revanced.MICROG_PACKAGE_NAME", MICROG)
            log.info("Manifest: $receiverCount FCM receive filters redirected; app package preserved: $original")
        }
    }
}

// Internal routing dependency. Only the two version-scoped app patches are selectable.
internal val microGRoutingDependency = bytecodePatch(default = false) {
    dependsOn(manifestPatch)
    execute {
        var strings = 0
        var fields = 0
        var fcmRoutes = 0
        var checks = 0
        var hasFirebaseMessaging = false
        var existingFcmRoutes = 0
        classDefForEach { original ->
            if (original.type.startsWith("Lcom/google/firebase/messaging/")) hasFirebaseMessaging = true
            // Zalo 26.08.01: restrict changes to its verified FCM transport and SDK helpers.
            // Already routed input may contain changes from older bundles outside this scope;
            // preserve them instead of guessing how another patch configured auth/ads/ML APIs.
            if (packageMetadata.packageName == "com.zing.zalo" &&
                !(original.type.startsWith("Lcom/google/firebase/messaging/") ||
                  original.type.startsWith("Lcom/google/firebase/iid/") ||
                  original.type.startsWith("Ljf/") || original.type.startsWith("Lx9/") ||
                  original.type.startsWith("Ly9/") || original.type in setOf("Lpj/g;", "Lka/a;", "Lj20/e;"))) return@classDefForEach
            val pendingMethods = original.methods.mapNotNull { method ->
                val impl = method.implementation ?: return@mapNotNull null
                val originalInstructions = impl.instructions.toList()
                val literals = originalInstructions.mapNotNull {
                    ((it as? ReferenceInstruction)?.reference as? StringReference)?.string
                }.toSet()
                existingFcmRoutes += literals.count { it in fcmNames.map { name -> name.replaceFirst("com.google", VENDOR) } }
                val knownCheck = method.returnType == "I" &&
                    method.parameterTypes.map { it.toString() } == listOf("Landroid/content/Context;", "I") &&
                    method.accessFlags and AccessFlags.STATIC.value != 0 &&
                    literals.containsAll(listOf("This should never happen.", "MetadataValueReader", "com.google.android.gms"))
                val replacements = originalInstructions.mapIndexedNotNull { index, instruction ->
                    if (instruction.opcode != Opcode.CONST_STRING && instruction.opcode != Opcode.CONST_STRING_JUMBO)
                        return@mapIndexedNotNull null
                    val value = ((instruction as ReferenceInstruction).reference as? StringReference)?.string
                        ?: return@mapIndexedNotNull null
                    redirect(value)?.let { Triple(index, instruction as OneRegisterInstruction, it) }
                }
                if (replacements.isEmpty() && !(knownCheck )) null
                else Triple(method, replacements, knownCheck)
            }
            val pendingFields = original.staticFields.filter {
                redirect((it.initialValue as? com.android.tools.smali.dexlib2.iface.value.StringEncodedValue)?.value ?: "") != null
            }
            if (pendingMethods.isNotEmpty() || pendingFields.isNotEmpty()) {
                val mutable = mutableClassDefBy(original)
                pendingMethods.forEach { (source, changes, knownCheck) ->
                    val method = mutable.methods.first { it.name == source.name && it.parameterTypes == source.parameterTypes && it.returnType == source.returnType }
                    changes.forEach { (index, instruction, replacement) ->
                        // Jumbo handles both opcodes without overflowing a 16-bit string index.
                        method.replaceInstruction(index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                            instruction.registerA, ImmutableStringReference(replacement)))
                        strings++
                        if (replacement != MICROG) fcmRoutes++
                    }
                    if (knownCheck ) {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                        checks++
                    }
                }
                pendingFields.forEach { source ->
                    val field = mutable.staticFields.first { it.name == source.name && it.type == source.type }
                    val value = field.initialValue as MutableStringEncodedValue
                    val replacement = redirect(value.value)!!
                    value.setValue(replacement)
                    fields++
                    if (replacement != MICROG) fcmRoutes++
                }
            }
        }
        require(fcmRoutes > 0 || (hasFirebaseMessaging && strings + fields > 0) || (hasExistingRoute && hasFirebaseMessaging && existingFcmRoutes > 0)) {
            "No supported FCM route found. This app needs a specific patch; no output should be installed."
        }
        if (hasExistingRoute && strings + fields == 0) log.info("Existing microG FCM route verified; original spoofed signature preserved.")
        log.info("DEX: $strings literals, $fields static fields, $fcmRoutes FCM routes, $checks known availability checks patched")
        if (checks == 0) log.warning("No known availability fingerprint found. Runtime GMS signature checks may still reject MicroG.")
        log.warning("Experimental: a successful patch does not confirm FCM token registration or push delivery.")
    }
}
