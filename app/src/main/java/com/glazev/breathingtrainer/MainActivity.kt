package com.glazev.breathingtrainer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.glazev.breathingtrainer.privacy.PrivacyConsent
import com.glazev.breathingtrainer.privacy.PrivacyConsentStore
import com.glazev.breathingtrainer.billing.RuStoreDeepLinkPolicy
import com.glazev.breathingtrainer.ui.BreathingViewModel
import com.glazev.breathingtrainer.ui.NavGraph
import com.glazev.breathingtrainer.ui.components.PrivacyConsentDialog
import com.glazev.breathingtrainer.ui.theme.BreathingTrainerTheme
import com.glazev.breathingtrainer.widget.BreathingAppWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: BreathingViewModel by viewModels()
    private var privacyConsent by mutableStateOf(PrivacyConsent())
    private var pendingIntentUntilConsent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privacyConsent = PrivacyConsentStore.load(this)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        var startDestination = "settings"
        val action = intent?.action
        if (action == BreathingAppWidgetProvider.ACTION_START_WIDGET_TECHNIQUE ||
            action == BreathingAppWidgetProvider.ACTION_START_SOS) {
            startDestination = "training"
        }

        setContent {
            BreathingTrainerTheme {
                if (privacyConsent.isDecided) {
                    NavGraph(
                        viewModel = viewModel,
                        startDestination = startDestination,
                        privacyConsent = privacyConsent,
                        onPrivacyConsentChange = ::savePrivacyConsent
                    )
                } else {
                    PrivacyConsentDialog(
                        initialConsent = privacyConsent,
                        onSave = ::savePrivacyConsent
                    )
                }
            }
        }

        // Draw the first frame before starting optional analytics/auth/ad SDKs.
        window.decorView.postDelayed({
            UiSdkInitializer.initializeAuthentication(application)
            if (privacyConsent.isDecided) {
                UiSdkInitializer.applyPrivacyConsent(application, privacyConsent)
                viewModel.preloadInterstitialAd()
            }
        }, OPTIONAL_SDK_INIT_DELAY_MS)
        
        intent?.let(::handleIntentAfterConsent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentAfterConsent(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            viewModel.onHostStopped()
        }
    }

    private fun handleAllIntents(intent: Intent) {
        val action = intent.action ?: return
        if (action == BreathingAppWidgetProvider.ACTION_START_WIDGET_TECHNIQUE ||
            action == BreathingAppWidgetProvider.ACTION_START_SOS) {
            val techniqueId = intent.getStringExtra(BreathingAppWidgetProvider.EXTRA_TECHNIQUE_ID) ?: "square"
            val isSos = intent.getBooleanExtra(BreathingAppWidgetProvider.EXTRA_IS_SOS, false)
            
            viewModel.handleWidgetStart(
                techniqueId = techniqueId,
                isSos = isSos,
                activity = this
            )
            intent.action = null
        } else if (RuStoreDeepLinkPolicy.accepts(intent)) {
            viewModel.handleDeeplink(intent)
        }
    }

    private fun handleIntentAfterConsent(intent: Intent) {
        if (privacyConsent.isDecided) {
            handleAllIntents(intent)
        } else if (intent.action != Intent.ACTION_MAIN) {
            pendingIntentUntilConsent = Intent(intent)
        }
    }

    private fun savePrivacyConsent(consent: PrivacyConsent) {
        val decidedConsent = consent.copy(isDecided = true)
        PrivacyConsentStore.save(this, decidedConsent)
        privacyConsent = decidedConsent
        UiSdkInitializer.applyPrivacyConsent(application, decidedConsent)
        viewModel.preloadInterstitialAd()
        pendingIntentUntilConsent?.let { pending ->
            pendingIntentUntilConsent = null
            handleAllIntents(pending)
        }
    }

    companion object {
        private const val OPTIONAL_SDK_INIT_DELAY_MS = 500L
    }
}
