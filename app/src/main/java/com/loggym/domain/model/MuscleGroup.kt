package com.loggym.domain.model

/**
 * Lista fixa de grupos musculares (RF-130). A [key] é gravada no banco e nunca muda;
 * o rótulo exibido vem dos recursos de texto da UI.
 */
enum class MuscleGroup(val key: String) {
    CHEST("chest"),
    BACK("back"),
    TRAPS("traps"),
    SHOULDERS("shoulders"),
    BICEPS("biceps"),
    TRICEPS("triceps"),
    FOREARMS("forearms"),
    ABS("abs"),
    QUADS("quads"),
    HAMSTRINGS("hamstrings"),
    GLUTES("glutes"),
    CALVES("calves"),
    ADDUCTORS("adductors"),
    ABDUCTORS("abductors"),
    ;

    companion object {
        fun fromKey(key: String): MuscleGroup? = entries.firstOrNull { it.key == key }
    }
}
