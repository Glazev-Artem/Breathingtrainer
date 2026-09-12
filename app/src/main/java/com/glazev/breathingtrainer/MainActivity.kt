package com.glazev.breathingtrainer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.glazev.breathingtrainer.ui.BreathingViewModel
import com.glazev.breathingtrainer.ui.NavGraph
import com.glazev.breathingtrainer.ui.theme.BreathingTrainerTheme
import com.yandex.mobile.ads.common.MobileAds

class MainActivity : ComponentActivity() {

    private val viewModel: BreathingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) {}

        enableEdgeToEdge()
        setContent {
            BreathingTrainerTheme {
                NavGraph(viewModel = viewModel)
            }
        }
        
        // Обработка интента при холодном старте
        intent?.let { viewModel.handleDeeplink(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Обработка возврата из платежной системы (deeplink)
        viewModel.handleDeeplink(intent)
    }
}
