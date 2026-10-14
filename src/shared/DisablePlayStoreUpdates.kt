/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2470
 *
 * Original code hard forked from:
 * https://github.com/ReVanced/revanced-patches/blob/724e6d61b2ecd868c1a9a37d465a688e83a74799/patches/src/main/kotlin/app/revanced/patches/all/misc/versioncode/ChangeVersionCodePatch.kt
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

// SPDX-License-Identifier: GPL-3.0-only
// Adapted by DTinh on 2026-10-14 for Patcher 1.14 and the two supported apps.
// Keep original file notice above. Source commit: e5464395441e916b643af4e2175c4e253e6df487.
package vn.dtinh.patches

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
import org.w3c.dom.Element

private const val EXT = "Lvn/dtinh/updates/PlayStoreUpdates;"
private const val SAVED = "vn.dtinh.updates.ORIGINAL_VERSION_CODE"
private var originalVersionCode = 0
private var originalPackageName = ""

internal fun originalUpdateVersion(current: String, saved: String?): Int {
    val code = current.toIntOrNull()
    require(code != null && code > 0) { "Mã phiên bản ứng dụng không hợp lệ." }
    if (code != Int.MAX_VALUE) {
        require(saved == null || saved.toIntOrNull() == code) {
            "Thông tin phiên bản gốc không khớp; hãy dùng APK gốc."
        }
        return code
    }
    return saved?.toIntOrNull()?.also {
        require(it > 0 && it < Int.MAX_VALUE) { "Không xác định được phiên bản gốc; hãy dùng APK gốc." }
    } ?: error("APK đã đổi mã phiên bản nhưng thiếu thông tin gốc; hãy dùng APK gốc.")
}

private val disableUpdatesManifest = resourcePatch {
    execute {
        require(packageMetadata.packageName in setOf("com.facebook.orca", "com.zing.zalo"))
        originalPackageName = packageMetadata.packageName
        document("AndroidManifest.xml").use { doc ->
            val root = doc.documentElement
            val app = doc.getElementsByTagName("application").item(0) as Element
            val nodes = app.getElementsByTagName("meta-data")
            val saved = (0 until nodes.length).map { nodes.item(it) as Element }
                .filter { it.getAttribute("android:name") == SAVED }
            require(saved.size <= 1) { "Thông tin phiên bản gốc bị trùng." }
            originalVersionCode = originalUpdateVersion(root.getAttribute("android:versionCode"),
                saved.singleOrNull()?.getAttribute("android:value"))
            val node = saved.singleOrNull() ?: doc.createElement("meta-data").also { app.appendChild(it) }
            node.setAttribute("android:name", SAVED)
            node.removeAttribute("android:resource")
            node.setAttribute("android:value", originalVersionCode.toString())
        }
    }
    finalize {
        document("AndroidManifest.xml").use { doc ->
            doc.documentElement.setAttribute("android:versionCode", Int.MAX_VALUE.toString())
        }
    }
}

// DEX move-result uses an 8-bit register: iget's destination is already encodable.
// invoke-static/range also works when the receiver and destination are the same register.
internal fun rewriteUpdateVersionReads(method: MutableMethod): Int {
    val instructions = method.implementation?.instructions?.toList() ?: return 0
    var changed = 0
    for (index in instructions.indices.reversed()) {
        val instruction = instructions[index]
        val reference = (instruction as? ReferenceInstruction)?.reference
        if (instruction.opcode == Opcode.IGET && (reference as? FieldReference)?.toString() ==
            "Landroid/content/pm/PackageInfo;->versionCode:I") {
            val args = instruction as TwoRegisterInstruction
            method.replaceInstruction(index,
                "invoke-static/range {v${args.registerB} .. v${args.registerB}}, $EXT->getVersionCode(Landroid/content/pm/PackageInfo;)I")
            method.addInstructions(index + 1, "move-result v${args.registerA}")
            changed++
        } else if (instruction.opcode in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE) &&
            (reference as? MethodReference)?.toString() == "Landroid/content/pm/PackageInfo;->getLongVersionCode()J") {
            val register = when (instruction) {
                is FiveRegisterInstruction -> instruction.registerC
                is RegisterRangeInstruction -> instruction.startRegister
                else -> error("Dạng lệnh đọc phiên bản không được hỗ trợ.")
            }
            // A call with an ignored result needs no extra instruction.
            method.replaceInstruction(index,
                "invoke-static/range {v$register .. v$register}, $EXT->getVersionCodeLong(Landroid/content/pm/PackageInfo;)J")
            changed++
        }
    }
    return changed
}

// Unnamed dependency: enabled automatically inside the Messenger and Zalo FCM choices.
internal val disablePlayStoreUpdatesDependency = bytecodePatch(default = false) {
    dependsOn(disableUpdatesManifest)
    extendWith("extensions/updates-fcm.dex")
    finalize {
        val helper = mutableClassDefBy(classDefByOrNull(EXT) ?: error("Thiếu phần giữ phiên bản gốc."))
        val method = helper.methods.single { it.name == "originalVersionCode" }
        require((method.implementation?.registerCount ?: 0) >= 1)
        method.replaceInstruction(0, "const v0, 0x${originalVersionCode.toString(16)}")
        val packageMethod = helper.methods.single { it.name == "originalPackageName" }
        require((packageMethod.implementation?.registerCount ?: 0) >= 1)
        packageMethod.replaceInstruction(0, "const-string v0, \"$originalPackageName\"")
        classDefForEach { original ->
            if (original.type.startsWith("Lvn/dtinh/")) return@classDefForEach
            for (source in original.methods) {
                val relevant = source.implementation?.instructions?.any {
                    val ref = (it as? ReferenceInstruction)?.reference
                    (it.opcode == Opcode.IGET && (ref as? FieldReference)?.toString() ==
                        "Landroid/content/pm/PackageInfo;->versionCode:I") ||
                    (it.opcode in setOf(Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE) &&
                        (ref as? MethodReference)?.toString() == "Landroid/content/pm/PackageInfo;->getLongVersionCode()J")
                } == true
                if (relevant) {
                    val mutable = mutableClassDefBy(original).methods.single {
                        it.name == source.name && it.parameterTypes == source.parameterTypes && it.returnType == source.returnType
                    }
                    rewriteUpdateVersionReads(mutable)
                }
            }
        }
    }
}

