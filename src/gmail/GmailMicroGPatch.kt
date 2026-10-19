// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.gmail

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31i
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass

private data class StringEdit(val index: Int, val register: Int, val value: String)

val gmailMicroGSupport = bytecodePatch(
    name = "Gmail qua microG",
    description = "Đăng nhập và nhận thông báo qua MicroG-RE. Chỉ hỗ trợ Gmail 2026.09.21.992487546.Release.",
    default = false,
) {
    compatibleWith("com.google.android.gm"(VERSION))
    dependsOn(gmailManifest)
    extendWith("extensions/gmail-fcm.dex")
    execute {
        val auth = classDefByOrNull("Laboa;") ?: error("Gmail auth layout changed")
        require(auth.methods.any { it.name == "b" && it.returnType == "Lcom/google/android/gms/auth/TokenData;" } &&
            auth.methods.any { it.name == "m" && it.parameterTypes.map { p -> p.toString() } == listOf("Landroid/accounts/Account;") }) { "Gmail token/account layout changed" }
        require(classDefByOrNull("Labnv;") != null && classDefByOrNull("Lzlp;") != null && classDefByOrNull("Lcom/google/android/libraries/hub/firebase/FirebaseMessagingServiceImpl;") != null) { "Gmail OAuth/Chime/FCM layout changed" }
        var accountClassifiers = 0
        var feedbackActions = 0
        var chimeraAuthorities = 0
        var accounts = 0; var stores = 0; var authClass = 0; var checks = 0; var fcm = 0
        classDefForEach { original ->
            if (original.type.startsWith("Lvn/dtinh/")) return@classDefForEach
            // Keep untouched classes DEX-backed. Materializing every Gmail class exhausts
            // the Android patching heap; only classes with edits need mutable methods.
            var mutable: MutableClass? = null
            for (method in original.methods) {
                val instructions = method.implementation?.instructions ?: continue
                var edits: MutableList<StringEdit>? = null
                val availability = original.type == "Labwk;" && method.name == "b" &&
                    method.parameterTypes.map { it.toString() } == listOf("Landroid/content/Context;", "I") && method.returnType == "I"
                var existingHook = false
                instructions.forEachIndexed { index, ins ->
                    val reference = (ins as? ReferenceInstruction)?.reference
                    if (availability && reference is MethodReference && reference.definingClass == EXT) existingHook = true
                    val value = (reference as? StringReference)?.string ?: return@forEachIndexed
                    if (value in accountTypes) accounts++
                    if (original.type == "Laboa;" && value == "com.google.android.gms.auth.GetToken") authClass++
                    if (value in microgFcmActions) fcm++
                    var replacement = routes[value]
                    // c() is the Binder descriptor; d() is the service action. Both
                    // contain the same literal: redirect only d(), preserve c()/proxy.
                    if (original.type == "Lacjh;" && method.name == "d" && method.parameterTypes.isEmpty() &&
                        method.returnType == "Ljava/lang/String;" && value in setOf(FEEDBACK, "app.revanced.android.gms.feedback.internal.IFeedbackService")) {
                        replacement = "app.revanced.android.gms.feedback.internal.IFeedbackService"
                        feedbackActions++
                    }
                    if (original.type == "Lacct;" && value in setOf("com.google.android.gms.chimera", "app.revanced.android.gms.chimera")) chimeraAuthorities++
                    if (original.type in cacheTypes && value in cacheNames) {
                        stores++; replacement = value.replace("com.google.android.gms.appid", STORE)
                    }
                    if (replacement != null && replacement != value) {
                        require(ins.opcode in setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO))
                        val changes = edits ?: ArrayList<StringEdit>().also { edits = it }
                        changes.add(StringEdit(index, (ins as OneRegisterInstruction).registerA, replacement))
                        if (value in originalFcmActions) fcm++
                    }
                }
                if (availability) checks++
                // D() classifies account types with hashCode followed by equals.
                // Changing only the equals literal makes microG take the fallback branch.
                val classifier = original.type == "Lawrl;" && method.name == "D" &&
                    method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") && method.returnType == "I"
                if (edits == null && (!availability || existingHook) && !classifier) continue
                val targetClass = mutable ?: mutableClassDefBy(original).also { mutable = it }
                val target = targetClass.methods.single {
                    it.name == method.name && it.returnType == method.returnType && it.parameterTypes == method.parameterTypes
                }
                if (classifier) {
                    val hashInstructions = target.implementation!!.instructions.withIndex().filter {
                        val ins = it.value
                        ins.opcode == Opcode.CONST && (ins as? NarrowLiteralInstruction)?.narrowLiteral in
                            setOf("com.google".hashCode(), "app.revanced".hashCode())
                    }
                    require(hashInstructions.size == 1) { "Gmail account classifier hash layout changed" }
                    val hash = hashInstructions.single()
                    target.replaceInstruction(hash.index, BuilderInstruction31i(Opcode.CONST,
                        (hash.value as OneRegisterInstruction).registerA, "app.revanced".hashCode()))
                    accountClassifiers++
                }
                // Replace in place before inserting the hook, so original indexes stay valid.
                edits?.forEach { edit ->
                    target.replaceInstruction(edit.index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO,
                        edit.register, ImmutableStringReference(edit.value)))
                }
                if (availability && !existingHook) target.addInstructions(0,
                    "invoke-static/range {p0 .. p0}, $EXT->availability(Landroid/content/Context;)I\nmove-result v0\nreturn v0")
            }
        }
        require(accountClassifiers == 1) { "Gmail account classifier is missing" }
        require(feedbackActions == 1 && chimeraAuthorities == 1) { "Gmail service discovery layout changed: feedback=$feedbackActions chimera=$chimeraAuthorities" }
        require(accounts == 237 && stores == 6 && authClass == 1 && checks == 1 && fcm > 0) { "Unexpected Gmail layout: account literals=$accounts cache literals=$stores GetToken class=$authClass checks=$checks FCM routes=$fcm. No APK should be installed." }
    }
}
