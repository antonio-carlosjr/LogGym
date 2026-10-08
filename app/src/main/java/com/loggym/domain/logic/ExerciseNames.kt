package com.loggym.domain.logic

import java.util.Locale

/** RF-132: nomes de exercícios comparados sem diferenciar maiúsculas e espaços. */
object ExerciseNames {
    private val whitespace = Regex("\\s+")
    private val ptBr = Locale.forLanguageTag("pt-BR")

    /** Forma gravada: sem espaços nas pontas nem repetidos. */
    fun clean(name: String): String = name.trim().replace(whitespace, " ")

    /** Chave de comparação usada para detectar duplicatas. */
    fun comparisonKey(name: String): String = clean(name).lowercase(ptBr)
}
