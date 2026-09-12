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
    private fun createPhases(inhale: Float, hold1: Float, exhale: Float, hold2: Float): List<BreathingPhase> {
        val phases = mutableListOf<BreathingPhase>()
        if (inhale > 0) phases.add(BreathingPhase(PhaseType.INHALE, inhale))
        if (hold1 > 0) phases.add(BreathingPhase(PhaseType.HOLD_IN, hold1))
        if (exhale > 0) phases.add(BreathingPhase(PhaseType.EXHALE, exhale))
        if (hold2 > 0) phases.add(BreathingPhase(PhaseType.HOLD_OUT, hold2))
        return phases
    }

    val SquareBreathing = BreathingTechnique("square", "Квадратное дыхание", "4-4-4-4", createPhases(4f, 4f, 4f, 4f))
    val Relax478 = BreathingTechnique("478", "Техника 4-7-8", "4-7-8-0", createPhases(4f, 7f, 8f, 0f))

    val list = listOf(
        BreathingTechnique("equal", "Равное дыхание", "4-0-4-0", createPhases(4f, 0f, 4f, 0f)),
        SquareBreathing,
        Relax478,
        BreathingTechnique("relax1", "Расслабление", "3-0-5-0", createPhases(3f, 0f, 5f, 0f)),
        BreathingTechnique("calm", "Спокойствие", "4-0-6-0", createPhases(4f, 0f, 6f, 0f)),
        BreathingTechnique("relax2", "Расслабление (вариант 2)", "4-2-4-0", createPhases(4f, 2f, 4f, 0f)),
        BreathingTechnique("rest", "Отдых", "4-0-4-4", createPhases(4f, 0f, 4f, 4f)),
        BreathingTechnique("clear_mind", "Ясный ум", "4-0-8-0", createPhases(4f, 0f, 8f, 0f)),
        BreathingTechnique("addiction", "Победите зависимость", "5-0-10-5", createPhases(5f, 0f, 10f, 5f)),
        BreathingTechnique("focus", "Фокус", "4-4-8-0", createPhases(4f, 4f, 8f, 0f)),
        BreathingTechnique("anxiety", "Снятие тревоги", "4-2-6-0", createPhases(4f, 2f, 6f, 0f)),
        BreathingTechnique("pain", "Снятие боли", "4-3-7-0", createPhases(4f, 3f, 7f, 0f)),
        BreathingTechnique("sleep1", "Сон", "5-7-7-1", createPhases(5f, 7f, 7f, 1f)),
        BreathingTechnique("sleep2", "Полноценный сон", "5-0-14-1", createPhases(5f, 0f, 14f, 1f)),
        BreathingTechnique("deep_rest", "Глубокий отдых", "5-1-15-1", createPhases(5f, 1f, 15f, 1f)),
        BreathingTechnique("activation", "Активация", "6-0-4-0", createPhases(6f, 0f, 4f, 0f)),
        BreathingTechnique("energy", "Энергия", "6-6-6-1", createPhases(6f, 6f, 6f, 1f)),
        BreathingTechnique("lungs1", "Тренировка легких", "5-8-5-8", createPhases(5f, 8f, 5f, 8f)),
        BreathingTechnique("lungs2", "Легкие (тяжелый уровень)", "6-16-8-8", createPhases(6f, 16f, 8f, 8f)),
        BreathingTechnique("endurance", "Выносливость", "6-15-10-0", createPhases(6f, 15f, 10f, 0f)),
        BreathingTechnique("asthma", "Медленное для астмы", "2-0-3-2", createPhases(2f, 0f, 3f, 2f))
    )
}
