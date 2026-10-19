// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.facebook

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
import vn.dtinh.patches.microGRoutingDependency

private const val OLD_EXT = "Lvn/dtinh/facebook/FacebookMicroGFcmSupport;"
private const val EXT = "Lvn/dtinh/facebook/FacebookMicroGFcmSupportV2;"
private const val SESSION = "Lcom/facebook/auth/usersession/FbUserSession;"
private const val PROVIDER = "Lcom/facebook/push/fcm/customprovider/FirebaseInitCustomProvider\$Impl;"
private const val IID = "Lcom/google/firebase/iid/FirebaseInstanceId;"
private const val OLD_STORE = "dtinh.microg.facebook.appid.v1"
private const val STORE = "dtinh.microg.facebook.appid.v2"

private val facebookManifest = resourcePatch {
    dependsOn(microGRoutingDependency)
    execute {
        require(packageMetadata.packageName == "com.facebook.katana" && packageMetadata.versionName == "573.0.0.37.74")
        document("AndroidManifest.xml").use { doc ->
            val receivers = doc.getElementsByTagName("receiver")
            val receiver = (0 until receivers.length).map { receivers.item(it) as Element }.single {
                it.getAttribute("android:name") == "com.google.firebase.iid.FirebaseInstanceIdReceiver"
            }
            require(receiver.getAttribute("android:permission") == "app.revanced.android.c2dm.permission.SEND")
            receiver.setAttribute("android:enabled", "true")
        }
    }
}

val facebookMicroGSupport = bytecodePatch(
    name = "Thông báo Facebook qua microG",
    description = "Nhận thông báo qua MicroG-RE. Cài chung được với Messenger.",
    default = false,
) {
    compatibleWith("com.facebook.katana"("573.0.0.37.74"))
    dependsOn(facebookManifest)
    extendWith("extensions/facebook-fcm.dex")
    execute {
        fun has(type: String, name: String, parameters: List<String>, result: String) =
            classDefByOrNull(type)?.methods?.any { it.name == name && it.returnType == result &&
                it.parameterTypes.map { p -> p.toString() } == parameters } == true
        require(has("LX/19m;", "A0H", listOf("Landroid/content/Context;"), SESSION) &&
            has("LX/86K;", "<init>", emptyList(), "V") &&
            has("LX/5GG;", "<init>", emptyList(), "V") &&
            has("LX/5GG;", "A00", listOf("Z", "Ljava/lang/String;"), "V") &&
            has("LX/86K;", "Cx6", listOf(SESSION, "Ljava/lang/String;"), "Z") &&
            classDefByOrNull("Lcom/facebook/auth/usersession/manager/FbUserSessionImpl;")?.fields?.any {
                it.name == "A02" && it.type == "Ljava/lang/String;"
            } == true) { "Facebook session/FCM layout changed; use 573.0.0.37.74." }
        // Require both modern-IPC endpoints as well as the existing legacy route.
        for ((type, name) in listOf("LX/cd1;" to "A02", "LX/fAd;" to "run")) {
            val route = classDefByOrNull(type)?.let { mutableClassDefBy(it) }?.methods?.singleOrNull { it.name == name }
                ?: error("Facebook IPC layout changed: $type->$name")
            val literals = route.implementation!!.instructions.mapNotNull {
                ((it as? ReferenceInstruction)?.reference as? StringReference)?.string
            }.toSet()
            require("app.revanced.android.gms" in literals && "com.google.android.gms" !in literals) {
                "Facebook modern IPC still targets the original Google backend: $type->$name"
            }
        }
        var stores = 0; var pending = 0; var startup = 0; var ready = 0; var checks = 0
        classDefForEach { original ->
            if (original.type !in setOf("LX/1eU;", "LX/1eX;", IID, "LX/1fs;", PROVIDER, "LX/86K;")) return@classDefForEach
            val mutable = mutableClassDefBy(original)
            for (method in mutable.methods.toList()) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                val calls = instructions.mapNotNull { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() }.toSet()
                val changes = mutableListOf<Pair<Int, () -> Unit>>()
                instructions.forEachIndexed { index, instruction ->
                    val ref = (instruction as? ReferenceInstruction)?.reference
                    val text = (ref as? StringReference)?.string
                    if (original.type in setOf("LX/1eU;", "LX/1eX;", IID) && text in
                        setOf("com.google.android.gms.appid", "com.google.android.gms.appid-no-backup", OLD_STORE, "$OLD_STORE-no-backup", STORE, "$STORE-no-backup")) {
                        stores++
                        if (text != STORE && text != "$STORE-no-backup") changes += index to {
                            method.replaceInstruction(index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                                (instruction as OneRegisterInstruction).registerA,
                                ImmutableStringReference(text!!.replace("com.google.android.gms.appid", STORE).replace(OLD_STORE, STORE))))
                        }
                    }
                    val call = (ref as? MethodReference)?.toString()
                    if (original.type == "LX/1fs;" && call in setOf(
                        "Landroid/app/PendingIntent;->getBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;",
                        "$EXT->pendingBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;",
                        "$OLD_EXT->pendingBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;")) {
                        pending++
                        if (!call!!.startsWith(EXT)) {
                            val args = instruction as FiveRegisterInstruction
                            changes += index to { method.replaceInstruction(index,
                                "invoke-static {v${args.registerC}, v${args.registerD}, v${args.registerE}, v${args.registerF}}, $EXT->pendingBroadcast(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;") }
                        }
                    }
                    if (original.type == PROVIDER && method.name == "A05") {
                        if (call in setOf("$EXT->boot(Landroid/content/Context;)V", "$OLD_EXT->boot(Landroid/content/Context;)V")) {
                            startup++
                            if (call!!.startsWith(OLD_EXT)) {
                                val register = (instruction as FiveRegisterInstruction).registerC
                                changes += index to { method.replaceInstruction(index, "invoke-static {v$register}, $EXT->boot(Landroid/content/Context;)V") }
                            }
                        }
                        if (call == "LX/1Xi;->A02(Landroid/content/Context;)Z" && calls.none { it in setOf("$EXT->boot(Landroid/content/Context;)V", "$OLD_EXT->boot(Landroid/content/Context;)V") }) {
                            val register = (instruction as FiveRegisterInstruction).registerC
                            val next = instructions.getOrNull(index + 1)?.opcode
                            val after = index + if (next in setOf(Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT_WIDE)) 2 else 1
                            changes += index to { method.addInstructions(after, "invoke-static {v$register}, $EXT->boot(Landroid/content/Context;)V") }
                            startup++
                        }
                    }
                    if (original.type == "LX/86K;" && method.name == "Cx7") {
                        // Replace only the FCM call; keep availability checks for other APIs intact.
                        if (call in setOf("$EXT->availability(Landroid/content/Context;)I", "$OLD_EXT->availability(Landroid/content/Context;)I")) {
                            checks++
                            if (call!!.startsWith(OLD_EXT)) {
                                val register = (instruction as FiveRegisterInstruction).registerC
                                changes += index to { method.replaceInstruction(index, "invoke-static {v$register}, $EXT->availability(Landroid/content/Context;)I") }
                            }
                        }
                        if (call == "LX/cck;->A04(Landroid/content/Context;I)I") {
                            val args = instruction as FiveRegisterInstruction
                            changes += index to { method.replaceInstruction(index,
                                "invoke-static {v${args.registerD}}, $EXT->availability(Landroid/content/Context;)I") }
                            checks++
                        }
                        if (call in setOf("$EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V", "$OLD_EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V")) {
                            ready++
                            if (call!!.startsWith(OLD_EXT)) {
                                val args = instruction as FiveRegisterInstruction
                                changes += index to { method.replaceInstruction(index,
                                    "invoke-static {v${args.registerC}, v${args.registerD}}, $EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V") }
                            }
                        }
                        if (call == "LX/1ki;->A05($SESSION" + "ILjava/lang/String;)V" && calls.none { it in setOf("$EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V", "$OLD_EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V") }) {
                            val args = instruction as FiveRegisterInstruction
                            changes += index to { method.addInstructions(index + 1,
                                "invoke-static {v${args.registerD}, v${args.registerF}}, $EXT->tokenReady(Ljava/lang/Object;Ljava/lang/String;)V") }
                            ready++
                        }
                    }
                }
                changes.sortedByDescending { it.first }.forEach { it.second() }

            }
        }
        require(stores == 4 && pending == 1 && startup == 1 && ready == 1 && checks == 1) {
            "Unexpected Facebook layout: stores=$stores pending=$pending startup=$startup ready=$ready checks=$checks."
        }
    }
}
