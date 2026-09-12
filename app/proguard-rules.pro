# RuStore Billing SDK
-keep class ru.rustore.sdk.billingclient.** { *; }
-keep class ru.rustore.sdk.core.** { *; }
-dontwarn ru.rustore.sdk.**

# Kotlin Serialization (используется в RuStore SDK)
-keep class kotlinx.serialization.json.** { *; }
-keepattributes *Annotation*, EnclosingMethod, Signature, InnerClasses

# AppMetrica & Yandex Ads (на всякий случай, если будут проблемы в релизе)
-keep class com.yandex.mobile.ads.** { *; }
-keep class io.appmetrica.analytics.** { *; }
-dontwarn com.yandex.mobile.ads.**
-dontwarn io.appmetrica.analytics.**

# Firebase
-keepattributes SourceFile,LineNumberTable
-keep public class * extends com.google.firebase.messaging.FirebaseMessagingService
