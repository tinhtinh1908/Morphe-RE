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
// Adapted by DTinh on 2026-10-14 for Patcher 1.14: selectable patch for Messenger/Zalo.
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

private const val EXT = "Lvn/dtinh/updates/PlayStoreUpdates;"
private var originalVersionCode = 0

private val disableUpdatesManifest = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            originalVersionCode = doc.documentElement.getAttribute("android:versionCode").toInt()
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
            if (instructions.getOrNull(index + 1)?.opcode != Opcode.MOVE_RESULT_WIDE) continue
            // Keep the original move-result-wide, as Morphe does.
            method.replaceInstruction(index,
                "invoke-static/range {v$register .. v$register}, $EXT->getVersionCodeLong(Landroid/content/pm/PackageInfo;)J")
            changed++
        }
    }
    return changed
}

// Standalone selectable patch, using Morphe's version-code and helper behavior.
val disablePlayStoreUpdatesPatch = bytecodePatch(
    name = "Chặn cập nhật CH Play",
    description = "Giữ bản đã vá, không cho CH Play cập nhật. Có thể gây lỗi với một số ứng dụng.",
    default = false,
) {
    compatibleWith("com.facebook.orca"("573.0.0.44.88"), "com.zing.zalo"("26.08.01"))
    dependsOn(disableUpdatesManifest)
    extendWith("extensions/updates-fcm.dex")
    finalize {
        val helper = mutableClassDefBy(classDefByOrNull(EXT) ?: error("Thiếu phần giữ phiên bản gốc."))
        val method = helper.methods.single { it.name == "originalVersionCode" }
        require((method.implementation?.registerCount ?: 0) >= 1)
        method.replaceInstruction(0, "const v0, 0x${originalVersionCode.toString(16)}")
        classDefForEach { original ->
            if (original.type.startsWith("Lvn/dtinh/") || original.type.startsWith("Lapp/morphe/extension/")) return@classDefForEach
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

