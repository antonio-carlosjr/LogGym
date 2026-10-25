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
        assertEquals(invalid(TargetFormError.MISSING_REPS), TargetForm("3", "", "").validate())
        assertEquals(invalid(TargetFormError.INVALID_REPS), TargetForm("3", "-1", "12").validate())
        assertEquals(invalid(TargetFormError.MIN_GREATER_THAN_MAX), TargetForm("3", "12", "8").validate())
    }

    @Test
    fun `aceita valores altos sem teto - RF-141`() {
        val result = TargetForm("10", "50", "100", restSeconds = "600").validate()

        assertEquals(TargetFormResult.Valid(ExerciseTarget.Repetitions(10, 50, 100, Duration.ofSeconds(600))), result)
    }

    @Test
    fun `descanso anda em passos de 15 s entre 0 e 600 - RF-142`() {
        assertEquals("15", TargetForm().adjustRest(+1).restSeconds)
        assertEquals("0", TargetForm().adjustRest(-1).restSeconds)
        assertEquals("600", TargetForm(restSeconds = "590").adjustRest(+1).restSeconds)
        assertEquals("52", TargetForm(restSeconds = "37").adjustRest(+1).restSeconds)
        assertEquals("22", TargetForm(restSeconds = "37").adjustRest(-1).restSeconds)
    }

    @Test fun `descanso invalido nao e transformado nem salvo`() {
        for (text in listOf("-1", "601", "3.7", "3,7", "abc", "999999999999999999999999")) {
            val form = TargetForm("3", "8", "12", restSeconds = text)
            assertEquals(invalid(TargetFormError.INVALID_REST), form.validate())
            assertEquals(form, form.adjustRest(1))
        }
        assertEquals(0L, (TargetForm("3", "8", "12").validate() as TargetFormResult.Valid).target.rest.seconds)
    }

    @Test
    fun `campos numericos aceitam so digitos com limite de layout - P8`() {
        assertEquals("1234", TargetForm.digitsOnly("1a2-3 45"))
    }

    @Test
    fun `descanso e exibido em minutos e segundos`() {
        assertEquals("0:00", formatRest(0))
        assertEquals("1:30", formatRest(90))
        assertEquals("10:00", formatRest(600))
    }

    private fun invalid(error: TargetFormError) = TargetFormResult.Invalid(error)

    @Test fun `minimo vazio normaliza para zero e maximo e obrigatorio`() {
        assertEquals(0, (TargetForm("3", "", "12").validate() as TargetFormResult.Valid).target.repMin)
        assertEquals(invalid(TargetFormError.INVALID_REPS), TargetForm("3", "0", "0").validate())
    }

    @Test fun `fixo valida valor positivo e reabre sem migracao`() {
        val form = TargetForm(sets = "3", repetitionMode = RepetitionMode.FIXED, fixedReps = "10")
        val target = (form.validate() as TargetFormResult.Valid).target
        assertEquals(10, target.repMin)
        assertEquals(10, target.repMax)
        assertEquals(RepetitionMode.FIXED, TargetForm.from(target).repetitionMode)
        for (value in listOf("", "0", "-1", "1.5")) {
            org.junit.Assert.assertTrue(form.copy(fixedReps = value).validate() is TargetFormResult.Invalid)
        }
        val range = form.copy(repetitionMode = RepetitionMode.RANGE, repMin = "8", repMax = "12")
        assertEquals("10", range.copy(repetitionMode = RepetitionMode.FIXED).fixedReps)
    }
}
