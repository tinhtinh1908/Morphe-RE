// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.patches.gmail

internal const val VERSION = "2026.09.21.992487546.Release"
internal const val MICROG = "app.revanced.android.gms"
internal const val SIGNER = "38918a453d07199354f8b19af05ec6562ced5788"
internal const val STORE = "dtinh.microg.gmail.fcm.v1"
internal const val EXT = "Lvn/dtinh/gmail/GmailBackendV7;"
private val names = listOf("android.c2dm.intent.REGISTER", "android.c2dm.intent.UNREGISTER",
    "android.c2dm.intent.REGISTRATION", "android.c2dm.intent.RECEIVE",
    "android.c2dm.permission.SEND", "android.c2dm.permission.RECEIVE", "iid.TOKEN_REQUEST", "android.gcm.intent.SEND")
// Endpoint names from MicroG-RE 7.2.1 manifest. These are service actions /
// provider authorities, not Java class names, OAuth scopes or Binder descriptors.
private val extraEndpoints = listOf(
    "android.gms.phenotype.service.START",
    "android.gms.clearcut.service.START",
    "android.gms.clearcut.bootcount.service.START",
    "android.gms.pseudonymous.service.START",
    "android.mobstore.service.START",
    "android.gms.wearable.BIND",
    "android.gms.people.service.START",
    "android.gms.signin.service.START",
    "android.gms.fonts.service.START",
    "android.gms.chimera",
    "android.gms.fonts",
    "android.gms.phenotype",
    "android.gsf.gservices",
    "android.providers.gsf.permission.READ_GSERVICES",
)
private val endpointRoutes = extraEndpoints.associate { "com.google.$it" to "app.revanced.$it" } + mapOf(
    "content://com.google.android.gsf.gservices" to "content://app.revanced.android.gsf.gservices",
    "content://com.google.android.gsf.gservices/prefix" to "content://app.revanced.android.gsf.gservices/prefix",
    "content://com.google.android.gms.phenotype" to "content://app.revanced.android.gms.phenotype"
)
internal const val FEEDBACK = "com.google.android.gms.feedback.internal.IFeedbackService"
internal val routes = names.associate { "com.google.$it" to "app.revanced.$it" } + mapOf(
    "com.google.android.gms" to MICROG,
    "com.google.android.gms.auth.accounts" to "$MICROG.auth.accounts",
    "com.google.android.gms.auth.api.signin.service.START" to "$MICROG.auth.api.signin.service.START",
    "com.google.android.gms.auth.GOOGLE_SIGN_IN" to "$MICROG.auth.GOOGLE_SIGN_IN",
    "com.google.android.gms.auth.account.data.service.START" to "$MICROG.auth.account.data.service.START",
    "com.google.android.gms.auth.service.START" to "$MICROG.auth.service.START",
    "com.google" to "app.revanced"
) + endpointRoutes
internal val originalFcmActions = names.mapTo(hashSetOf()) { "com.google.$it" }
internal val microgFcmActions = names.mapTo(hashSetOf()) { "app.revanced.$it" }
internal val accountTypes = setOf("com.google", "app.revanced")
internal val cacheNames = setOf("com.google.android.gms.appid", "com.google.android.gms.appid-no-backup", STORE, "$STORE-no-backup")
internal val cacheTypes = setOf("Lbrjc;", "Lcfsd;", "Lcftl;", "Lcftz;")
