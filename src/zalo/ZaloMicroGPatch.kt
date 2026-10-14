// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.zalo

import vn.dtinh.patches.microGRoutingDependency
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

private const val EXT = "Lvn/dtinh/zalo/ZaloMicroGSupportV6;"
private const val STORE = "dtinh.microg.zalo.fcm.v1"
private val zaloManifest = resourcePatch {
    dependsOn(microGRoutingDependency)
    execute {
        require(packageMetadata.packageName == "com.zing.zalo" && packageMetadata.versionName == "26.08.01") {
            "This patch supports Zalo 26.08.01 only."
        }
        document("AndroidManifest.xml").use { doc ->
            val receivers = doc.getElementsByTagName("receiver")
            val receiver = (0 until receivers.length).map { receivers.item(it) as Element }
                .single { it.getAttribute("android:name") == "com.google.firebase.iid.FirebaseInstanceIdReceiver" }
            require(receiver.getAttribute("android:permission") == "app.revanced.android.c2dm.permission.SEND") {
                "Zalo FCM receiver has not been routed to MicroG-RE."
            }
            receiver.setAttribute("android:enabled", "true")
            val services = doc.getElementsByTagName("service")
            require((0 until services.length).count {
                (services.item(it) as Element).getAttribute("android:name") == "com.zing.zalo.service.ZaloFirebaseMessagingService"
            } == 1) { "Zalo's original FCM service is missing or ambiguous." }
        }
    }
}

val zaloMicroGSupport = bytecodePatch(
    name = "Zalo microG FCM support",
    description = "Zalo 26.08.01: chuyển FCM sang MicroG-RE, tách bộ nhớ token và thử lại luồng đăng ký sẵn có khi đã đăng nhập.",
    default = false,
) {
    compatibleWith("com.zing.zalo"("26.08.01"))
    dependsOn(zaloManifest)
    extendWith("extensions/zalo-fcm.dex")
    execute {
        fun has(type: String, name: String, parameters: List<String>, result: String) =
            classDefByOrNull(type)?.methods?.any { it.name == name &&
                it.parameterTypes.map { p -> p.toString() } == parameters && it.returnType == result } == true
        val context = "Landroid/content/Context;"
        val cloud = "Lyi0/a;"
        require(has("Lxo1/k;", "f", emptyList(), "Z") &&
            has("Lyi0/d;", "a", emptyList(), "Lyi0/f;") &&
            has("Lyi0/f;", "a", listOf(context), "Ljava/lang/String;") &&
            has("Lyi0/f;", "b", emptyList(), cloud) &&
            has("Lyi0/f;", "e", listOf(cloud, context, "Ljava/lang/String;"), "V") &&
            has("Lxi0/b;", "a", listOf(context), "Ljava/lang/String;") &&
            has("Lxi0/e;", "a", listOf("Ljava/lang/String;"), "V") &&
            classDefByOrNull("Lcom/zing/zalocore/CoreUtility;")?.fields?.any { it.name == "i" && it.type == "Ljava/lang/String;" } == true &&
            classDefByOrNull("Lyi0/f;")?.fields?.any { it.name == "a" && it.type == "Lx5/c;" } == true &&
            classDefByOrNull(cloud)?.fields?.any { it.name == "FIREBASE" && it.type == cloud } == true) {
            "Zalo login/provider/submission layout changed. Use the supported input APK."
        }
        var stores = 0; var boot = 0; var ready = 0
        classDefForEach { original ->
            if (original.type !in setOf("Lei0/p;", "Ljf/w;", "Lff/c;", "Lcom/zing/zalo/MainApplication;", "Lyi0/f;")) return@classDefForEach
            val mutable = mutableClassDefBy(original)
            for (method in mutable.methods.toList()) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                val calls = instructions.mapNotNull { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.toString() }
                val changes = mutableListOf<Pair<Int, () -> Unit>>()
                instructions.forEachIndexed { index, instruction ->
                    val ref = (instruction as? ReferenceInstruction)?.reference
                    val text = (ref as? StringReference)?.string
                    if (original.type in setOf("Lei0/p;", "Ljf/w;", "Lff/c;")) {
                        if (text in setOf("$STORE", "$STORE-no-backup")) stores++
                        if (text in setOf("com.google.android.gms.appid", "com.google.android.gms.appid-no-backup")) {
                            val replacement = text!!.replace("com.google.android.gms.appid", STORE)
                            changes += index to {
                                method.replaceInstruction(index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                                    (instruction as OneRegisterInstruction).registerA, ImmutableStringReference(replacement)))
                                stores++
                            }
                        }
                    }
                    val call = (ref as? MethodReference)?.toString()
                    if (original.type == "Lcom/zing/zalo/MainApplication;" && method.name == "onCreate") {
                        if (call == "$EXT->boot(Landroid/content/Context;)V") boot++
                        if (call == "Lcom/zing/zalo/startup/StartupApplication;->onCreate()V" && "$EXT->boot(Landroid/content/Context;)V" !in calls) {
                            val contextRegister = (instruction as FiveRegisterInstruction).registerC
                            changes += index to { method.addInstructions(index + 1, "invoke-static/range {v$contextRegister .. v$contextRegister}, $EXT->boot(Landroid/content/Context;)V"); boot++ }
                        }
                    }
                    if (original.type == "Lyi0/f;" && method.name == "e") {
                        if (call == "$EXT->tokenReady(Ljava/lang/String;)V") ready++
                        if (call == "Lxi0/e;->a(Ljava/lang/String;)V" && "$EXT->tokenReady(Ljava/lang/String;)V" !in calls) {
                            val token = (instruction as FiveRegisterInstruction).registerC
                            changes += index to { method.addInstructions(index + 1, "invoke-static/range {v$token .. v$token}, $EXT->tokenReady(Ljava/lang/String;)V"); ready++ }
                        }
                    }
                }
                changes.sortedByDescending { it.first }.forEach { it.second() }
            }
        }
        require(stores == 4 && boot == 1 && ready == 1) {
            "Unexpected Zalo FCM layout: stores=$stores boot=$boot ready=$ready. No output should be installed."
        }
    }
}
