package com.glazev.breathingtrainer

import android.app.Application
import android.util.Log
import com.vk.id.VKID
import com.yandex.mobile.ads.common.MobileAds
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import com.glazev.breathingtrainer.privacy.PrivacyConsent
import java.util.concurrent.atomic.AtomicBoolean

/** Loads analytics, advertising and sign-in SDK classes only after the first UI frame. */
object UiSdkInitializer {
    private val authenticationStarted = AtomicBoolean(false)
    private val adsStarted = AtomicBoolean(false)
    private val analyticsStarted = AtomicBoolean(false)

    fun initializeAuthentication(application: Application) {
        if (!authenticationStarted.compareAndSet(false, true)) return
        runCatching { VKID.init(application) }
            .onFailure { Log.e("VKID", "VK ID init error", it) }
    }

    fun applyPrivacyConsent(application: Application, consent: PrivacyConsent) {
        check(consent.isDecided) { "Privacy choice must be saved before SDK initialization" }

        MobileAds.setLocationConsent(false)
        MobileAds.setUserConsent(consent.personalizedAdsEnabled)
        MobileAds.setAppAdAnalyticsReporting(consent.analyticsEnabled)
        if (adsStarted.compareAndSet(false, true)) {
            MobileAds.initialize(application) {
                Log.d("Yandex Ads", "SDK initialized successfully")
            }
        }

        if (consent.analyticsEnabled && BuildConfig.APPMETRICA_API_KEY.isNotBlank()) {
            AppMetrica.setLocationTracking(false)
            AppMetrica.setAdvIdentifiersTracking(consent.personalizedAdsEnabled)
            if (analyticsStarted.compareAndSet(false, true)) {
                runCatching {
                    val config = AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY).build()
                    AppMetrica.activate(application, config)
                }.onFailure { Log.e("AppMetrica", "SDK init error", it) }
            }
            AppMetrica.setDataSendingEnabled(true)
        } else if (analyticsStarted.get()) {
            runCatching {
                AppMetrica.setDataSendingEnabled(false)
            }.onFailure { Log.e("AppMetrica", "SDK init error", it) }
        }
    }
}
