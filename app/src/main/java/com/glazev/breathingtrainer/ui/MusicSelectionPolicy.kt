package com.glazev.breathingtrainer.ui

internal object MusicSelectionPolicy {
    fun resolve(
        defaultMusic: List<BackgroundMusic>,
        customMusic: List<BackgroundMusic>,
        selectedId: String?,
        fallbackId: String = "main"
    ): BackgroundMusic? =
        (defaultMusic + customMusic).firstOrNull { it.id == selectedId }
            ?: defaultMusic.firstOrNull { it.id == fallbackId }
            ?: defaultMusic.firstOrNull()
}
