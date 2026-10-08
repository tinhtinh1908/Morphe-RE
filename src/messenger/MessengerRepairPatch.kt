// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.messenger

import vn.dtinh.patches.fcmMicroGPatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.*
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element

private const val EXT = "Lvn/dtinh/messenger/MicroGFcmSupport;"
private const val IID = "Lcom/google/firebase/iid/FirebaseInstanceId;"
private const val RECEIVER = "com.google.firebase.iid.FirebaseInstanceIdReceiver"

private val repairManifest = resourcePatch {
    dependsOn(fcmMicroGPatch)
    execute {
        require(packageMetadata.packageName == "com.facebook.orca" && packageMetadata.versionName == "573.0.0.44.88") {
            "This repair was verified only against Messenger 573.0.0.44.88."
        }
        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as Element
            val metadata = doc.getElementsByTagName("meta-data")
            require((0 until metadata.length).any {
                val e = metadata.item(it) as Element
                e.getAttribute("android:name") == "app.revanced.MICROG_PACKAGE_NAME" &&
                    e.getAttribute("android:value") == "app.revanced.android.gms"
            }) { "FCM routing dependency did not produce the required microG metadata." }
            val receivers = doc.getElementsByTagName("receiver")
            val matched = (0 until receivers.length).map { receivers.item(it) as Element }
                .filter { it.getAttribute("android:name") == RECEIVER }
            require(matched.size == 1) { "Expected one Firebase IID receiver." }
            require(matched.single().getAttribute("android:permission") == "app.revanced.android.c2dm.permission.SEND") {
                "Firebase receiver is not routed to MicroG-RE."
            }
            matched.single().setAttribute("android:enabled", "true")
            require((0 until doc.getElementsByTagName("activity").length).none {
                (doc.getElementsByTagName("activity").item(it) as Element).getAttribute("android:name") == "vn.dtinh.messenger.DiagnosticsActivity"
            }) { "Use the input APK from before the old diagnostics repair, so its injected UI and hooks are absent." }

        }
    }
}

val messengerMicroGRepair = bytecodePatch(
    name = "Messenger microG FCM support",
    description = "Messenger 573: route FCM to MicroG-RE, isolate IID tokens, fix PendingIntent and start Messenger's registration flow.",
    default = false,
) {
    compatibleWith("com.facebook.orca"("573.0.0.44.88"))
    dependsOn(repairManifest)
    extendWith("extensions/messenger-fcm.dex")
    execute {
        var stores = 0; var pending = 0; var startup = 0
        classDefForEach { original ->
            if (original.type !in setOf("LX/1fR;", IID, "LX/1hL;",
                "Lcom/facebook/push/fcm/customprovider/FirebaseInitCustomProvider\$Impl;")) return@classDefForEach
            val mutable = mutableClassDefBy(original)
            for (method in mutable.methods.toList()) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                val changes = mutableListOf<Pair<Int, () -> Unit>>()
                instructions.forEachIndexed { index, instruction ->
                    val ref = (instruction as? ReferenceInstruction)?.reference
                    val text = (ref as? StringReference)?.string
                    if (original.type in setOf("LX/1fR;", IID) && text in setOf("com.google.android.gms.appid", "com.google.android.gms.appid-no-backup")) {
                        val replacement = text!!.replace("com.google.android.gms.appid", "dtinh.microg.firebase.appid.v2")
                        changes += index to {
                            method.replaceInstruction(index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                                (instruction as OneRegisterInstruction).registerA, ImmutableStringReference(replacement)))
                            stores++
                        }
                    }
                    val call = (ref as? MethodReference)?.toString()
                    if (original.type == "LX/1hL;" && call == "Landroid/app/PendingIntent;->getBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;") {
                        val args = instruction as FiveRegisterInstruction
                        changes += index to {
                            method.replaceInstruction(index, "invoke-static {v${args.registerC}, v${args.registerD}, v${args.registerE}, v${args.registerF}}, $EXT->pendingBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;")
                            pending++
                        }
                    }
                    if (original.type.endsWith("FirebaseInitCustomProvider\$Impl;") && call == "LX/1cw;->A02(Landroid/content/Context;)Z") {
                        val register = (instruction as FiveRegisterInstruction).registerC
                        changes += index to { method.addInstructions(index + 1, "invoke-static {v$register}, $EXT->boot(Landroid/content/Context;)V"); startup++ }
                    }

                }
                // Apply from the end so earlier insertions do not shift later source indices.
                changes.sortedByDescending { it.first }.forEach { it.second() }

            }
        }
        require(stores == 3 && pending == 1 && startup == 1) {
            "Unexpected Messenger layout: stores=$stores pending=$pending startup=$startup. Use the input before the older Messenger repair."
        }
    }
}
