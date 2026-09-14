package com.glazev.breathingtrainer.model

import androidx.annotation.RawRes

enum class PhaseType {
    INHALE, HOLD_IN, EXHALE, HOLD_OUT
}

data class BreathingPhase(
    val type: PhaseType,
    val durationSeconds: Float,
    @RawRes val soundResId: Int? = null
)

data class BreathingTechnique(
    val id: String,
    val name: String,
    val description: String,
    val phases: List<BreathingPhase>,
    val cycles: Int = 1,
    val isCustom: Boolean = false
)

object DefaultTechniques {
    private fun formatDuration(seconds: Float): String =
        if (seconds % 1f == 0f) seconds.toInt().toString() else seconds.toString()

    private fun technique(
        id: String,
        name: String,
        inhale: Float,
        holdIn: Float,
        exhale: Float,
        holdOut: Float
    ) = BreathingTechnique(
        id = id,
        name = name,
        description = listOf(inhale, holdIn, exhale, holdOut)
            .joinToString("-") { formatDuration(it) },
        phases = createPhases(inhale, holdIn, exhale, holdOut)
    )

    private fun createPhases(inhale: Float, hold1: Float, exhale: Float, hold2: Float): List<BreathingPhase> {
        val phases = mutableListOf<BreathingPhase>()
        if (inhale > 0) phases.add(BreathingPhase(PhaseType.INHALE, inhale))
        if (hold1 > 0) phases.add(BreathingPhase(PhaseType.HOLD_IN, hold1))
        if (exhale > 0) phases.add(BreathingPhase(PhaseType.EXHALE, exhale))
        if (hold2 > 0) phases.add(BreathingPhase(PhaseType.HOLD_OUT, hold2))
        return phases
    }

    val SquareBreathing = technique("square", "Квадратное дыхание", 4f, 4f, 4f, 4f)
    val Relax478 = technique("478", "Техника 4-7-8", 4f, 7f, 8f, 0f)

    val list = listOf(
        technique("equal", "Равное дыхание", 4f, 0f, 4f, 0f),
        SquareBreathing,
        Relax478,
        technique("relax1", "Расслабление", 3f, 0f, 5f, 0f),
        technique("calm", "Спокойствие", 4f, 0f, 6f, 0f),
        technique("relax2", "Расслабление (вариант 2)", 4f, 2f, 4f, 0f),
        technique("rest", "Отдых", 4f, 0f, 4f, 4f),
        technique("clear_mind", "Ясный ум", 4f, 0f, 8f, 0f),
        technique("addiction", "Победите зависимость", 5f, 0f, 10f, 5f),
        technique("focus", "Фокус", 4f, 4f, 8f, 0f),
        technique("anxiety", "Снятие тревоги", 4f, 2f, 6f, 0f),
        technique("pain", "Снятие боли", 4f, 3f, 7f, 0f),
        technique("sleep1", "Сон", 5f, 7f, 7f, 1f),
        technique("sleep2", "Полноценный сон", 5f, 0f, 14f, 1f),
        technique("deep_rest", "Глубокий отдых", 5f, 1f, 15f, 1f),
        technique("activation", "Активация", 6f, 0f, 4f, 0f),
        technique("energy", "Энергия", 6f, 6f, 6f, 1f),
        technique("lungs1", "Тренировка легких", 5f, 8f, 5f, 8f),
        technique("lungs2", "Легкие (тяжелый уровень)", 6f, 16f, 8f, 8f),
        technique("endurance", "Выносливость", 6f, 15f, 10f, 0f),
        technique("asthma", "Медленное для астмы", 2f, 0f, 3f, 2f)
    )
}
