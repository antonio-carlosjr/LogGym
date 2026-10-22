package com.loggym.domain.logic

data class SetValues(val weightKg: Double? = null, val reps: Int? = null, val durationSeconds: Int? = null) {
    fun validate(type: com.loggym.domain.model.TrackingType) {
        require(weightKg == null || (weightKg.isFinite() && weightKg >= 0))
        require(reps == null || reps >= 0)
        require(durationSeconds == null || durationSeconds > 0)
        if (type == com.loggym.domain.model.TrackingType.TIME) require(weightKg == null && reps == null)
        else require(durationSeconds == null)
    }
    fun isIncomplete(type: com.loggym.domain.model.TrackingType): Boolean =
        if (type == com.loggym.domain.model.TrackingType.TIME) durationSeconds == null
        else weightKg == null || reps == null

    companion object {
        val EMPTY = SetValues(weightKg = null, reps = null)
    }
}

/** Regras de pré-preenchimento das séries (seção 8). */
object SetPrefill {

    /**
     * Associa as séries da última sessão concluída às séries atuais pela posição ordinal (RF-69).
     * Faltantes começam vazias (RF-70, RF-72) e excedentes são ignoradas (RF-71).
     */
    fun build(targetSets: Int, previous: List<SetValues>): List<SetValues> =
        List(targetSets) { index -> previous.getOrNull(index) ?: SetValues.EMPTY }
}
