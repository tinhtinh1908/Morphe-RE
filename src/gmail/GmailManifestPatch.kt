// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.gmail

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.security.MessageDigest

internal val gmailManifest = resourcePatch {
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
                if (e.tagName in setOf("action", "uses-permission", "permission", "package") &&
                    !(e.tagName == "uses-permission" && e.getAttribute("android:name") == "com.google.android.providers.gsf.permission.READ_GSERVICES")) {
                    routes[e.getAttribute("android:name")]?.let { e.setAttribute("android:name", it) }
                }
                routes[e.getAttribute("android:permission")]?.let { e.setAttribute("android:permission", it) }
            }
            // The upstream GServicesProvider checks the original requested permission
            // even when its authority is relocated. Keep both permissions.
            for (readGservices in listOf("com.google.android.providers.gsf.permission.READ_GSERVICES",
                "app.revanced.android.providers.gsf.permission.READ_GSERVICES")) {
                val permissions = (0 until all.length).map { all.item(it) as Element }.filter {
                    it.tagName == "uses-permission" && it.getAttribute("android:name") == readGservices
                }
                if (permissions.isEmpty()) root.appendChild(doc.createElement("uses-permission").also {
                    it.setAttribute("android:name", readGservices)
                })
                permissions.drop(1).forEach { it.parentNode.removeChild(it) }
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
