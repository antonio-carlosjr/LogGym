package com.loggym.domain.logic

import com.loggym.domain.model.InputRules
import java.math.BigDecimal
import java.math.RoundingMode

/** RF-140: digitação com vírgula ou ponto e exibição pt-BR sem zeros à direita. */
object WeightFormat {
    private val pattern = Regex("""^\d+([.,]\d{1,${InputRules.WEIGHT_MAX_DECIMALS}})?$""")

    /** Retorna null para texto vazio ou inválido (negativo, letras, mais de 2 casas). */
    fun parse(input: String): Double? {
        val text = input.trim()
        if (!pattern.matches(text)) return null
        return text.replace(',', '.').toDouble()
    }

    fun format(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(InputRules.WEIGHT_MAX_DECIMALS, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
}
