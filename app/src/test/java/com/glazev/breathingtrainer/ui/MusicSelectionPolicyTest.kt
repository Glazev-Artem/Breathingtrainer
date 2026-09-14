package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicSelectionPolicyTest {
    private val defaults = listOf(
        BackgroundMusic("none", "Без музыки"),
        BackgroundMusic("main", "Стандартная")
    )

    @Test
    fun `restores selected custom track after custom list is loaded`() {
        val custom = BackgroundMusic("custom-1", "Мой трек")

        assertEquals(
            custom,
            MusicSelectionPolicy.resolve(defaults, listOf(custom), selectedId = custom.id)
        )
    }

    @Test
    fun `missing selection safely falls back to main track`() {
        assertEquals(
            defaults[1],
            MusicSelectionPolicy.resolve(defaults, emptyList(), selectedId = "deleted-track")
        )
    }
}
