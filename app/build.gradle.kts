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

fun configuredValue(name: String): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orNull
        ?: localProperties.getProperty(name).orEmpty()

val appMetricaKey = configuredValue("APPMETRICA_API_KEY")
val squareAdId = configuredValue("YANDEX_SQUARE_AD_ID")
val interstitialAdId = configuredValue("YANDEX_INTERSTITIAL_AD_ID")
val yandexClientId = configuredValue("YANDEX_CLIENT_ID")
val vkAppId = configuredValue("VK_APP_ID")
val authExchangeUrl = configuredValue("AUTH_EXCHANGE_URL")

android {
    namespace = "com.glazev.breathingtrainer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glazev.breathingtrainer"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaKey\"")
        buildConfigField("String", "YANDEX_SQUARE_AD_ID", "\"$squareAdId\"")
        buildConfigField("String", "YANDEX_INTERSTITIAL_AD_ID", "\"$interstitialAdId\"")
        buildConfigField("String", "AUTH_EXCHANGE_URL", "\"$authExchangeUrl\"")

        manifestPlaceholders["YANDEX_CLIENT_ID"] = yandexClientId.ifEmpty { "placeholder" }
        manifestPlaceholders["VKIDClientID"] = vkAppId.ifEmpty { "0" }
        // The SDK requires a non-empty legacy placeholder even when the OAuth 2.1
        // authorization-code + PKCE flow performs token exchange on our backend.
        manifestPlaceholders["VKIDClientSecret"] = "pkce-public-client"
        manifestPlaceholders["VKIDRedirectHost"] = "vk.com"
        manifestPlaceholders["VKIDRedirectScheme"] = "vk" + (vkAppId.ifEmpty { "0" })
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

val validateReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Fails release builds when production service configuration is missing or malformed."
    inputs.properties(
        mapOf(
            "APPMETRICA_API_KEY" to appMetricaKey,
            "YANDEX_SQUARE_AD_ID" to squareAdId,
            "YANDEX_INTERSTITIAL_AD_ID" to interstitialAdId,
            "YANDEX_CLIENT_ID" to yandexClientId,
            "VK_APP_ID" to vkAppId,
            "AUTH_EXCHANGE_URL" to authExchangeUrl
        )
    )
    doLast {
        val configuration = inputs.properties.mapValues { (_, value) -> value.toString() }
        val missing = configuration.filterValues { it.isBlank() }.keys
        check(missing.isEmpty()) {
            "Missing release configuration: ${missing.joinToString()}. " +
                "Set Gradle properties, environment variables, or local.properties."
        }
        check(configuration.getValue("VK_APP_ID").toLongOrNull()?.let { it > 0 } == true) {
            "VK_APP_ID must be a positive numeric application ID."
        }
        check(configuration.getValue("AUTH_EXCHANGE_URL").startsWith("https://")) {
            "AUTH_EXCHANGE_URL must use HTTPS."
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseConfiguration)
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
    implementation(libs.androidx.media3.common)
    
    // Реклама и Авторизация Яндекса
    implementation(libs.yandex.mobileads)
    implementation(libs.yandex.authsdk)
    implementation(libs.appmetrica.sdk)
    
    // VK ID SDK
    implementation(libs.vk.id)
    
    // RuStore Pay SDK
    implementation(libs.rustore.pay)

    // Firebase & Google Credential Manager
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
