package com.loggym.domain.logic

data class SetValues(val weightKg: Double?, val reps: Int?) {
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
