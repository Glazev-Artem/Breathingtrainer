package com.glazev.breathingtrainer.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

internal class AudioFocusController(
    context: Context,
    private val onFocusChange: (Int) -> Unit
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) ownsFocus = false
        onFocusChange(focusChange)
    }
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setOnAudioFocusChangeListener(focusListener, mainHandler)
        .build()

    private var ownsFocus = false

    fun request(): Boolean {
        if (ownsFocus) return true
        ownsFocus = runCatching {
            audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }.onFailure { error ->
            Log.e(TAG, "Unable to request audio focus", error)
        }.getOrDefault(false)
        return ownsFocus
    }

    fun abandon() {
        if (!ownsFocus) return
        runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
            .onFailure { error -> Log.e(TAG, "Unable to abandon audio focus", error) }
        ownsFocus = false
    }

    companion object {
        private const val TAG = "AudioFocus"
    }
}
