package com.glazev.breathingtrainer.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.glazev.breathingtrainer.AdConfig
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.size(80.dp), // Фиксированный квадратный размер
        factory = { context ->
            BannerAdView(context).apply {
                setAdUnitId(AdConfig.SQUARE_AD_UNIT_ID)
                setAdSize(BannerAdSize.fixedSize(context, 80, 80))
                val adRequest = AdRequest.Builder().build()
                loadAd(adRequest)
            }
        }
    )
}
