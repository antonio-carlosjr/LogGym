package com.loggym.ui.plans

import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.InputRules
import com.loggym.ui.util.formatRest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class TargetFormTest {

    @Test
    fun `formulario novo comeca vazio e e invalido - RF-143`() {
        assertEquals(TargetFormResult.Invalid(TargetFormError.MISSING_SETS), TargetForm().validate())
    }

    @Test
    fun `padrao so e aplicado quando pedido - RF-143`() {
        val result = TargetForm().withDefault().validate()

        assertEquals(TargetFormResult.Valid(InputRules.DEFAULT_TARGET), result)
    }

    @Test
    fun `valida series, repeticoes e faixa - RF-50 a RF-53`() {
        assertEquals(invalid(TargetFormError.INVALID_SETS), TargetForm("0", "8", "12").validate())
        assertEquals(invalid(TargetFormError.MISSING_REPS), TargetForm("3", "", "12").validate())
        assertEquals(invalid(TargetFormError.INVALID_REPS), TargetForm("3", "0", "12").validate())
        assertEquals(invalid(TargetFormError.MIN_GREATER_THAN_MAX), TargetForm("3", "12", "8").validate())
    }

    @Test
    fun `aceita valores altos sem teto - RF-141`() {
        val result = TargetForm("10", "50", "100", restSeconds = 600).validate()

        assertEquals(TargetFormResult.Valid(ExerciseTarget(10, 50, 100, Duration.ofSeconds(600))), result)
    }

    @Test
    fun `descanso anda em passos de 15 s entre 0 e 600 - RF-142`() {
        assertEquals(15L, TargetForm().adjustRest(+1).restSeconds)
        assertEquals(0L, TargetForm().adjustRest(-1).restSeconds)
        assertEquals(600L, TargetForm(restSeconds = 590).adjustRest(+4).restSeconds)
    }

    @Test
    fun `campos numericos aceitam so digitos com limite de layout - P8`() {
        assertEquals("1234", TargetForm.digitsOnly("1a2-3 45"))
    }

    @Test
    fun `descanso e exibido em minutos e segundos`() {
        assertEquals("sem descanso", formatRest(0))
        assertEquals("1:30", formatRest(90))
        assertEquals("10:00", formatRest(600))
    }

    private fun invalid(error: TargetFormError) = TargetFormResult.Invalid(error)
}
