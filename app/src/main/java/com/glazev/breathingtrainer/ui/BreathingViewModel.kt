package com.glazev.breathingtrainer.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.glazev.breathingtrainer.AdConfig
import com.glazev.breathingtrainer.BuildConfig
import com.glazev.breathingtrainer.R
import com.glazev.breathingtrainer.auth.AuthTokenExchangeClient
import com.glazev.breathingtrainer.auth.ExternalAuthProvider
import com.glazev.breathingtrainer.auth.VkAuthorizationRequest
import com.glazev.breathingtrainer.auth.VkPkceSession
import com.glazev.breathingtrainer.billing.PremiumEntitlement
import com.glazev.breathingtrainer.model.BreathingPhase
import com.glazev.breathingtrainer.model.BreathingTechnique
import com.glazev.breathingtrainer.model.DefaultTechniques
import com.glazev.breathingtrainer.model.PhaseType
import com.glazev.breathingtrainer.notifications.ReminderScheduler
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import java.security.MessageDigest
import java.lang.ref.WeakReference
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
    val wimHofRecoveryTime: Float = 15f,
    val selectedVoice: String = "Male",
    val selectedMusic: BackgroundMusic? = null,
    val currentPhaseIndex: Int = 0,
    val currentPhaseType: PhaseType? = null,
    val remainingSeconds: Float = 0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
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
    val authProviderLabel: String? = null,
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
    private val authTokenExchangeClient = AuthTokenExchangeClient(BuildConfig.AUTH_EXCHANGE_URL)
    private val vkPkceSession = VkPkceSession()
    
    private val _uiState = MutableStateFlow(BreathingUiState(
        isPremium = false,
        musicVolume = prefs.getFloat("music_volume", 0.75f),
        breathVolume = prefs.getFloat("breath_volume", 0.3f),
        finalSoundVolume = prefs.getFloat("final_sound_volume", 0.3f),
        userEmail = auth.currentUser?.let { it.email ?: it.displayName ?: "Аккаунт" },
        authProviderLabel = auth.currentUser?.providerData
            ?.takeIf { providers -> providers.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } }
            ?.let { "Google" },
        selectedMusic = MusicSelectionPolicy.resolve(
            defaultMusic = defaultMusic,
            customMusic = emptyList(),
            selectedId = prefs.getString("selected_music_id", "main")
        ),
        selectedVoice = prefs.getString("selected_voice", "Male") ?: "Male",
        reminderTime = prefs.getString("reminder_time", null),
        dayReminders = parseDayReminders(prefs.getString("day_reminders", "{}") ?: "{}"),
        streakCount = prefs.getInt("streak_count", 0),
        vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
    ))
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private var timerJob: Job? = null
    private var activeLocalProfileId = auth.currentUser?.uid ?: GUEST_PROFILE_ID
    private var cloudSyncGeneration = 0
    private var cloudReadyUid: String? = null
    private var cloudChangedWhileLoading = false
    private var cloudWriteJob: Job? = null
    private val premiumEntitlement = PremiumEntitlement()
    private var countdownJob: Job? = null
    private val sessionClock = TrainingSessionClock()
    private val audioInterruptionPolicy = AudioInterruptionPolicy()
    private var audioPlaybackAllowed = false
    private var completionSoundOwnsFocus = false
    private val audioFocusController = AudioFocusController(application) { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioPlaybackAllowed = true
                if (audioInterruptionPolicy.onGain(_uiState.value.isPaused)) {
                    resumeTrainingInternal(requestFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                audioPlaybackAllowed = false
                stopCompletionSoundForInterruption()
                if (audioInterruptionPolicy.onLoss(isTransient = true, isRunning = _uiState.value.isRunning)) {
                    pauseTrainingInternal(releaseFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioPlaybackAllowed = false
                stopCompletionSoundForInterruption()
                if (audioInterruptionPolicy.onLoss(isTransient = false, isRunning = _uiState.value.isRunning)) {
                    pauseTrainingInternal(releaseFocus = false)
                }
            }
        }
    }
    private val breathPlayerDelegate = lazy {
        createAudioPlayer("breathing voice").apply {
            volume = _uiState.value.breathVolume
        }
    }
    private val breathPlayer by breathPlayerDelegate
    private val musicPlayerDelegate = lazy {
        createAudioPlayer("background music").apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = _uiState.value.musicVolume
        }
    }
    private val musicPlayer by musicPlayerDelegate
    private val finalPlayerDelegate = lazy {
        createAudioPlayer("completion sound", abandonFocusWhenEnded = true).apply {
            volume = _uiState.value.finalSoundVolume
        }
    }
    private val finalPlayer by finalPlayerDelegate

    private fun createAudioPlayer(
        label: String,
        abandonFocusWhenEnded: Boolean = false
    ): ExoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            false
        )
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(AUDIO_TAG, "$label playback failed", error)
                runCatching {
                    stop()
                    clearMediaItems()
                }
                if (abandonFocusWhenEnded) finishCompletionSound()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (abandonFocusWhenEnded && playbackState == Player.STATE_ENDED) {
                    finishCompletionSound()
                }
            }
        })
    }

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var pendingTrainingStart: PendingTrainingStart? = null
    private var trainingStartTimeoutJob: Job? = null
    private val trainingStartGate = SingleFlightGate()
    private val interstitialAdLoader by lazy {
        InterstitialAdLoader(application).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isInterstitialLoading = false
                    if (pendingTrainingStart != null) {
                        showLoadedInterstitial(ad)
                    } else {
                        interstitialAd = ad
                    }
                    Log.d("Ads", "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    isInterstitialLoading = false
                    Log.e("Ads", "Failed to load interstitial ad: ${error.description}")
                    completePendingTrainingStart()
                }
            })
        }
    }

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
        // Remove the legacy client-controlled entitlement. RuStore is the source of truth.
        prefs.edit { remove("is_premium") }
        loadCustomMusic()
        loadUserPresets()
        loadHistory()
        checkPurchases()
        loadProductsInfo()
        loadFinalSoundConfig()
        initializeProfileStorage()
        pruneUnusedPersistedUriPermissions()
        ReminderScheduler.restoreAll(application)
        
        if (auth.currentUser != null) {
            refreshAuthIdentity()
            syncWithCloud()
        }
        refreshStreak()
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
        saveCurrentLocalProfile()
        pruneUnusedPersistedUriPermissions()
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
        val safeVolume = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(finalSoundVolume = safeVolume) }
        if (finalPlayerDelegate.isInitialized()) finalPlayer.volume = safeVolume
        prefs.edit { putFloat("final_sound_volume", safeVolume) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun playFinalSound(): Boolean {
        val sound = _uiState.value.finalSound ?: return false
        if (!requestAudioFocus()) return false
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
            else -> return false
        }
        completionSoundOwnsFocus = true
        return runCatching {
            finalPlayer.setMediaItem(mediaItem)
            finalPlayer.prepare()
            finalPlayer.play()
            true
        }.onFailure { error ->
            Log.e(AUDIO_TAG, "Unable to start completion sound", error)
            finishCompletionSound()
        }.getOrDefault(false)
    }

    private fun finishCompletionSound() {
        if (!completionSoundOwnsFocus) return
        completionSoundOwnsFocus = false
        audioPlaybackAllowed = false
        audioFocusController.abandon()
    }

    private fun stopCompletionSoundForInterruption() {
        if (!completionSoundOwnsFocus) return
        if (finalPlayerDelegate.isInitialized()) finalPlayer.pause()
        finishCompletionSound()
    }

    private fun requestAudioFocus(): Boolean {
        audioPlaybackAllowed = audioFocusController.request()
        if (!audioPlaybackAllowed) {
            Log.w(AUDIO_TAG, "Audio focus was denied; the timer will continue without sound")
        }
        return audioPlaybackAllowed
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        prefs.edit { putBoolean("vibration_enabled", enabled) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun vibrate(duration: Long = 100) {
        if (!_uiState.value.vibrationEnabled) return
        vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateDouble() {
        if (!_uiState.value.vibrationEnabled) return
        viewModelScope.launch {
            vibrate(100)
            delay(200)
            vibrate(100)
        }
    }

    private fun loadInterstitialAdIfNeeded() {
        if (_uiState.value.isPremium || interstitialAd != null || isInterstitialLoading) return
        val adRequestConfiguration = AdRequestConfiguration.Builder(AdConfig.INTERSTITIAL_AD_UNIT_ID).build()
        isInterstitialLoading = true
        runCatching { interstitialAdLoader.loadAd(adRequestConfiguration) }
            .onFailure { error ->
                isInterstitialLoading = false
                Log.e("Ads", "Interstitial load request failed", error)
                completePendingTrainingStart()
            }
    }

    fun preloadInterstitialAd() {
        loadInterstitialAdIfNeeded()
    }

    fun startTrainingWithAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (!trainingStartGate.tryStart()) {
            Log.d("Ads", "Ignoring duplicate training start while an ad request is active")
            return
        }

        if (_uiState.value.isPremium) {
            trainingStartGate.finish()
            onAdDismissed()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            trainingStartGate.finish()
            return
        }

        pendingTrainingStart = PendingTrainingStart(WeakReference(activity), onAdDismissed)
        val readyAd = interstitialAd
        if (readyAd != null) {
            interstitialAd = null
            showLoadedInterstitial(readyAd)
        } else {
            trainingStartTimeoutJob?.cancel()
            trainingStartTimeoutJob = viewModelScope.launch {
                delay(AD_START_TIMEOUT_MS)
                if (pendingTrainingStart != null) {
                    Log.w("Ads", "Ad was not ready before timeout; starting training without it")
                    completePendingTrainingStart()
                }
            }
            loadInterstitialAdIfNeeded()
        }
    }

    private fun showLoadedInterstitial(ad: InterstitialAd) {
        val request = pendingTrainingStart ?: run {
            interstitialAd = ad
            return
        }
        val activity = request.activity.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            cancelPendingTrainingStart()
            interstitialAd = ad
            return
        }

        trainingStartTimeoutJob?.cancel()
        trainingStartTimeoutJob = null
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() = Unit

            override fun onAdFailedToShow(error: AdError) {
                Log.e("Ads", "Ad failed to show: ${error.description}")
                completePendingTrainingStart()
                loadInterstitialAdIfNeeded()
            }

            override fun onAdDismissed() {
                completePendingTrainingStart()
                loadInterstitialAdIfNeeded()
            }

            override fun onAdClicked() = Unit
            override fun onAdImpression(impressionData: ImpressionData?) = Unit
        })
        runCatching { ad.show(activity) }
            .onFailure { error ->
                Log.e("Ads", "Ad show request failed", error)
                completePendingTrainingStart()
                loadInterstitialAdIfNeeded()
            }
    }

    private fun completePendingTrainingStart() {
        val request = pendingTrainingStart ?: return
        pendingTrainingStart = null
        trainingStartTimeoutJob?.cancel()
        trainingStartTimeoutJob = null
        trainingStartGate.finish()

        val activity = request.activity.get()
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            request.onContinue()
        }
    }

    private fun cancelPendingTrainingStart() {
        pendingTrainingStart = null
        trainingStartTimeoutJob?.cancel()
        trainingStartTimeoutJob = null
        trainingStartGate.finish()
    }

    fun selectTechniqueById(id: String) {
        val allTechniques = DefaultTechniques.list + _uiState.value.userPresets
        val found = allTechniques.find { it.id == id } ?: DefaultTechniques.SquareBreathing
        selectPreset(found)
    }

    fun handleWidgetStart(
        techniqueId: String,
        isSos: Boolean,
        activity: Activity
    ) {
        selectTechniqueById(techniqueId)

        // Независимо от режима, мы СРАЗУ переключаемся на экран тренировки, 
        // чтобы пользователь не видел "лишних экранов" (настроек).
        viewModelScope.launch {
            _navigationEvent.send("training")
        }

        if (isSos) {
            // SOS режим: Запускается моментально БЕЗ РЕКЛАМЫ
            stopTraining()
            startTraining()
        } else {
            // Обычный запуск: Ждем загрузки рекламы (пользователь видит экран дыхания, но таймер стоит)
            startTrainingWithAd(activity) {
                stopTraining()
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
            val selectedMusicId = prefs.getString("selected_music_id", "main")
            val selectedMusic = MusicSelectionPolicy.resolve(
                defaultMusic = defaultMusic,
                customMusic = customList,
                selectedId = selectedMusicId
            )
            _uiState.update {
                it.copy(customMusicList = customList, selectedMusic = selectedMusic)
            }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading custom music: ${e.message}")
            _uiState.update { it.copy(customMusicList = emptyList()) }
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
        val state = _uiState.value
        if (state.customMusicList.none { it.id == musicId }) return
        val removedList = state.customMusicList.filterNot { it.id == musicId }
        val selectedMusic = if (state.selectedMusic?.id == musicId) defaultMusic[1] else state.selectedMusic
        _uiState.update {
            it.copy(customMusicList = removedList, selectedMusic = selectedMusic)
        }
        if (state.selectedMusic?.id == musicId) {
            prefs.edit { putString("selected_music_id", defaultMusic[1].id) }
            if (state.isRunning) playMusic()
        }
        saveCustomMusic()
        saveCurrentLocalProfile()
        pruneUnusedPersistedUriPermissions()
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun deleteCustomMusic(music: BackgroundMusic) {
        removeCustomMusic(music.id)
    }

    fun selectMusic(music: BackgroundMusic) {
        _uiState.update { it.copy(selectedMusic = music) }
        prefs.edit { putString("selected_music_id", music.id) }
        if (_uiState.value.isRunning) {
            playMusic()
        }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun updateVoice(voice: String) {
        _uiState.update { it.copy(selectedVoice = voice) }
        prefs.edit { putString("selected_voice", voice) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun updateMusicVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(musicVolume = safeVolume) }
        if (musicPlayerDelegate.isInitialized()) musicPlayer.volume = safeVolume
        prefs.edit { putFloat("music_volume", safeVolume) }
        if (auth.currentUser != null) pushDataToCloud()
    }

    fun updateBreathVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(breathVolume = safeVolume) }
        if (breathPlayerDelegate.isInitialized()) breathPlayer.volume = safeVolume
        prefs.edit { putFloat("breath_volume", safeVolume) }
        if (auth.currentUser != null) pushDataToCloud()
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
                applyVerifiedPremiumStatus(hasPremium)
            }
            .addOnFailureListener { e -> Log.e("RuStorePay", "Failed to check purchases: ${e.message}") }
    }

    private fun applyVerifiedPremiumStatus(enabled: Boolean) {
        premiumEntitlement.applyVerifiedResult(enabled)
        _uiState.update { it.copy(isPremium = premiumEntitlement.isPremium) }
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
            val intent = Intent(Intent.ACTION_VIEW, "https://apps.rustore.ru/subscriber/subscriptions".toUri())
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
                activeTechnique = null,
                repetitions = if (enabled) {
                    TrainingModePolicy.WIM_HOF_REPETITIONS
                } else {
                    TrainingModePolicy.NORMAL_REPETITIONS
                },
                cycles = if (enabled) {
                    TrainingModePolicy.WIM_HOF_CYCLES
                } else {
                    TrainingModePolicy.NORMAL_CYCLES
                },
                currentCycle = 1,
                currentRepetition = 1,
                isWimHofRetentionPhase = false,
                isWimHofRecoveryPhase = false,
                currentPhaseType = null
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
        _uiState.update { it.copy(cycles = TrainingModePolicy.cycles(cycles)) }
    }

    fun updateRepetitions(reps: Int) {
        _uiState.update { it.copy(repetitions = TrainingModePolicy.repetitions(reps)) }
    }

    fun selectPreset(technique: BreathingTechnique) {
        _uiState.update {
            it.copy(
                activeTechnique = technique,
                isWimHofMode = false,
                repetitions = if (it.isWimHofMode) {
                    TrainingModePolicy.NORMAL_REPETITIONS
                } else {
                    TrainingModePolicy.repetitions(it.repetitions)
                },
                cycles = TrainingModePolicy.cycles(technique.cycles),
                currentCycle = 1,
                currentRepetition = 1,
                isWimHofRetentionPhase = false,
                isWimHofRecoveryPhase = false,
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

    fun pauseTraining() {
        audioInterruptionPolicy.clear()
        pauseTrainingInternal(releaseFocus = true)
    }

    private fun pauseTrainingInternal(releaseFocus: Boolean) {
        if (!sessionClock.pause()) return
        if (musicPlayerDelegate.isInitialized()) musicPlayer.pause()
        if (breathPlayerDelegate.isInitialized()) breathPlayer.pause()
        if (releaseFocus) {
            audioPlaybackAllowed = false
            audioFocusController.abandon()
        }
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumeTraining() {
        audioInterruptionPolicy.clear()
        resumeTrainingInternal(requestFocus = true)
    }

    private fun resumeTrainingInternal(requestFocus: Boolean) {
        if (!sessionClock.resume()) return
        if (requestFocus) requestAudioFocus()
        _uiState.update { it.copy(isRunning = true, isPaused = false) }
        if (audioPlaybackAllowed) {
            if (musicPlayerDelegate.isInitialized() && musicPlayer.mediaItemCount > 0) musicPlayer.play()
            if (breathPlayerDelegate.isInitialized() && breathPlayer.mediaItemCount > 0) breathPlayer.play()
        }
    }

    fun onHostStopped() {
        audioInterruptionPolicy.clear()
        if (_uiState.value.isRunning) {
            pauseTrainingInternal(releaseFocus = true)
        } else {
            audioPlaybackAllowed = false
            if (musicPlayerDelegate.isInitialized()) musicPlayer.pause()
            if (breathPlayerDelegate.isInitialized()) breathPlayer.pause()
            if (finalPlayerDelegate.isInitialized()) finalPlayer.pause()
            finishCompletionSound()
            audioFocusController.abandon()
        }
    }

    fun startBreathing(technique: BreathingTechnique) {
        val validPhases = technique.phases.filter { phase ->
            phase.durationSeconds.isFinite() && phase.durationSeconds > 0f
        }
        if (validPhases.isEmpty()) {
            Toast.makeText(getApplication(), "Укажите длительность хотя бы одной фазы", Toast.LENGTH_SHORT).show()
            return
        }

        val validTechnique = if (validPhases.size == technique.phases.size) {
            technique
        } else {
            technique.copy(phases = validPhases)
        }
        val isWimHof = validTechnique.id == "wim_hof"
        val sessionRepetitions = TrainingModePolicy.repetitions(_uiState.value.repetitions)
        val sessionCycles = if (isWimHof) {
            TrainingModePolicy.cycles(_uiState.value.cycles)
        } else {
            TrainingModePolicy.cycles(validTechnique.cycles)
        }
        if (!sessionClock.begin()) return
        audioInterruptionPolicy.clear()
        requestAudioFocus()
        _uiState.update {
            it.copy(
                activeTechnique = validTechnique,
                isWimHofMode = isWimHof,
                repetitions = sessionRepetitions,
                cycles = sessionCycles,
                currentCycle = 1,
                currentRepetition = 1,
                currentPhaseIndex = 0,
                currentPhaseType = validTechnique.phases.first().type,
                isCountingDown = true,
                countdownSeconds = 5,
                isRunning = true,
                isPaused = false,
                currentSessionRetentions = emptyList(),
                inhaleTime = validTechnique.phases.find { p -> p.type == PhaseType.INHALE }?.durationSeconds ?: (if (isWimHof) 1.5f else 0f),
                inhaleHoldTime = validTechnique.phases.find { p -> p.type == PhaseType.HOLD_IN }?.durationSeconds ?: 0f,
                exhaleTime = validTechnique.phases.find { p -> p.type == PhaseType.EXHALE }?.durationSeconds ?: (if (isWimHof) 1.5f else 0f),
                exhaleHoldTime = validTechnique.phases.find { p -> p.type == PhaseType.HOLD_OUT }?.durationSeconds ?: 0f
            )
        }
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdownSeconds > 0) {
                runPausableTimer(1f)
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
            val targetCycles = TrainingModePolicy.cycles(_uiState.value.cycles)
            val targetRepetitions = TrainingModePolicy.repetitions(_uiState.value.repetitions)
            
            for (cycle in _uiState.value.currentCycle..targetCycles) {
                _uiState.update { it.copy(currentCycle = cycle) }
                
                for (rep in _uiState.value.currentRepetition..targetRepetitions) {
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
                        
                        runPausableTimer(phase.durationSeconds) { remaining ->
                            _uiState.update { it.copy(remainingSeconds = remaining) }
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
                    
                    _uiState.update { it.copy(remainingSeconds = 0f) }
                    runRetentionStopwatch(
                        shouldContinue = { _uiState.value.isWimHofRetentionPhase }
                    ) { elapsed ->
                        _uiState.update { it.copy(remainingSeconds = elapsed) }
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
                    runPausableTimer(recoveryDuration) { remaining ->
                        _uiState.update { it.copy(remainingSeconds = remaining) }
                    }
                    
                    _uiState.update { it.copy(isWimHofRecoveryPhase = false, currentPhaseType = null) }
                }
                
                _uiState.update { it.copy(currentRepetition = 1) }
            }
            stopBreathing(releaseAudioFocus = false)
            if (!playFinalSound()) {
                audioPlaybackAllowed = false
                audioFocusController.abandon()
            }
        }
    }

    fun finishRetention() {
        while (true) {
            val current = _uiState.value
            if (!current.isWimHofRetentionPhase) return
            val spent = TrainingModePolicy.stopwatchElapsedSeconds(current.remainingSeconds)
            val completed = current.copy(
                isWimHofRetentionPhase = false,
                currentSessionRetentions = current.currentSessionRetentions + spent
            )
            if (_uiState.compareAndSet(current, completed)) return
        }
    }

    fun stopBreathing() = stopBreathing(releaseAudioFocus = true)

    private fun stopBreathing(releaseAudioFocus: Boolean) {
        val duration = sessionClock.finishSeconds()
        timerJob?.cancel()
        timerJob = null
        countdownJob?.cancel()
        countdownJob = null
        
        if (musicPlayerDelegate.isInitialized()) {
            musicPlayer.pause()
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
        }
        
        if (breathPlayerDelegate.isInitialized()) {
            breathPlayer.pause()
            breathPlayer.stop()
            breathPlayer.clearMediaItems()
        }
        
        if (finalPlayerDelegate.isInitialized()) {
            finalPlayer.pause()
            finalPlayer.stop()
            finalPlayer.clearMediaItems()
        }

        audioInterruptionPolicy.clear()
        completionSoundOwnsFocus = false
        if (releaseAudioFocus) {
            audioPlaybackAllowed = false
            audioFocusController.abandon()
        }
        
        if (duration != null && duration > 10) {
            saveToHistory(duration)
        }

        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isCountingDown = false,
                isWimHofRetentionPhase = false,
                isWimHofRecoveryPhase = false,
                currentPhaseType = null
            )
        }
    }

    private suspend fun runPausableTimer(
        durationSeconds: Float,
        shouldContinue: () -> Boolean = { true },
        onTick: (Float) -> Unit = {}
    ) {
        var remainingMillis = (durationSeconds.coerceAtLeast(0f) * 1_000f).toLong()
        var previousTick = SystemClock.elapsedRealtime()
        onTick(remainingMillis / 1_000f)

        while (remainingMillis > 0L && sessionClock.isActive && shouldContinue()) {
            if (_uiState.value.isPaused) {
                delay(100)
                previousTick = SystemClock.elapsedRealtime()
                continue
            }

            delay(minOf(100L, remainingMillis.coerceAtLeast(1L)))
            val currentTick = SystemClock.elapsedRealtime()
            if (_uiState.value.isPaused) {
                previousTick = currentTick
                continue
            }
            remainingMillis = (remainingMillis - (currentTick - previousTick).coerceAtLeast(0L))
                .coerceAtLeast(0L)
            previousTick = currentTick
            onTick(remainingMillis / 1_000f)
        }
    }

    private suspend fun runRetentionStopwatch(
        shouldContinue: () -> Boolean,
        onTick: (Float) -> Unit
    ) {
        val stopwatch = RetentionStopwatch()
        onTick(0f)
        while (sessionClock.isActive && shouldContinue()) {
            delay(100L)
            onTick(stopwatch.tick(isPaused = _uiState.value.isPaused))
        }
    }

    private fun playVoice(phaseType: PhaseType?, duration: Float, special: String? = null) {
        if (!_uiState.value.isRunning) return
        vibrate()
        if (!audioPlaybackAllowed) return
        
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
        
        val resId = breathingVoiceResource(baseName, bestDuration)
        if (resId != 0) {
            val uri = "android.resource://${getApplication<Application>().packageName}/$resId".toUri()
            runCatching {
                breathPlayer.setMediaItem(MediaItem.fromUri(uri))
                breathPlayer.setPlaybackParameters(PlaybackParameters(speed))
                breathPlayer.prepare()
                breathPlayer.play()
            }.onFailure { error ->
                Log.e(AUDIO_TAG, "Unable to start breathing voice", error)
            }
        }
    }

    private fun breathingVoiceResource(baseName: String, durationSeconds: Int): Int =
        when (baseName) {
            "m_vdoh_nos" -> when (durationSeconds) {
                1 -> R.raw.m_vdoh_nos_1sec
                2 -> R.raw.m_vdoh_nos_2sec
                3 -> R.raw.m_vdoh_nos_3sec
                4 -> R.raw.m_vdoh_nos_4sec
                8 -> R.raw.m_vdoh_nos_8sec
                else -> 0
            }
            "z_vdoh_nos" -> when (durationSeconds) {
                1 -> R.raw.z_vdoh_nos_1sec
                2 -> R.raw.z_vdoh_nos_2sec
                3 -> R.raw.z_vdoh_nos_3sec
                4 -> R.raw.z_vdoh_nos_4sec
                8 -> R.raw.z_vdoh_nos_8sec
                else -> 0
            }
            "m_vidoh_rot" -> when (durationSeconds) {
                1 -> R.raw.m_vidoh_rot_1sec
                2 -> R.raw.m_vidoh_rot_2sec
                3 -> R.raw.m_vidoh_rot_3sec
                5 -> R.raw.m_vidoh_rot_5sec
                10 -> R.raw.m_vidoh_rot_10sec
                else -> 0
            }
            "z_vidoh_rot" -> when (durationSeconds) {
                1 -> R.raw.z_vidoh_rot_1sec
                2 -> R.raw.z_vidoh_rot_2sec
                3 -> R.raw.z_vidoh_rot_3sec
                5 -> R.raw.z_vidoh_rot_5sec
                10 -> R.raw.z_vidoh_rot_10sec
                else -> 0
            }
            else -> 0
        }

    private fun playMusic() {
        if (!_uiState.value.isRunning && !_uiState.value.isCountingDown) return
        if (!audioPlaybackAllowed) return
        
        val music = _uiState.value.selectedMusic ?: return
        if (music.id == "none") {
            musicPlayer.pause()
            return
        }

        val mediaItem = when {
            music.resId != null -> MediaItem.fromUri("android.resource://${getApplication<Application>().packageName}/${music.resId}".toUri())
            music.uri != null -> {
                if (isUriAccessible(music.uri)) {
                    MediaItem.fromUri(music.uri)
                } else {
                    Log.w("BreathingViewModel", "Music URI not accessible, falling back")
                    val fallback = defaultMusic[1]
                    selectMusic(fallback)
                    MediaItem.fromUri("android.resource://${getApplication<Application>().packageName}/${fallback.resId}".toUri())
                }
            }
            else -> return
        }

        runCatching {
            musicPlayer.setMediaItem(mediaItem)
            musicPlayer.prepare()
            musicPlayer.play()
        }.onFailure { error ->
            Log.e(AUDIO_TAG, "Unable to start background music", error)
        }
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
            obj.put("cycles", tech.cycles)
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
        val json = prefs.getString("user_presets", null)
        if (json == null) {
            _uiState.update { it.copy(userPresets = emptyList()) }
            return
        }
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
                    cycles = obj.optInt("cycles", 1).coerceIn(1, 100),
                    isCustom = obj.optBoolean("isCustom", false)
                ))
            }
            _uiState.update { it.copy(userPresets = list) }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading presets: ${e.message}")
            _uiState.update { it.copy(userPresets = emptyList()) }
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
        val newList = (_uiState.value.trainingHistory + record).takeLast(MAX_HISTORY)
        _uiState.update { it.copy(trainingHistory = newList) }
        saveHistoryToPrefs(newList)
        refreshStreak()
        if (auth.currentUser != null) pushDataToCloud()
    }

    private fun saveHistoryToPrefs(list: List<TrainingRecord>) {
        val jsonArray = JSONArray()
        list.takeLast(MAX_HISTORY).forEach { rec ->
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
        val json = prefs.getString("training_history", null)
        if (json == null) {
            _uiState.update { it.copy(trainingHistory = emptyList()) }
            return
        }
        try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<TrainingRecord>()
            val firstRecordIndex = (jsonArray.length() - MAX_HISTORY).coerceAtLeast(0)
            for (i in firstRecordIndex until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val retArray = obj.optJSONArray("retentions")
                val rets = mutableListOf<Int>()
                if (retArray != null) {
                    for (j in 0 until minOf(retArray.length(), MAX_RETENTIONS)) {
                        rets.add(retArray.optInt(j, 0).coerceAtLeast(0))
                    }
                }
                list.add(TrainingRecord(
                    obj.optLong("date", 0L).coerceAtLeast(0L),
                    obj.optString("name", "Тренировка").take(MAX_TEXT_LENGTH),
                    obj.optInt("circles", 0).coerceIn(0, 10_000),
                    obj.optInt("duration", 0).coerceIn(0, 86_400),
                    rets
                ))
            }
            _uiState.update { it.copy(trainingHistory = list) }
        } catch (e: Exception) {
            Log.e("BreathingViewModel", "Error loading history: ${e.message}")
            _uiState.update { it.copy(trainingHistory = emptyList()) }
        }
    }

    private fun refreshStreak(nowMillis: Long = System.currentTimeMillis()) {
        val history = _uiState.value.trainingHistory
        val currentStreak = StreakCalculator.calculate(
            trainingTimestamps = history.map { it.date },
            nowMillis = nowMillis
        )
        val lastTrainingDate = history.maxOfOrNull { it.date }?.coerceAtMost(nowMillis) ?: 0L
        prefs.edit {
            putLong("last_training_date", lastTrainingDate)
            putInt("streak_count", currentStreak)
        }
        _uiState.update { it.copy(streakCount = currentStreak) }
    }

    fun setReminder(hour: Int, minute: Int, date: String? = null) {
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        if (date == null) {
            ReminderScheduler.scheduleDaily(getApplication(), hour, minute)
            _uiState.update { it.copy(reminderTime = timeStr) }
            prefs.edit { putString("reminder_time", timeStr) }
        } else {
            if (!ReminderScheduler.scheduleDate(getApplication(), date, hour, minute)) {
                Toast.makeText(getApplication(), "Выбранное время уже прошло", Toast.LENGTH_LONG).show()
                return
            }
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
            ReminderScheduler.cancelDaily(getApplication())
        } else {
            ReminderScheduler.cancelDate(getApplication(), date)
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

    private fun parseDayReminders(json: String): Map<String, String> {
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { map[it] = obj.getString(it) }
            map
        } catch (e: Exception) { emptyMap() }
    }

    private fun profilePreferences(profileId: String): SharedPreferences {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(profileId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return getApplication<Application>().getSharedPreferences(
            "app_profile_${digest.take(24)}",
            Context.MODE_PRIVATE
        )
    }

    private fun pruneUnusedPersistedUriPermissions() {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        val heldPermissions = resolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .associateBy { it.uri.toString() }
        if (heldPermissions.isEmpty()) return

        val profileDirectory = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
        val profileFiles = profileDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("app_profile_") && file.name.endsWith(".xml")
        } ?: return

        val referencedUris = mutableSetOf<String>()
        collectCustomAudioReferences(prefs, heldPermissions.keys, referencedUris)
        profileFiles.forEach { file ->
            val preferenceName = file.name.removeSuffix(".xml")
            collectCustomAudioReferences(
                context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE),
                heldPermissions.keys,
                referencedUris
            )
        }

        UriPermissionPolicy.unusedPermissions(heldPermissions.keys, referencedUris).forEach { uriString ->
            val permission = heldPermissions[uriString] ?: return@forEach
            runCatching {
                resolver.releasePersistableUriPermission(
                    permission.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.onFailure { error ->
                Log.w("BreathingViewModel", "Unable to release unused audio permission", error)
            }
        }
    }

    private fun collectCustomAudioReferences(
        preferences: SharedPreferences,
        heldUris: Set<String>,
        destination: MutableSet<String>
    ) {
        preferences.getString("final_sound_uri", null)
            ?.takeIf(String::isNotBlank)
            ?.let(destination::add)

        val rawMusic = preferences.getString("custom_music_list", null) ?: return
        runCatching {
            val music = JSONArray(rawMusic)
            for (index in 0 until music.length()) {
                music.optJSONObject(index)?.optString("uri")
                    ?.takeIf(String::isNotBlank)
                    ?.let(destination::add)
            }
        }.onFailure {
            // Corrupt legacy JSON must not make us revoke a grant it may still mention.
            heldUris.filterTo(destination, rawMusic::contains)
        }
    }

    private fun cancelCurrentReminders() {
        val context = getApplication<Application>()
        ReminderScheduler.cancelDaily(context)
        _uiState.value.dayReminders.keys.forEach { ReminderScheduler.cancelDate(context, it) }
    }

    /**
     * Older releases kept every user's data in one SharedPreferences file.  Scoped
     * snapshots prevent a later Firebase account from inheriting and uploading the
     * previous account's local history and settings.
     */
    private fun initializeProfileStorage() {
        if (!prefs.getBoolean(PROFILE_STORAGE_INITIALIZED, false)) {
            saveCurrentLocalProfile()
            prefs.edit {
                putBoolean(PROFILE_STORAGE_INITIALIZED, true)
                if (activeLocalProfileId != GUEST_PROFILE_ID) {
                    putBoolean(FIRST_ACCOUNT_CLAIMED, true)
                }
            }
            return
        }

        if (profilePreferences(activeLocalProfileId).getBoolean(PROFILE_EXISTS, false)) {
            restoreLocalProfile(activeLocalProfileId)
        } else {
            resetLocalProfile()
            saveCurrentLocalProfile()
        }
    }

    private fun switchLocalProfile(profileId: String) {
        if (profileId == activeLocalProfileId) return

        val previousProfileId = activeLocalProfileId
        saveCurrentLocalProfile()
        cancelCurrentReminders()
        activeLocalProfileId = profileId

        val target = profilePreferences(profileId)
        if (target.getBoolean(PROFILE_EXISTS, false)) {
            restoreLocalProfile(profileId)
        } else if (
            previousProfileId == GUEST_PROFILE_ID &&
            profileId != GUEST_PROFILE_ID &&
            !prefs.getBoolean(FIRST_ACCOUNT_CLAIMED, false)
        ) {
            // One-time migration: the first account keeps data created before sign-in.
            prefs.edit { putBoolean(FIRST_ACCOUNT_CLAIMED, true) }
            saveCurrentLocalProfile()
        } else {
            resetLocalProfile()
            saveCurrentLocalProfile()
        }
        ReminderScheduler.restoreAll(getApplication())
    }

    private fun saveCurrentLocalProfile() {
        persistCurrentStateToFlatPreferences()
        val destination = profilePreferences(activeLocalProfileId)
        destination.edit {
            PROFILE_KEYS.forEach { remove(it) }
            PROFILE_KEYS.forEach { key ->
                when (val value = prefs.all[key]) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
            putBoolean(PROFILE_EXISTS, true)
        }
    }

    private fun restoreLocalProfile(profileId: String) {
        val source = profilePreferences(profileId)
        prefs.edit {
            PROFILE_KEYS.forEach { remove(it) }
            PROFILE_KEYS.forEach { key ->
                when (val value = source.all[key]) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        reloadUiFromFlatPreferences()
    }

    private fun resetLocalProfile() {
        prefs.edit {
            PROFILE_KEYS.forEach { remove(it) }
        }
        reloadUiFromFlatPreferences()
    }

    private fun persistCurrentStateToFlatPreferences() {
        val state = _uiState.value
        prefs.edit {
            putString("selected_voice", state.selectedVoice)
            putString("selected_music_id", state.selectedMusic?.id ?: "main")
            putFloat("music_volume", state.musicVolume)
            putFloat("breath_volume", state.breathVolume)
            putFloat("final_sound_volume", state.finalSoundVolume)
            putInt("streak_count", state.streakCount)
            putBoolean("vibration_enabled", state.vibrationEnabled)
            state.reminderTime?.let { putString("reminder_time", it) } ?: remove("reminder_time")
            state.finalSound?.let { sound ->
                putString("final_sound_id", sound.id)
                putString("final_sound_name", sound.name)
                sound.uri?.let { putString("final_sound_uri", it.toString()) }
                    ?: remove("final_sound_uri")
            }
        }
        saveCustomMusic()
        savePresetsToPrefs(state.userPresets)
        saveHistoryToPrefs(state.trainingHistory)
        saveDayRemindersToPrefs(state.dayReminders)
    }

    private fun reloadUiFromFlatPreferences() {
        loadCustomMusic()
        loadUserPresets()
        loadHistory()
        loadFinalSoundConfig()
        val selectedMusicId = prefs.getString("selected_music_id", "main")
        val selectedMusic = MusicSelectionPolicy.resolve(
            defaultMusic = defaultMusic,
            customMusic = _uiState.value.customMusicList,
            selectedId = selectedMusicId
        )
        val musicVolume = prefs.getFloat("music_volume", 0.75f).coerceIn(0f, 1f)
        val breathVolume = prefs.getFloat("breath_volume", 0.3f).coerceIn(0f, 1f)
        val finalSoundVolume = prefs.getFloat("final_sound_volume", 0.3f).coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                selectedVoice = prefs.getString("selected_voice", "Male")
                    ?.takeIf { voice -> voice == "Male" || voice == "Female" } ?: "Male",
                selectedMusic = selectedMusic,
                musicVolume = musicVolume,
                breathVolume = breathVolume,
                finalSoundVolume = finalSoundVolume,
                reminderTime = prefs.getString("reminder_time", null),
                dayReminders = parseDayReminders(prefs.getString("day_reminders", "{}") ?: "{}"),
                streakCount = prefs.getInt("streak_count", 0).coerceAtLeast(0),
                vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
            )
        }
        if (musicPlayerDelegate.isInitialized()) musicPlayer.volume = musicVolume
        if (breathPlayerDelegate.isInitialized()) breathPlayer.volume = breathVolume
        if (finalPlayerDelegate.isInitialized()) finalPlayer.volume = finalSoundVolume
    }

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        userEmail = auth.currentUser?.let { user ->
                            user.email ?: user.displayName ?: "Аккаунт Google"
                        },
                        authProviderLabel = "Google"
                    )
                }
                syncWithCloud()
            }
            .addOnFailureListener { e ->
                Log.e("Auth", "Google sign in failed", e)
            }
    }

    fun signInWithYandexToken(yandexToken: String) {
        signInWithExternalToken(ExternalAuthProvider.YANDEX, yandexToken)
    }

    fun signInWithVKToken(accessToken: String) {
        signInWithExternalToken(ExternalAuthProvider.VK, accessToken)
    }

    internal fun beginVkAuthorization(): VkAuthorizationRequest = vkPkceSession.begin()

    fun cancelVkAuthorization() {
        vkPkceSession.clear()
    }

    fun signInWithVkAuthorizationCode(authorizationCode: String, deviceId: String) {
        val proof = vkPkceSession.consume()
        if (proof == null) {
            showAuthError("Сессия входа через VK истекла. Попробуйте ещё раз")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val exchangeResult = withContext(Dispatchers.IO) {
                authTokenExchangeClient.exchangeVkAuthorizationCode(
                    authorizationCode = authorizationCode,
                    deviceId = deviceId,
                    proof = proof
                )
            }
            completeExternalSignIn(ExternalAuthProvider.VK, exchangeResult)
        }
    }

    private fun signInWithExternalToken(provider: ExternalAuthProvider, accessToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val exchangeResult = withContext(Dispatchers.IO) {
                authTokenExchangeClient.exchange(provider, accessToken)
            }
            completeExternalSignIn(provider, exchangeResult)
        }
    }

    private fun completeExternalSignIn(
        provider: ExternalAuthProvider,
        exchangeResult: Result<com.glazev.breathingtrainer.auth.ExchangedAuthToken>
    ) {
        exchangeResult.fold(
            onSuccess = { exchanged ->
                auth.signInWithCustomToken(exchanged.firebaseCustomToken)
                    .addOnSuccessListener { result ->
                        val label = result.user?.email
                            ?: result.user?.displayName
                            ?: exchanged.displayName
                            ?: provider.fallbackLabel
                        val providerLabel = when (provider) {
                            ExternalAuthProvider.YANDEX -> "Яндекс"
                            ExternalAuthProvider.VK -> "VK"
                        }
                        _uiState.update {
                            it.copy(
                                userEmail = label,
                                authProviderLabel = providerLabel,
                                isSyncing = false
                            )
                        }
                        exchanged.displayName?.let { displayName ->
                            result.user?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build()
                            )?.addOnFailureListener { error ->
                                Log.e("Auth", "Failed to persist display name", error)
                            }
                        }
                        syncWithCloud()
                    }
                    .addOnFailureListener {
                        _uiState.update { it.copy(isSyncing = false) }
                        showAuthError("Не удалось войти через ${provider.fallbackLabel}")
                    }
            },
            onFailure = { error ->
                    _uiState.update { it.copy(isSyncing = false) }
                    showAuthError(error.message ?: "Ошибка сервера авторизации")
            }
        )
    }

    private fun showAuthError(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }

    private fun refreshAuthIdentity() {
        val user = auth.currentUser ?: return
        if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            _uiState.update {
                it.copy(
                    userEmail = user.email ?: user.displayName ?: "Аккаунт Google",
                    authProviderLabel = "Google"
                )
            }
            return
        }

        val uid = user.uid
        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->
                if (auth.currentUser?.uid != uid) return@addOnSuccessListener
                val providerLabel = when (tokenResult.claims["externalProvider"] as? String) {
                    "yandex" -> "Яндекс"
                    "vk" -> "VK"
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        userEmail = user.email ?: user.displayName ?: it.userEmail ?: "Аккаунт",
                        authProviderLabel = providerLabel ?: it.authProviderLabel
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.e("Auth", "Failed to resolve authentication provider", error)
            }
    }

    fun signOut() {
        saveCurrentLocalProfile()
        cloudSyncGeneration++
        cloudWriteJob?.cancel()
        cloudReadyUid = null
        auth.signOut()
        viewModelScope.launch {
            runCatching {
                CredentialManager.create(getApplication<Application>())
                    .clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { error ->
                Log.w("Auth", "Failed to clear Credential Manager state", error)
            }
        }
        switchLocalProfile(GUEST_PROFILE_ID)
        _uiState.update {
            it.copy(userEmail = null, authProviderLabel = null, isSyncing = false)
        }
    }

    fun syncWithCloud() {
        val user = auth.currentUser ?: return
        switchLocalProfile(user.uid)
        val generation = ++cloudSyncGeneration
        cloudWriteJob?.cancel()
        cloudReadyUid = null
        cloudChangedWhileLoading = false
        _uiState.update { it.copy(isSyncing = true) }
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (!isCurrentCloudOperation(user.uid, generation)) return@addOnSuccessListener
                if (doc.exists()) applyCloudData(doc.data.orEmpty(), cloudChangedWhileLoading)
                cloudReadyUid = user.uid
                _uiState.update { it.copy(isSyncing = false) }
                // Also upgrades old documents and stores merged append-only history.
                pushDataToCloud()
            }
            .addOnFailureListener { error ->
                if (!isCurrentCloudOperation(user.uid, generation)) return@addOnFailureListener
                Log.e("CloudSync", "Profile download failed", error)
                cloudReadyUid = user.uid
                _uiState.update { it.copy(isSyncing = false) }
                if (cloudChangedWhileLoading) pushDataToCloud()
            }
    }

    private fun pushDataToCloud() {
        val user = auth.currentUser ?: return
        saveCurrentLocalProfile()
        if (cloudReadyUid != user.uid) {
            cloudChangedWhileLoading = true
            return
        }

        val generation = cloudSyncGeneration
        cloudWriteJob?.cancel()
        cloudWriteJob = viewModelScope.launch {
            delay(CLOUD_WRITE_DEBOUNCE_MS)
            if (!isCurrentCloudOperation(user.uid, generation)) return@launch
            db.collection("users").document(user.uid)
                .set(buildCloudData(), SetOptions.merge())
                .addOnFailureListener { error -> Log.e("CloudSync", "Profile upload failed", error) }
        }
    }

    private fun isCurrentCloudOperation(uid: String, generation: Int): Boolean =
        auth.currentUser?.uid == uid && cloudSyncGeneration == generation

    private fun buildCloudData(): Map<String, Any> {
        val state = _uiState.value
        val selectedBuiltInMusic = state.selectedMusic
            ?.takeIf { selected -> defaultMusic.any { it.id == selected.id } }
            ?.id ?: "none"
        val selectedBuiltInFinalSound = state.finalSound
            ?.takeIf { selected -> finalSounds.any { it.id == selected.id } }
            ?.id ?: "final1"

        return hashMapOf(
            "schemaVersion" to CLOUD_SCHEMA_VERSION,
            "updatedAt" to FieldValue.serverTimestamp(),
            "clientRevision" to System.currentTimeMillis(),
            "selectedVoice" to state.selectedVoice,
            "selectedMusicId" to selectedBuiltInMusic,
            "musicVolume" to state.musicVolume.toDouble(),
            "breathVolume" to state.breathVolume.toDouble(),
            "finalSoundVolume" to state.finalSoundVolume.toDouble(),
            "finalSoundId" to selectedBuiltInFinalSound,
            "vibrationEnabled" to state.vibrationEnabled,
            "streakCount" to state.streakCount.coerceAtLeast(0),
            "lastTrainingDate" to prefs.getLong("last_training_date", 0L).coerceAtLeast(0L),
            "reminderTime" to (state.reminderTime ?: ""),
            "dayReminders" to state.dayReminders.entries.take(MAX_DAY_REMINDERS)
                .associate { it.key to it.value },
            "userPresets" to state.userPresets.takeLast(MAX_PRESETS).map { technique ->
                mapOf(
                    "id" to technique.id.take(MAX_TEXT_LENGTH),
                    "name" to technique.name.take(MAX_TEXT_LENGTH),
                    "description" to technique.description.take(MAX_DESCRIPTION_LENGTH),
                    "cycles" to technique.cycles.coerceIn(1, 100),
                    "isCustom" to technique.isCustom,
                    "phases" to technique.phases.take(MAX_PHASES).map { phase ->
                        mapOf(
                            "type" to phase.type.name,
                            "duration" to phase.durationSeconds.coerceIn(0.1f, 600f).toDouble()
                        )
                    }
                )
            },
            "trainingHistory" to state.trainingHistory.takeLast(MAX_HISTORY).map { record ->
                mapOf(
                    "date" to record.date.coerceAtLeast(0L),
                    "name" to record.techniqueName.take(MAX_TEXT_LENGTH),
                    "circles" to record.circles.coerceIn(0, 10_000),
                    "duration" to record.durationTotalSeconds.coerceIn(0, 86_400),
                    "retentions" to record.retentions.take(MAX_RETENTIONS)
                        .map { it.coerceAtLeast(0) }
                )
            }
        )
    }

    private fun applyCloudData(data: Map<String, Any>, preferLocalSettings: Boolean) {
        val localState = _uiState.value
        val remotePresets = parseCloudPresets(data["userPresets"])
        val remoteHistory = parseCloudHistory(data["trainingHistory"])
        val mergedHistory = (localState.trainingHistory + remoteHistory)
            .distinctBy {
                "${it.date}|${it.techniqueName}|${it.circles}|${it.durationTotalSeconds}|${it.retentions}"
            }
            .sortedBy { it.date }
            .takeLast(MAX_HISTORY)
        val chosenPresets = if (preferLocalSettings || !data.containsKey("userPresets")) {
            localState.userPresets
        } else {
            remotePresets
        }

        val remoteMusic = (data["selectedMusicId"] as? String)
            ?.let { id -> defaultMusic.find { it.id == id } }
        val remoteFinalSound = (data["finalSoundId"] as? String)
            ?.let { id -> finalSounds.find { it.id == id } }
        val remoteReminders = parseCloudReminders(data["dayReminders"])
        val useRemote = !preferLocalSettings
        if (useRemote) cancelCurrentReminders()

        val nowMillis = System.currentTimeMillis()
        val mergedStreak = StreakCalculator.calculate(
            trainingTimestamps = mergedHistory.map { it.date },
            nowMillis = nowMillis
        )
        val mergedLastTrainingDate = mergedHistory.maxOfOrNull { it.date }
            ?.coerceAtMost(nowMillis) ?: 0L

        _uiState.update { current ->
            current.copy(
                selectedVoice = if (useRemote) {
                    (data["selectedVoice"] as? String)
                        ?.takeIf { it == "Male" || it == "Female" } ?: current.selectedVoice
                } else current.selectedVoice,
                selectedMusic = if (useRemote) remoteMusic ?: current.selectedMusic else current.selectedMusic,
                musicVolume = if (useRemote) cloudFloat(data["musicVolume"], current.musicVolume)
                    else current.musicVolume,
                breathVolume = if (useRemote) cloudFloat(data["breathVolume"], current.breathVolume)
                    else current.breathVolume,
                finalSoundVolume = if (useRemote) cloudFloat(data["finalSoundVolume"], current.finalSoundVolume)
                    else current.finalSoundVolume,
                finalSound = if (useRemote) remoteFinalSound ?: current.finalSound else current.finalSound,
                vibrationEnabled = if (useRemote) data["vibrationEnabled"] as? Boolean
                    ?: current.vibrationEnabled else current.vibrationEnabled,
                reminderTime = if (useRemote && data.containsKey("reminderTime")) {
                    (data["reminderTime"] as? String)?.takeIf(::isValidTime)
                } else current.reminderTime,
                dayReminders = if (useRemote && data.containsKey("dayReminders")) remoteReminders
                    else current.dayReminders,
                streakCount = mergedStreak,
                userPresets = chosenPresets,
                trainingHistory = mergedHistory
            )
        }
        prefs.edit { putLong("last_training_date", mergedLastTrainingDate) }
        persistCurrentStateToFlatPreferences()
        saveCurrentLocalProfile()
        val updated = _uiState.value
        if (musicPlayerDelegate.isInitialized()) musicPlayer.volume = updated.musicVolume
        if (breathPlayerDelegate.isInitialized()) breathPlayer.volume = updated.breathVolume
        if (finalPlayerDelegate.isInitialized()) finalPlayer.volume = updated.finalSoundVolume
        if (useRemote) ReminderScheduler.restoreAll(getApplication())
    }

    private fun parseCloudPresets(value: Any?): List<BreathingTechnique> =
        (value as? List<*>)?.take(MAX_PRESETS)?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val id = (item["id"] as? String)?.take(MAX_TEXT_LENGTH)
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = (item["name"] as? String)?.take(MAX_TEXT_LENGTH)
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val phases = (item["phases"] as? List<*>)?.take(MAX_PHASES)?.mapNotNull { rawPhase ->
                val phase = rawPhase as? Map<*, *> ?: return@mapNotNull null
                val type = (phase["type"] as? String)?.let { runCatching { PhaseType.valueOf(it) }.getOrNull() }
                    ?: return@mapNotNull null
                val duration = (phase["duration"] as? Number)?.toFloat()
                    ?.takeIf { it in 0.1f..600f } ?: return@mapNotNull null
                BreathingPhase(type, duration)
            }.orEmpty()
            if (phases.isEmpty()) return@mapNotNull null
            BreathingTechnique(
                id = id,
                name = name,
                description = (item["description"] as? String)?.take(MAX_DESCRIPTION_LENGTH).orEmpty(),
                phases = phases,
                cycles = (item["cycles"] as? Number)?.toInt()?.coerceIn(1, 100) ?: 1,
                isCustom = item["isCustom"] as? Boolean ?: true
            )
        }.orEmpty()

    private fun parseCloudHistory(value: Any?): List<TrainingRecord> =
        (value as? List<*>)?.takeLast(MAX_HISTORY)?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            val date = (item["date"] as? Number)?.toLong()?.takeIf { it >= 0L }
                ?: return@mapNotNull null
            val name = (item["name"] as? String)?.take(MAX_TEXT_LENGTH)
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            TrainingRecord(
                date = date,
                techniqueName = name,
                circles = (item["circles"] as? Number)?.toInt()?.coerceIn(0, 10_000) ?: 0,
                durationTotalSeconds = (item["duration"] as? Number)?.toInt()
                    ?.coerceIn(0, 86_400) ?: 0,
                retentions = (item["retentions"] as? List<*>)?.take(MAX_RETENTIONS)
                    ?.mapNotNull { value ->
                        (value as? Number)?.toLong()
                            ?.coerceIn(0L, Int.MAX_VALUE.toLong())
                            ?.toInt()
                    }.orEmpty()
            )
        }.orEmpty()

    private fun parseCloudReminders(value: Any?): Map<String, String> =
        (value as? Map<*, *>)?.entries?.asSequence()?.mapNotNull { (rawDate, rawTime) ->
            val date = rawDate as? String ?: return@mapNotNull null
            val time = rawTime as? String ?: return@mapNotNull null
            if (DATE_PATTERN.matches(date) && isValidTime(time)) date to time else null
        }?.take(MAX_DAY_REMINDERS)?.toMap().orEmpty()

    private fun cloudFloat(value: Any?, fallback: Float): Float =
        (value as? Number)?.toFloat()?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: fallback

    private fun isValidTime(value: String): Boolean {
        val parts = value.split(':')
        return parts.size == 2 &&
            parts[0].toIntOrNull() in 0..23 && parts[1].toIntOrNull() in 0..59
    }

    override fun onCleared() {
        super.onCleared()
        cloudWriteJob?.cancel()
        timerJob?.cancel()
        countdownJob?.cancel()
        sessionClock.finishSeconds()
        cancelPendingTrainingStart()
        vkPkceSession.clear()
        audioInterruptionPolicy.clear()
        audioPlaybackAllowed = false
        completionSoundOwnsFocus = false
        audioFocusController.abandon()
        if (breathPlayerDelegate.isInitialized()) breathPlayer.release()
        if (musicPlayerDelegate.isInitialized()) musicPlayer.release()
        if (finalPlayerDelegate.isInitialized()) finalPlayer.release()
    }

    private data class PendingTrainingStart(
        val activity: WeakReference<Activity>,
        val onContinue: () -> Unit
    )

    companion object {
        private const val AUDIO_TAG = "TrainingAudio"
        private const val AD_START_TIMEOUT_MS = 5_000L
        private const val GUEST_PROFILE_ID = "guest"
        private const val PROFILE_STORAGE_INITIALIZED = "profile_storage_initialized_v2"
        private const val FIRST_ACCOUNT_CLAIMED = "first_account_claimed_v2"
        private const val PROFILE_EXISTS = "profile_exists"
        private const val CLOUD_SCHEMA_VERSION = 2
        private const val CLOUD_WRITE_DEBOUNCE_MS = 500L
        private const val MAX_PRESETS = 50
        private const val MAX_PHASES = 20
        private const val MAX_HISTORY = 100
        private const val MAX_RETENTIONS = 50
        private const val MAX_DAY_REMINDERS = 64
        private const val MAX_TEXT_LENGTH = 120
        private const val MAX_DESCRIPTION_LENGTH = 500
        private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val PROFILE_KEYS = setOf(
            "music_volume",
            "breath_volume",
            "final_sound_volume",
            "selected_voice",
            "selected_music_id",
            "reminder_time",
            "day_reminders",
            "streak_count",
            "last_training_date",
            "vibration_enabled",
            "final_sound_id",
            "final_sound_name",
            "final_sound_uri",
            "custom_music_list",
            "user_presets",
            "training_history"
        )
    }
}
