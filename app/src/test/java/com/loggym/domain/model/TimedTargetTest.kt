package com.loggym.domain.model

import com.loggym.domain.logic.SetPrefill
import com.loggym.domain.logic.SetValues
import com.loggym.ui.plans.TargetForm
import com.loggym.ui.plans.TargetFormResult
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration

class TimedTargetTest {
    @Test fun defaultIsExplicitAndContainsOnlyDuration() {
        val blank = TargetForm(trackingType = TrackingType.TIME)
        assertTrue(blank.validate() is TargetFormResult.Invalid)
        val target = (blank.withDefault().validate() as TargetFormResult.Valid).target
        assertEquals(ExerciseTarget.Timed(3, 30, Duration.ofSeconds(90)), target)
        assertNull(target.repMin)
        assertEquals(listOf(SetValues(durationSeconds = 45), SetValues.EMPTY),
            SetPrefill.build(2, listOf(SetValues(durationSeconds = 45))))
    }
    @Test(expected = IllegalArgumentException::class) fun durationMustBePositive() {
        ExerciseTarget.Timed(3, 0, Duration.ZERO)
    }
    @Test(expected = IllegalArgumentException::class) fun timedValuesCannotContainWeight() {
        SetValues(0.0, null, 30).validate(TrackingType.TIME)
    }
    @Test fun nullAndZeroAreDifferent() {
        assertTrue(SetValues().isIncomplete(TrackingType.WEIGHT_REPS))
        assertFalse(SetValues(0.0, 0).isIncomplete(TrackingType.WEIGHT_REPS))
        SetValues().validate(TrackingType.TIME)
    }
}
