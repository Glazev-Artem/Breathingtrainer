package com.glazev.breathingtrainer.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.glazev.breathingtrainer.AdConfig
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.model.BreathingPhase
import com.glazev.breathingtrainer.model.BreathingTechnique
import com.glazev.breathingtrainer.model.DefaultTechniques
import com.glazev.breathingtrainer.model.PhaseType
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.Product
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchase
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductPurchaseResult
import ru.rustore.sdk.pay.model.ProductPurchaseStatus
import ru.rustore.sdk.pay.model.SubscriptionPurchase
import ru.rustore.sdk.pay.model.SubscriptionPurchaseStatus
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

data class BackgroundMusic(
    val id: String,
    val name: String,
    val resId: Int? = null,
    val uri: Uri? = null
)

data class TrainingRecord(
    val date: Long,
    val techniqueName: String,
    val circles: Int,
    val durationTotalSeconds: Int,
    val retentions: List<Int> = emptyList()
)

data class BreathingUiState(
    val inhaleTime: Float = 4f,
    val inhaleHoldTime: Float = 0f,
    val exhaleTime: Float = 4f,
    val exhaleHoldTime: Float = 0f,
    val repetitions: Int = 10,
    val cycles: Int = 1,
    val isWimHofMode: Boolean = false,
    val wimHofRetentionTime: Float = 60f,
    val wimHofRecoveryTime: Float = 15f,
    val selectedVoice: String = "Male",
    val selectedMusic: BackgroundMusic? = null,
    val currentPhaseIndex: Int = 0,
    val currentPhaseType: PhaseType? = null,
    val remainingSeconds: Float = 0f,
    val isRunning: Boolean = false,
    val isCountingDown: Boolean = false,
    val countdownSeconds: Int = 5,
    val currentCycle: Int = 1,
    val currentRepetition: Int = 1,
    val activeTechnique: BreathingTechnique? = null,
    val isWimHofRetentionPhase: Boolean = false,
    val isWimHofRecoveryPhase: Boolean = false,
    val isPremium: Boolean = false,
    val monthlyPrice: String = "",
    val lifetimePrice: String = "",
    val musicVolume: Float = 0.75f,
    val breathVolume: Float = 0.3f,
    val customMusicList: List<BackgroundMusic> = emptyList(),
    val userPresets: List<BreathingTechnique> = emptyList(),
    val isSyncing: Boolean = false,
    val userEmail: String? = null,
    val trainingHistory: List<TrainingRecord> = emptyList(),
    val currentSessionRetentions: List<Int> = emptyList(),
    val reminderTime: String? = null,
    val dayReminders: Map<String, String> = emptyMap(),
    val streakCount: Int = 0,
    val vibrationEnabled: Boolean = true,
    val finalSound: BackgroundMusic? = null,
    val finalSoundVolume: Float = 0.3f
)

class BreathingViewModel(application: Application) : AndroidViewModel(application) {

    val defaultMusic = listOf(
        BackgroundMusic("none", "Без музыки"),
        BackgroundMusic("main", "Медитация 1", R.raw.meditation_breath_bg_main),
        BackgroundMusic("bg1", "Медитация 2", R.raw.meditation_breath_bg_1),
        BackgroundMusic("bg2", "Медитация 3", R.raw.meditation_breath_bg_2),
        BackgroundMusic("bg3", "Медитация 4", R.raw.meditation_breath_bg_3),
        BackgroundMusic("bg4", "Медитация 5", R.raw.meditation_breath_bg_4),
        BackgroundMusic("bg5", "Медитация 6", R.raw.meditation_breath_bg_5)
    )

    val finalSounds = listOf(
        BackgroundMusic("final1", "Финал 1", R.raw.final_sound1),
        BackgroundMusic("final2", "Финал 2", R.raw.final_sound2),
        BackgroundMusic("final3", "Финал 3", R.raw.final_sound3)
    )

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    
    private val _uiState = MutableStateFlow(BreathingUiState(
        isPremium = prefs.getBoolean("is_premium", false),
        musicVolume = prefs.getFloat("music_volume", 0.75f),
        breathVolume = prefs.getFloat("breath_volume", 0.3f),
        finalSoundVolume = prefs.getFloat("final_sound_volume", 0.3f),
        userEmail = auth.currentUser?.email,
        selectedMusic = defaultMusic[1],
        selectedVoice = prefs.getString("selected_voice", "Male") ?: "Male",
        reminderTime = prefs.getString("reminder_time", null),
        dayReminders = parseDayReminders(prefs.getString("day_reminders", "{}") ?: "{}"),
        streakCount = prefs.getInt("streak_count", 0),
        vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
    ))
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var trainingStartTime: Long = 0
    private val breathPlayer = ExoPlayer.Builder(application).build()
    private val musicPlayer = ExoPlayer.Builder(application).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
    }
    private val finalPlayer = ExoPlayer.Builder(application).build()

    private var interstitialAd: InterstitialAd? = null
    private val interstitialAdLoader = InterstitialAdLoader(application)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        breathPlayer.volume = _uiState.value.breathVolume
        musicPlayer.volume = _uiState.value.musicVolume
        finalPlayer.volume = _uiState.value.finalSoundVolume
        
        setupAdLoader()
        loadInterstitialAd()
        loadCustomMusic()
        loadUserPresets()
        loadHistory()
        checkPurchases()
        loadProductsInfo()
        loadFinalSoundConfig()
        
        if (auth.currentUser != null) {
            syncWithCloud()
        }
        updateStreakOnStart()
    }

    private fun loadFinalSoundConfig() {
        val id = prefs.getString("final_sound_id", "final1")
        val uriStr = prefs.getString("final_sound_uri", null)
        val name = prefs.getString("final_sound_name", "Свой звук") ?: "Свой звук"
        val sound = if (uriStr != null) {
            BackgroundMusic(id ?: "custom_final", name, uri = uriStr.toUri())
        } else {
            finalSounds.find { it.id == id } ?: finalSounds[0]
        }
        _uiState.update { it.copy(finalSound = sound) }
    }

    fun updateFinalSound(sound: BackgroundMusic) {
        _uiState.update { it.copy(finalSound = sound) }
        prefs.edit { 
            putString("final_sound_id", sound.id)
            putString("final_sound_name", sound.name)
            if (sound.uri != null) putString("final_sound_uri", sound.uri.toString())
            else remove("final_sound_uri")
        }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun addCustomFinalSound(uri: Uri) {
        try {
            val context = getApplication<Application>()
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("BreathingViewModel", "Persistable permission failed: ${e.message}")
            }
            
            var name = "Свой звук"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }

            val newSound = BackgroundMusic("custom_final_${System.currentTimeMillis()}", name, uri = uri)
            updateFinalSound(newSound)
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error adding custom final sound: ${e.message}")
        }
    }

    fun updateFinalSoundVolume(volume: Float) {
        _uiState.update { it.copy(finalSoundVolume = volume) }
        finalPlayer.volume = volume
        prefs.edit { putFloat("final_sound_volume", volume) }
    }

    private fun playFinalSound() {
        val sound = _uiState.value.finalSound ?: return
        val mediaItem = when {
            sound.resId != null -> MediaItem.fromUri("android.resource://${getApplication<Application>().packageName}/${sound.resId}".toUri())
            sound.uri != null -> {
                if (isUriAccessible(sound.uri)) {
                    MediaItem.fromUri(sound.uri)
                } else {
                    Log.w("BreathingViewModel", "Final sound URI not accessible, falling back")
                    val fallback = finalSounds[0]
                    updateFinalSound(fallback)
                    MediaItem.fromUri("android.resource://${getApplication<Application>().packageName}/${fallback.resId}".toUri())
                }
            }
            else -> return
        }
        finalPlayer.setMediaItem(mediaItem)
        finalPlayer.prepare()
        finalPlayer.play()
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        prefs.edit { putBoolean("vibration_enabled", enabled) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun vibrate(duration: Long = 100) {
        if (!_uiState.value.vibrationEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }

    private fun vibrateDouble() {
        if (!_uiState.value.vibrationEnabled) return
        viewModelScope.launch {
            vibrate(100)
            delay(200)
            vibrate(100)
        }
    }

    private fun setupAdLoader() {
        interstitialAdLoader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                this@BreathingViewModel.interstitialAd = ad
                Log.d("Ads", "Interstitial ad loaded")
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.e("Ads", "Failed to load interstitial ad: ${error.description}")
            }
        })
    }

    private fun loadInterstitialAd() {
        val adRequestConfiguration = AdRequestConfiguration.Builder(AdConfig.INTERSTITIAL_AD_UNIT_ID).build()
        interstitialAdLoader.loadAd(adRequestConfiguration)
    }

    fun showInterstitialAd(activity: Activity) {
        if (_uiState.value.isPremium) return
        
        interstitialAd?.apply {
            setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdDismissed() {
                    interstitialAd = null
                    loadInterstitialAd()
                }
                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            show(activity)
        } ?: loadInterstitialAd()
    }

    fun startTrainingWithAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (_uiState.value.isPremium) {
            onAdDismissed()
            return
        }
        
        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    Log.d("Ads", "Ad shown, cancelling any pending start to avoid double triggers")
                }
                override fun onAdFailedToShow(error: AdError) {
                    Log.e("Ads", "Ad failed to show: ${error.description}")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdDismissed() {
                    Log.d("Ads", "Ad dismissed, triggering onAdDismissed callback")
                    interstitialAd = null
                    loadInterstitialAd()
                    onAdDismissed()
                }
                override fun onAdClicked() {}
                override fun onAdImpression(impressionData: ImpressionData?) {}
            })
            currentAd.show(activity)
        } else {
            Log.d("Ads", "No ad loaded, starting training immediately")
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    fun selectTechniqueById(id: String) {
        val allTechniques = DefaultTechniques.list + _uiState.value.userPresets
        val found = allTechniques.find { it.id == id } ?: DefaultTechniques.SquareBreathing
        selectPreset(found)
    }

    fun handleWidgetStart(
        techniqueId: String,
        isSos: Boolean,
        activity: Activity,
        onNavigateToTraining: () -> Unit
    ) {
        selectTechniqueById(techniqueId)

        if (isSos) {
            // SOS режим: Запускается моментально БЕЗ РЕКЛАМЫ для всех
            onNavigateToTraining()
            startTraining()
        } else {
            // Обычный запуск: Если нет подписки, показываем рекламу
            startTrainingWithAd(activity) {
                onNavigateToTraining()
                startTraining()
            }
        }
    }

    private fun loadCustomMusic() {
        val musicJson = prefs.getString("custom_music_list", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(musicJson)
            val customList = mutableListOf<BackgroundMusic>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                customList.add(BackgroundMusic(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    uri = obj.getString("uri").toUri()
                ))
            }
            _uiState.update { it.copy(customMusicList = customList) }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading custom music: ${e.message}")
        }
    }

    private fun saveCustomMusic() {
        val jsonArray = JSONArray()
        _uiState.value.customMusicList.forEach { music ->
            val obj = JSONObject()
            obj.put("id", music.id)
            obj.put("name", music.name)
            obj.put("uri", music.uri.toString())
            jsonArray.put(obj)
        }
        prefs.edit { putString("custom_music_list", jsonArray.toString()) }
    }

    fun addCustomMusic(uri: Uri) {
        try {
            val context = getApplication<Application>()
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("BreathingViewModel", "Persistable permission failed: ${e.message}")
            }
            
            var name = "Свой трек"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }

            val newMusic = BackgroundMusic(
                id = UUID.randomUUID().toString(),
                name = name,
                uri = uri
            )
            _uiState.update { it.copy(customMusicList = it.customMusicList + newMusic) }
            saveCustomMusic()
            if (auth.currentUser != null) pushDataToCloud()
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error adding custom music: ${e.message}")
        }
    }

    fun removeCustomMusic(musicId: String) {
        _uiState.update { it.copy(customMusicList = it.customMusicList.filter { it.id != musicId }) }
        if (_uiState.value.selectedMusic?.id == musicId) {
            selectMusic(defaultMusic[1])
        }
        saveCustomMusic()
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun deleteCustomMusic(music: BackgroundMusic) {
        removeCustomMusic(music.id)
    }

    fun selectMusic(music: BackgroundMusic) {
        _uiState.update { it.copy(selectedMusic = music) }
        if (_uiState.value.isRunning) {
            playMusic()
        }
    }

    fun updateVoice(voice: String) {
        _uiState.update { it.copy(selectedVoice = voice) }
        prefs.edit { putString("selected_voice", voice) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun updateMusicVolume(volume: Float) {
        _uiState.update { it.copy(musicVolume = volume) }
        musicPlayer.volume = volume
        prefs.edit { putFloat("music_volume", volume) }
    }

    fun updateBreathVolume(volume: Float) {
        _uiState.update { it.copy(breathVolume = volume) }
        breathPlayer.volume = volume
        prefs.edit { putFloat("breath_volume", volume) }
    }

    fun loadProductsInfo() {
        val monthlyId = getApplication<Application>().getString(R.string.product_id_monthly)
        val lifetimeId = getApplication<Application>().getString(R.string.product_id_lifetime)
        val productIds = listOf(ProductId(monthlyId), ProductId(lifetimeId))
        
        RuStorePayClient.instance.getProductInteractor().getProducts(productIds)
            .addOnSuccessListener { productList ->
                productList.forEach { product ->
                    val priceText = product.amountLabel.value
                    if (product.productId.value == monthlyId) {
                        _uiState.update { it.copy(monthlyPrice = priceText) }
                    } else if (product.productId.value == lifetimeId) {
                        _uiState.update { it.copy(lifetimePrice = priceText) }
                    }
                }
            }
            .addOnFailureListener { e -> Log.e("RuStorePay", "Failed to load products: ${e.message}") }
    }

    fun checkPurchases() {
        val monthlyId = getApplication<Application>().getString(R.string.product_id_monthly)
        val lifetimeId = getApplication<Application>().getString(R.string.product_id_lifetime)
        
        RuStorePayClient.instance.getPurchaseInteractor().getPurchases()
            .addOnSuccessListener { purchases ->
                val hasPremium = purchases.any { purchase ->
                    when (purchase) {
                        is ProductPurchase -> {
                            val pid = purchase.productId.value
                            (pid == monthlyId || pid == lifetimeId) &&
                            (purchase.status == ProductPurchaseStatus.CONFIRMED || purchase.status == ProductPurchaseStatus.PAID)
                        }
                        is SubscriptionPurchase -> {
                            val pid = purchase.productId.value
                            (pid == monthlyId || pid == lifetimeId) &&
                            purchase.status == SubscriptionPurchaseStatus.ACTIVE
                        }
                        else -> false
                    }
                }
                setPremiumStatus(hasPremium)
            }
            .addOnFailureListener { e -> Log.e("RuStorePay", "Failed to check purchases: ${e.message}") }
    }

    private fun setPremiumStatus(enabled: Boolean) {
        _uiState.update { it.copy(isPremium = enabled) }
        prefs.edit { putBoolean("is_premium", enabled) }
    }

    fun purchaseMonthly() {
        val productId = getApplication<Application>().getString(R.string.product_id_monthly)
        purchaseProduct(productId)
    }

    fun purchaseLifetime() {
        val productId = getApplication<Application>().getString(R.string.product_id_lifetime)
        purchaseProduct(productId)
    }

    fun purchaseProduct(productId: String) {
        val params = ProductPurchaseParams(productId = ProductId(productId))
        RuStorePayClient.instance.getPurchaseInteractor().purchase(params)
            .addOnSuccessListener { result ->
                Log.d("RuStorePay", "Purchase success: ${result.orderId?.value ?: result.purchaseId.value}")
                checkPurchases()
            }
            .addOnFailureListener { error ->
                Log.e("RuStorePay", "Purchase task failed: ${error.message}")
                val tip = if (error.message?.contains("RU_STORE_NOT_INSTALLED") == true) "Пожалуйста, установите RuStore." else ""
                Toast.makeText(getApplication(), "Ошибка RuStore: ${error.message}. $tip", Toast.LENGTH_LONG).show()
            }
    }

    fun openRuStoreSubscriptions() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://apps.rustore.ru/subscriber/subscriptions"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("RuStore", "Failed to open subscriptions", e)
        }
    }

    fun handleDeeplink(intent: Intent) {
        try {
            RuStorePayClient.instance.getIntentInteractor().proceedIntent(intent)
        } catch (e: Exception) {
            Log.e("RuStorePay", "Error handling deeplink: ${e.message}")
        }
    }

    fun setWimHofMode(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                isWimHofMode = enabled,
                repetitions = if (enabled) 30 else it.repetitions,
                cycles = if (enabled) 3 else it.cycles
            ) 
        }
    }

    fun updateInhale(time: Float) {
        _uiState.update { it.copy(inhaleTime = time, activeTechnique = null) }
    }

    fun updateInhaleHold(time: Float) {
        _uiState.update { it.copy(inhaleHoldTime = time, activeTechnique = null) }
    }

    fun updateExhale(time: Float) {
        _uiState.update { it.copy(exhaleTime = time, activeTechnique = null) }
    }

    fun updateExhaleHold(time: Float) {
        _uiState.update { it.copy(exhaleHoldTime = time, activeTechnique = null) }
    }

    fun updateCycles(cycles: Int) {
        _uiState.update { it.copy(cycles = cycles) }
    }

    fun updateRepetitions(reps: Int) {
        _uiState.update { it.copy(repetitions = reps) }
    }

    fun updateWimHofRetention(time: Float) {
        _uiState.update { it.copy(wimHofRetentionTime = time) }
    }

    fun selectPreset(technique: BreathingTechnique) {
        _uiState.update {
            it.copy(
                activeTechnique = technique,
                isWimHofMode = false,
                inhaleTime = technique.phases.find { p -> p.type == PhaseType.INHALE }?.durationSeconds ?: 0f,
                inhaleHoldTime = technique.phases.find { p -> p.type == PhaseType.HOLD_IN }?.durationSeconds ?: 0f,
                exhaleTime = technique.phases.find { p -> p.type == PhaseType.EXHALE }?.durationSeconds ?: 0f,
                exhaleHoldTime = technique.phases.find { p -> p.type == PhaseType.HOLD_OUT }?.durationSeconds ?: 0f
            )
        }
    }

    fun saveUserPreset(name: String) {
        val technique = BreathingTechnique(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "${_uiState.value.inhaleTime.toInt()}-${_uiState.value.inhaleHoldTime.toInt()}-${_uiState.value.exhaleTime.toInt()}-${_uiState.value.exhaleHoldTime.toInt()}",
            phases = listOfNotNull(
                if (_uiState.value.inhaleTime > 0) BreathingPhase(PhaseType.INHALE, _uiState.value.inhaleTime) else null,
                if (_uiState.value.inhaleHoldTime > 0) BreathingPhase(PhaseType.HOLD_IN, _uiState.value.inhaleHoldTime) else null,
                if (_uiState.value.exhaleTime > 0) BreathingPhase(PhaseType.EXHALE, _uiState.value.exhaleTime) else null,
                if (_uiState.value.exhaleHoldTime > 0) BreathingPhase(PhaseType.HOLD_OUT, _uiState.value.exhaleHoldTime) else null
            ),
            isCustom = true
        )
        savePreset(technique)
        _uiState.update { it.copy(activeTechnique = technique) }
    }

    fun deleteUserPreset(technique: BreathingTechnique) {
        deletePreset(technique.id)
    }

    fun startTraining() {
        val state = _uiState.value
        val technique = if (state.isWimHofMode) {
            BreathingTechnique(
                id = "wim_hof",
                name = "Вим Хоф",
                description = "Метод Вима Хофа",
                phases = listOf(
                    BreathingPhase(PhaseType.INHALE, 1.5f),
                    BreathingPhase(PhaseType.EXHALE, 1.5f)
                )
            )
        } else {
            state.activeTechnique ?: BreathingTechnique(
                id = "custom_temp",
                name = "Пользовательский",
                description = "${state.inhaleTime.toInt()}-${state.inhaleHoldTime.toInt()}-${state.exhaleTime.toInt()}-${state.exhaleHoldTime.toInt()}",
                phases = listOfNotNull(
                    if (state.inhaleTime > 0) BreathingPhase(PhaseType.INHALE, state.inhaleTime) else null,
                    if (state.inhaleHoldTime > 0) BreathingPhase(PhaseType.HOLD_IN, state.inhaleHoldTime) else null,
                    if (state.exhaleTime > 0) BreathingPhase(PhaseType.EXHALE, state.exhaleTime) else null,
                    if (state.exhaleHoldTime > 0) BreathingPhase(PhaseType.HOLD_OUT, state.exhaleHoldTime) else null
                ),
                isCustom = true
            )
        }
        startBreathing(technique)
    }

    fun stopTraining() = stopBreathing()

    fun startBreathing(technique: BreathingTechnique) {
        _uiState.update {
            it.copy(
                activeTechnique = technique,
                currentCycle = 1,
                currentRepetition = 1,
                currentPhaseIndex = 0,
                currentPhaseType = technique.phases.getOrNull(0)?.type,
                isCountingDown = true,
                countdownSeconds = 5,
                isRunning = true,
                currentSessionRetentions = emptyList(),
                inhaleTime = technique.phases.find { p -> p.type == PhaseType.INHALE }?.durationSeconds ?: (if (technique.id == "wim_hof") 1.5f else 0f),
                inhaleHoldTime = technique.phases.find { p -> p.type == PhaseType.HOLD_IN }?.durationSeconds ?: 0f,
                exhaleTime = technique.phases.find { p -> p.type == PhaseType.EXHALE }?.durationSeconds ?: (if (technique.id == "wim_hof") 1.5f else 0f),
                exhaleHoldTime = technique.phases.find { p -> p.type == PhaseType.HOLD_OUT }?.durationSeconds ?: 0f
            )
        }
        trainingStartTime = System.currentTimeMillis()
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdownSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(countdownSeconds = it.countdownSeconds - 1) }
            }
            _uiState.update { it.copy(isCountingDown = false) }
            playMusic()
            runBreathingCycle()
        }
    }

    private fun runBreathingCycle() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val technique = _uiState.value.activeTechnique ?: return@launch
            
            for (cycle in _uiState.value.currentCycle.._uiState.value.cycles) {
                _uiState.update { it.copy(currentCycle = cycle) }
                
                for (rep in _uiState.value.currentRepetition.._uiState.value.repetitions) {
                    _uiState.update { it.copy(currentRepetition = rep) }
                    
                    for (phaseIndex in technique.phases.indices) {
                        val phase = technique.phases[phaseIndex]
                        _uiState.update { 
                            it.copy(
                                currentPhaseIndex = phaseIndex, 
                                currentPhaseType = phase.type,
                                remainingSeconds = phase.durationSeconds 
                            ) 
                        }
                        
                        playVoice(phase.type, phase.durationSeconds)
                        
                        val durationMs = (phase.durationSeconds * 1000).toLong()
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < durationMs) {
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                            _uiState.update { it.copy(remainingSeconds = maxOf(0f, phase.durationSeconds - elapsed)) }
                            delay(100)
                        }
                        _uiState.update { it.copy(remainingSeconds = 0f) }
                    }
                }

                if (_uiState.value.isWimHofMode) {
                    _uiState.update { 
                        it.copy(
                            isWimHofRetentionPhase = true, 
                            remainingSeconds = 0f,
                            currentPhaseIndex = -1,
                            currentPhaseType = null
                        )
                    }
                    playVoice(null, 0f, "retention")
                    vibrateDouble()
                    
                    val retentionTime = _uiState.value.wimHofRetentionTime
                    _uiState.update { it.copy(remainingSeconds = retentionTime) }
                    val retentionStartTime = System.currentTimeMillis()
                    while (_uiState.value.isWimHofRetentionPhase) {
                        val elapsed = (System.currentTimeMillis() - retentionStartTime) / 1000f
                        _uiState.update { it.copy(remainingSeconds = maxOf(0f, retentionTime - elapsed)) }
                        delay(100)
                        if (_uiState.value.remainingSeconds <= 0f && retentionTime > 0f) break 
                    }
                    
                    if (_uiState.value.isWimHofRetentionPhase) {
                        finishRetention()
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isWimHofRecoveryPhase = true, 
                            remainingSeconds = _uiState.value.wimHofRecoveryTime,
                            currentPhaseType = PhaseType.HOLD_IN
                        )
                    }
                    playVoice(null, _uiState.value.wimHofRecoveryTime, "recovery")
                    vibrateDouble()
                    
                    val recoveryDuration = _uiState.value.wimHofRecoveryTime
                    val recoveryStartTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - recoveryStartTime < recoveryDuration * 1000) {
                        val elapsed = (System.currentTimeMillis() - recoveryStartTime) / 1000f
                        _uiState.update { it.copy(remainingSeconds = maxOf(0f, recoveryDuration - elapsed)) }
                        delay(100)
                    }
                    
                    _uiState.update { it.copy(isWimHofRecoveryPhase = false, currentPhaseType = null) }
                }
                
                _uiState.update { it.copy(currentRepetition = 1) }
            }
            stopBreathing()
            playFinalSound()
        }
    }

    fun finishRetention() {
        val total = _uiState.value.wimHofRetentionTime
        val spent = (total - _uiState.value.remainingSeconds).toInt()
        _uiState.update { 
            it.copy(
                isWimHofRetentionPhase = false,
                currentSessionRetentions = it.currentSessionRetentions + spent
            )
        }
    }

    fun stopBreathing() {
        timerJob?.cancel()
        timerJob = null
        countdownJob?.cancel()
        countdownJob = null
        
        musicPlayer.pause()
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        
        breathPlayer.pause()
        breathPlayer.stop()
        breathPlayer.clearMediaItems()
        
        finalPlayer.pause()
        finalPlayer.stop()
        finalPlayer.clearMediaItems()
        
        val duration = ((System.currentTimeMillis() - trainingStartTime) / 1000).toInt()
        if (duration > 10) {
            saveToHistory(duration)
        }

        _uiState.update {
            it.copy(
                isRunning = false,
                isCountingDown = false,
                isWimHofRetentionPhase = false,
                isWimHofRecoveryPhase = false,
                currentPhaseType = null
            )
        }
    }

    private fun playVoice(phaseType: PhaseType?, duration: Float, special: String? = null) {
        if (!_uiState.value.isRunning) return
        
        val voicePrefix = if (_uiState.value.selectedVoice == "Male") "m" else "z"
        
        val baseName = when {
            special == "retention" -> null
            special == "recovery" -> "${voicePrefix}_vdoh_nos"
            phaseType == PhaseType.INHALE -> "${voicePrefix}_vdoh_nos"
            phaseType == PhaseType.EXHALE -> "${voicePrefix}_vidoh_rot"
            else -> null
        } ?: return

        val availableDurations = if (baseName.contains("vdoh_nos")) listOf(1, 2, 3, 4, 8) else listOf(1, 2, 3, 5, 10)
        val bestDuration = availableDurations.minByOrNull { abs(it - duration) } ?: availableDurations.first()
        
        val speed = bestDuration.toFloat() / duration.coerceAtLeast(0.1f)
        
        val fileName = "${baseName}_${bestDuration}sec"
        
        val resId = getApplication<Application>().resources.getIdentifier(fileName, "raw", getApplication<Application>().packageName)
        if (resId != 0) {
            val uri = Uri.parse("android.resource://${getApplication<Application>().packageName}/$resId")
            breathPlayer.setMediaItem(MediaItem.fromUri(uri))
            breathPlayer.setPlaybackParameters(PlaybackParameters(speed))
            breathPlayer.prepare()
            breathPlayer.play()
        }
        vibrate()
    }

    private fun playMusic() {
        if (!_uiState.value.isRunning && !_uiState.value.isCountingDown) return
        
        val music = _uiState.value.selectedMusic ?: return
        if (music.id == "none") {
            musicPlayer.pause()
            return
        }

        val mediaItem = when {
            music.resId != null -> MediaItem.fromUri(Uri.parse("android.resource://${getApplication<Application>().packageName}/${music.resId}"))
            music.uri != null -> {
                if (isUriAccessible(music.uri)) {
                    MediaItem.fromUri(music.uri)
                } else {
                    Log.w("BreathingViewModel", "Music URI not accessible, falling back")
                    val fallback = defaultMusic[1]
                    selectMusic(fallback)
                    MediaItem.fromUri(Uri.parse("android.resource://${getApplication<Application>().packageName}/${fallback.resId}"))
                }
            }
            else -> return
        }

        musicPlayer.setMediaItem(mediaItem)
        musicPlayer.prepare()
        musicPlayer.play()
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            getApplication<Application>().contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) { false }
    }

    fun savePreset(technique: BreathingTechnique) {
        val newList = _uiState.value.userPresets + technique
        _uiState.update { it.copy(userPresets = newList) }
        savePresetsToPrefs(newList)
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun deletePreset(techniqueId: String) {
        val newList = _uiState.value.userPresets.filter { it.id != techniqueId }
        _uiState.update { it.copy(userPresets = newList) }
        savePresetsToPrefs(newList)
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun savePresetsToPrefs(list: List<BreathingTechnique>) {
        val jsonArray = JSONArray()
        list.forEach { tech ->
            val obj = JSONObject()
            obj.put("id", tech.id)
            obj.put("name", tech.name)
            obj.put("description", tech.description)
            obj.put("isCustom", tech.isCustom)
            val phasesArray = JSONArray()
            tech.phases.forEach { phase ->
                val pObj = JSONObject()
                pObj.put("type", phase.type.name)
                pObj.put("duration", phase.durationSeconds.toDouble())
                phasesArray.put(pObj)
            }
            obj.put("phases", phasesArray)
            jsonArray.put(obj)
        }
        prefs.edit { putString("user_presets", jsonArray.toString()) }
    }

    private fun loadUserPresets() {
        val json = prefs.getString("user_presets", null) ?: return
        try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<BreathingTechnique>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val phasesList = mutableListOf<BreathingPhase>()
                val pArray = obj.getJSONArray("phases")
                for (j in 0 until pArray.length()) {
                    val pObj = pArray.getJSONObject(j)
                    val savedType = pObj.getString("type")
                    val type = try {
                        PhaseType.valueOf(savedType)
                    } catch (e: Exception) {
                        if (savedType == "HOLD") PhaseType.HOLD_IN else PhaseType.INHALE
                    }
                    phasesList.add(BreathingPhase(type, pObj.getDouble("duration").toFloat()))
                }
                list.add(BreathingTechnique(
                    id = obj.getString("id"), 
                    name = obj.getString("name"), 
                    description = obj.optString("description", ""),
                    phases = phasesList, 
                    isCustom = obj.optBoolean("isCustom", false)
                ))
            }
            _uiState.update { it.copy(userPresets = list) }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading presets: ${e.message}")
        }
    }

    private fun saveToHistory(durationSeconds: Int) {
        val technique = _uiState.value.activeTechnique ?: return
        val record = TrainingRecord(
            date = System.currentTimeMillis(),
            techniqueName = technique.name,
            circles = _uiState.value.currentCycle,
            durationTotalSeconds = durationSeconds,
            retentions = _uiState.value.currentSessionRetentions
        )
        val newList = _uiState.value.trainingHistory + record
        _uiState.update { it.copy(trainingHistory = newList) }
        saveHistoryToPrefs(newList)
        updateStreak()
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun saveHistoryToPrefs(list: List<TrainingRecord>) {
        val jsonArray = JSONArray()
        list.takeLast(100).forEach { rec ->
            val obj = JSONObject()
            obj.put("date", rec.date)
            obj.put("name", rec.techniqueName)
            obj.put("circles", rec.circles)
            obj.put("duration", rec.durationTotalSeconds)
            obj.put("retentions", JSONArray(rec.retentions))
            jsonArray.put(obj)
        }
        prefs.edit { putString("training_history", jsonArray.toString()) }
    }

    private fun loadHistory() {
        val json = prefs.getString("training_history", null) ?: return
        try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<TrainingRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val retArray = obj.optJSONArray("retentions")
                val rets = mutableListOf<Int>()
                if (retArray != null) {
                    for (j in 0 until retArray.length()) rets.add(retArray.getInt(j))
                }
                list.add(TrainingRecord(
                    obj.getLong("date"), 
                    obj.getString("name"), 
                    obj.getInt("circles"), 
                    obj.getInt("duration"),
                    rets
                ))
            }
            _uiState.update { it.copy(trainingHistory = list) }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading history: ${e.message}")
        }
    }

    private fun updateStreak() {
        val lastDate = prefs.getLong("last_training_date", 0)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = today - 24 * 60 * 60 * 1000
        var currentStreak = prefs.getInt("streak_count", 0)
        if (lastDate < today) {
            if (lastDate >= yesterday) currentStreak++ else currentStreak = 1
            prefs.edit { 
                putLong("last_training_date", today)
                putInt("streak_count", currentStreak)
            }
            _uiState.update { it.copy(streakCount = currentStreak) }
        }
    }

    private fun updateStreakOnStart() {
        val lastDate = prefs.getLong("last_training_date", 0)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val yesterday = today - 24 * 60 * 60 * 1000
        if (lastDate < yesterday) {
            prefs.edit { putInt("streak_count", 0) }
            _uiState.update { it.copy(streakCount = 0) }
        }
    }

    fun setReminder(hour: Int, minute: Int, date: String? = null) {
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        if (date == null) {
            _uiState.update { it.copy(reminderTime = timeStr) }
            prefs.edit { putString("reminder_time", timeStr) }
            scheduleNotification(hour, minute)
        } else {
            val newDayReminders = _uiState.value.dayReminders.toMutableMap()
            newDayReminders[date] = timeStr
            _uiState.update { it.copy(dayReminders = newDayReminders) }
            saveDayRemindersToPrefs(newDayReminders)
        }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun cancelReminder(date: String? = null) {
        if (date == null) {
            _uiState.update { it.copy(reminderTime = null) }
            prefs.edit { remove("reminder_time") }
            cancelNotification()
        } else {
            val newDayReminders = _uiState.value.dayReminders.toMutableMap()
            newDayReminders.remove(date)
            _uiState.update { it.copy(dayReminders = newDayReminders) }
            saveDayRemindersToPrefs(newDayReminders)
        }
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun saveDayRemindersToPrefs(map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit { putString("day_reminders", obj.toString()) }
    }

    private fun scheduleNotification(hour: Int, minute: Int) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.glazev.breathingtrainer.NOTIFICATION_ALARM")
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelNotification() {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.glazev.breathingtrainer.NOTIFICATION_ALARM")
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    private fun parseDayReminders(json: String): Map<String, String> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { map[it] = obj.getString(it) }
            map
        } catch (e: Exception) { emptyMap() }
    }

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                _uiState.update { it.copy(userEmail = auth.currentUser?.email) }
                syncWithCloud()
            }
            .addOnFailureListener { e ->
                Log.e("Auth", "Google sign in failed", e)
            }
    }

    fun signInWithYandexToken(yandexToken: String) {
        Log.d("YandexAuth", "Received Yandex Auth Token successfully")
        val label = "Яндекс (Token: ${yandexToken.take(8)}...)"
        val current = auth.currentUser
        if (current == null) {
            auth.signInAnonymously().addOnSuccessListener {
                _uiState.update { it.copy(userEmail = label) }
                syncWithCloud()
            }.addOnFailureListener {
                _uiState.update { it.copy(userEmail = label) }
            }
        } else {
            _uiState.update { it.copy(userEmail = label) }
            syncWithCloud()
        }
    }

    fun signInWithVKToken(accessToken: String, userId: String) {
        Log.d("VKIDAuth", "Received VK ID Token for user $userId")
        val label = "VK ID ($userId)"
        val current = auth.currentUser
        if (current == null) {
            auth.signInAnonymously().addOnSuccessListener {
                _uiState.update { it.copy(userEmail = label) }
                syncWithCloud()
            }.addOnFailureListener {
                _uiState.update { it.copy(userEmail = label) }
            }
        } else {
            _uiState.update { it.copy(userEmail = label) }
            syncWithCloud()
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.update { it.copy(userEmail = null) }
    }

    fun syncWithCloud() {
        val user = auth.currentUser ?: return
        _uiState.update { it.copy(isSyncing = true) }
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val cloudPremium = doc.getBoolean("isPremium") ?: false
                    if (cloudPremium) setPremiumStatus(true)
                    val voice = doc.getString("selectedVoice")
                    if (voice != null) updateVoice(voice)
                    val streak = doc.getLong("streakCount")?.toInt() ?: 0
                    _uiState.update { it.copy(streakCount = streak) }
                    prefs.edit { putInt("streak_count", streak) }
                } else {
                    pushDataToCloud()
                }
                _uiState.update { it.copy(isSyncing = false) }
            }
            .addOnFailureListener { _uiState.update { it.copy(isSyncing = false) } }
    }

    private fun pushDataToCloud() {
        val user = auth.currentUser ?: return
        val data = hashMapOf(
            "isPremium" to _uiState.value.isPremium,
            "selectedVoice" to _uiState.value.selectedVoice,
            "streakCount" to _uiState.value.streakCount,
            "vibrationEnabled" to _uiState.value.vibrationEnabled,
            "musicVolume" to _uiState.value.musicVolume,
            "breathVolume" to _uiState.value.breathVolume,
            "finalSoundVolume" to _uiState.value.finalSoundVolume,
            "finalSoundId" to (_uiState.value.finalSound?.id ?: "final1")
        )
        db.collection("users").document(user.uid).set(data)
    }

    override fun onCleared() {
        super.onCleared()
        breathPlayer.release()
        musicPlayer.release()
        finalPlayer.release()
    }
}
