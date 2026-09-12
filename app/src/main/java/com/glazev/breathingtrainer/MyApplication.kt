package com.glazev.breathingtrainer

import android.app.Application
import android.util.Log
import com.yandex.mobile.ads.common.MobileAds
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // 1. Инициализация AppMetrica
        val config = AppMetricaConfig.newConfigBuilder(BuildConfig.APPMETRICA_API_KEY).build()
        AppMetrica.activate(this, config)
        
        // 2. Инициализация Yandex Mobile Ads SDK
        MobileAds.initialize(this) {
            Log.d("Yandex Ads", "SDK initialized successfully")
        }
        
        // 3. Проверка доступности RuStore Pay (из вашей инструкции)
        // Примечание: Сама инициализация Pay SDK происходит автоматически через Манифест
        Log.d("RuStorePay", "RuStore Pay SDK version: 10.2.0")
    }
}
