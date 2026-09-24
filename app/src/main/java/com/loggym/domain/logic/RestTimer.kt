package com.loggym.domain.logic

import java.time.Duration
import java.time.Instant

/**
 * Cronômetro de descanso baseado em timestamp (seção 10, decisão técnica).
 * O instante final é persistido; o tempo restante é sempre recalculado (RF-93, RF-94).
 */
data class RestTimer(
    val startedAt: Instant,
    val endsAt: Instant,
) {
    fun remaining(now: Instant): Duration {
        val remaining = Duration.between(now, endsAt)
        return if (remaining.isNegative) Duration.ZERO else remaining
    }

    /** RF-95: se o horário final já passou, o descanso está concluído. */
    fun isFinished(now: Instant): Boolean = !now.isBefore(endsAt)

    companion object {
        /** RF-89: descanso zero não inicia cronômetro. */
        fun start(now: Instant, rest: Duration): RestTimer? =
            if (rest.isZero || rest.isNegative) null else RestTimer(startedAt = now, endsAt = now.plus(rest))
    }
}
