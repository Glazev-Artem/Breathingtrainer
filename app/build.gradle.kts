import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.glazev.breathingtrainer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glazev.breathingtrainer"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val appMetricaKey = localProperties.getProperty("APPMETRICA_API_KEY") ?: ""
        val bannerAdId = localProperties.getProperty("YANDEX_BANNER_AD_ID") ?: ""
        val squareAdId = localProperties.getProperty("YANDEX_SQUARE_AD_ID") ?: ""
        val interstitialAdId = localProperties.getProperty("YANDEX_INTERSTITIAL_AD_ID") ?: ""
        val yandexClientId = localProperties.getProperty("YANDEX_CLIENT_ID") ?: ""
        val vkAppId = localProperties.getProperty("VK_APP_ID") ?: ""
        val vkClientSecret = localProperties.getProperty("VK_CLIENT_SECRET") ?: ""

        buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaKey\"")
        buildConfigField("String", "YANDEX_BANNER_AD_ID", "\"$bannerAdId\"")
        buildConfigField("String", "YANDEX_SQUARE_AD_ID", "\"$squareAdId\"")
        buildConfigField("String", "YANDEX_INTERSTITIAL_AD_ID", "\"$interstitialAdId\"")
        buildConfigField("String", "YANDEX_CLIENT_ID", "\"$yandexClientId\"")
        buildConfigField("String", "VK_APP_ID", "\"$vkAppId\"")

        manifestPlaceholders["YANDEX_CLIENT_ID"] = yandexClientId.ifEmpty { "placeholder" }
        manifestPlaceholders["VKIDClientID"] = vkAppId.ifEmpty { "0" }
        manifestPlaceholders["VKIDClientSecret"] = vkClientSecret.ifEmpty { "secret" }
        manifestPlaceholders["VKIDRedirectHost"] = "vk.com"
        manifestPlaceholders["VKIDRedirectScheme"] = "vk" + (vkAppId.ifEmpty { "0" })
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Media (Sound)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    
    // Реклама и Авторизация Яндекса
    implementation(libs.yandex.mobileads)
    implementation(libs.yandex.authsdk)
    implementation(libs.appmetrica.sdk)
    
    // VK ID SDK
    implementation(libs.vk.id)
    
    // RuStore Pay SDK
    implementation(libs.rustore.pay)

    // Firebase & Google Auth
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
