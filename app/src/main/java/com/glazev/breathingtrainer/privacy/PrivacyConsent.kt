package com.glazev.breathingtrainer.privacy

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit

data class PrivacyConsent(
    val isDecided: Boolean = false,
    val analyticsEnabled: Boolean = false,
    val personalizedAdsEnabled: Boolean = false
)

object PrivacyConsentStore {
    private const val PREFERENCES = "privacy_preferences"
    private const val KEY_DECIDED = "consent_decided_v1"
    private const val KEY_ANALYTICS = "analytics_enabled"
    private const val KEY_PERSONALIZED_ADS = "personalized_ads_enabled"

    fun load(context: Context): PrivacyConsent {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return PrivacyConsent(
            isDecided = preferences.getBoolean(KEY_DECIDED, false),
            analyticsEnabled = preferences.getBoolean(KEY_ANALYTICS, false),
            personalizedAdsEnabled = preferences.getBoolean(KEY_PERSONALIZED_ADS, false)
        )
    }

    @SuppressLint("ApplySharedPref")
    fun save(context: Context, consent: PrivacyConsent) {
        // This must be durable before optional SDK initialization reads the choice.
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit(commit = true) {
            putBoolean(KEY_ANALYTICS, consent.analyticsEnabled)
            putBoolean(KEY_PERSONALIZED_ADS, consent.personalizedAdsEnabled)
            putBoolean(KEY_DECIDED, true)
        }
    }
}
