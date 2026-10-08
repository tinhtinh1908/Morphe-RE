// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.gmail

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
import java.security.MessageDigest

private const val VERSION = "2026.09.21.992487546.Release"
private const val MICROG = "app.revanced.android.gms"
private const val SIGNER = "38918a453d07199354f8b19af05ec6562ced5788"
private const val STORE = "dtinh.microg.gmail.fcm.v1"
private const val EXT = "Lvn/dtinh/gmail/GmailBackendV7;"
private val names = listOf("android.c2dm.intent.REGISTER", "android.c2dm.intent.UNREGISTER",
    "android.c2dm.intent.REGISTRATION", "android.c2dm.intent.RECEIVE",
    "android.c2dm.permission.SEND", "android.c2dm.permission.RECEIVE", "iid.TOKEN_REQUEST", "android.gcm.intent.SEND")
private val routes = names.associate { "com.google.$it" to "app.revanced.$it" } + mapOf(
    "com.google.android.gms" to MICROG,
    "com.google.android.gms.auth.accounts" to "$MICROG.auth.accounts",
    "com.google.android.gms.auth.api.signin.service.START" to "$MICROG.auth.api.signin.service.START",
    "com.google.android.gms.auth.GOOGLE_SIGN_IN" to "$MICROG.auth.GOOGLE_SIGN_IN",
    "com.google.android.gms.auth.account.data.service.START" to "$MICROG.auth.account.data.service.START",
    "com.google.android.gms.auth.service.START" to "$MICROG.auth.service.START",
    "com.google" to "app.revanced"
)
private val cacheTypes = setOf("Lbrjc;", "Lcfsd;", "Lcftl;", "Lcftz;")
private val gmailManifest = resourcePatch {
    execute {
        require(packageMetadata.packageName == "com.google.android.gm" && packageMetadata.versionName == VERSION) { "Gmail version not supported by this experimental patch." }
        document("AndroidManifest.xml").use { doc ->
            val root = doc.documentElement
            require(root.getAttribute("android:requiredSplitTypes").isEmpty()) { "Gmail needs its ABI/density splits. Merge the complete APKS before patching; base.apk alone is unsupported." }
            val app = doc.getElementsByTagName("application").item(0) as Element
            val meta = app.getElementsByTagName("meta-data")
            val values = (0 until meta.length).associate { val e = meta.item(it) as Element; e.getAttribute("android:name") to e.getAttribute("android:value") }
            if (values["app.revanced.MICROG_PACKAGE_NAME"] == MICROG) {
                require(values["$MICROG.SPOOFED_PACKAGE_NAME"] == "com.google.android.gm" && values["$MICROG.SPOOFED_PACKAGE_SIGNATURE"] == SIGNER) { "Original Gmail identity metadata is invalid." }
            } else {
                val certs = packageMetadata.signingCertificates.values.flatten().map { MessageDigest.getInstance("SHA-1").digest(it.encoded).joinToString("") { b -> "%02x".format(b.toInt() and 255) } }.distinct()
                require(certs.isEmpty() || certs == listOf(SIGNER)) { "Unexpected signed Gmail input; use the original split set or its unsigned merged APK." }
            }
            val all = doc.getElementsByTagName("*")
            for (i in 0 until all.length) {
                val e = all.item(i) as Element
                if (e.tagName in setOf("action", "uses-permission", "permission", "package")) routes[e.getAttribute("android:name")]?.let { e.setAttribute("android:name", it) }
                routes[e.getAttribute("android:permission")]?.let { e.setAttribute("android:permission", it) }
            }
            val receiver = (0 until all.length).map { all.item(it) as Element }.single { it.tagName == "receiver" && it.getAttribute("android:name") == "com.google.firebase.iid.FirebaseInstanceIdReceiver" }
            require(receiver.getAttribute("android:permission") == "app.revanced.android.c2dm.permission.SEND")
            receiver.setAttribute("android:enabled", "true")
            val queries = (doc.getElementsByTagName("queries").item(0) as? Element) ?: doc.createElement("queries").also { root.appendChild(it) }
            if ((0 until queries.childNodes.length).none { (queries.childNodes.item(it) as? Element)?.getAttribute("android:name") == MICROG }) queries.appendChild(doc.createElement("package").also { it.setAttribute("android:name", MICROG) })
            for ((key, value) in mapOf("app.revanced.MICROG_PACKAGE_NAME" to MICROG, "$MICROG.SPOOFED_PACKAGE_NAME" to "com.google.android.gm", "$MICROG.SPOOFED_PACKAGE_SIGNATURE" to SIGNER)) {
                val node = (0 until meta.length).map { meta.item(it) as Element }.firstOrNull { it.getAttribute("android:name") == key } ?: doc.createElement("meta-data").also { app.appendChild(it) }
                node.setAttribute("android:name", key); node.removeAttribute("android:resource"); node.setAttribute("android:value", value)
            }
        }
        var sync = 0
        get("res/xml").walkTopDown().filter { it.isFile && it.extension == "xml" }.forEach { file ->
            document("res/xml/${file.name}").use { doc ->
                val e = doc.documentElement
                if (e.tagName == "sync-adapter" && e.getAttribute("android:accountType") in setOf("com.google", "app.revanced")) {
                    e.setAttribute("android:accountType", "app.revanced"); sync++
                }
            }
        }
        require(sync >= 1) { "Gmail mail sync adapter is missing." }
    }
}

val gmailMicroGSupport = bytecodePatch(
    name = "Gmail microG support (experimental)",
    description = "Gmail $VERSION: personal accounts, OAuth, mail sync and FCM/Chime routing through MicroG-RE 7.2.1. Keep the original app package. Device behavior is unverified.",
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
        var accounts = 0; var stores = 0; var authClass = 0; var checks = 0; var fcm = 0
        classDefForEach { original ->
            if (original.type.startsWith("Lvn/dtinh/")) return@classDefForEach
            val mutable = mutableClassDefBy(original)
            for (method in mutable.methods.toList()) {
                val instructions = method.implementation?.instructions?.toList() ?: continue
                instructions.forEachIndexed { index, ins ->
                    val value = ((ins as? ReferenceInstruction)?.reference as? StringReference)?.string ?: return@forEachIndexed
                    if (value in setOf("com.google", "app.revanced")) accounts++
                    if (original.type == "Laboa;" && value == "com.google.android.gms.auth.GetToken") authClass++
                    if (value in names.map { "app.revanced.$it" }) fcm++
                    var replacement = routes[value]
                    if (original.type in cacheTypes && value in setOf("com.google.android.gms.appid", "com.google.android.gms.appid-no-backup", STORE, "$STORE-no-backup")) {
                        stores++; replacement = value.replace("com.google.android.gms.appid", STORE)
                    }
                    if (replacement != null && replacement != value) {
                        require(ins.opcode in setOf(Opcode.CONST_STRING, Opcode.CONST_STRING_JUMBO))
                        method.replaceInstruction(index, BuilderInstruction31c(Opcode.CONST_STRING_JUMBO, (ins as OneRegisterInstruction).registerA, ImmutableStringReference(replacement)))
                        if (value in names.map { "com.google.$it" }) fcm++
                    }
                }
                if (original.type == "Labwk;" && method.name == "b" && method.parameterTypes.map { it.toString() } == listOf("Landroid/content/Context;", "I") && method.returnType == "I") {
                    val existing = instructions.any { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.definingClass == EXT }
                    if (!existing) method.addInstructions(0, "invoke-static/range {p0 .. p0}, $EXT->availability(Landroid/content/Context;)I\nmove-result v0\nreturn v0")
                    checks++
                }
            }
        }
        require(accounts == 237 && stores == 6 && authClass == 1 && checks == 1 && fcm > 0) { "Unexpected Gmail layout: account literals=$accounts cache literals=$stores GetToken class=$authClass checks=$checks FCM routes=$fcm. No APK should be installed." }
    }
}
