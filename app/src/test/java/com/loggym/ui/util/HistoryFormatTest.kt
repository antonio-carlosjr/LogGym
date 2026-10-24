package com.loggym.ui.util

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class HistoryFormatTest {
    @Test fun dateUsesSpecifiedLocalZoneAcrossMidnight() {
        assertEquals("23/10/2026 23:30", Instant.parse("2026-10-24T02:30:00Z").formatSessionDate(ZoneId.of("America/Sao_Paulo")))
    }
    @Test fun durationPreservesZeroAndTreatsMissingOrInvalidEndAsUnavailable() {
        assertEquals(0L, sessionDurationSeconds(Instant.EPOCH, Instant.EPOCH))
        assertEquals(125L, sessionDurationSeconds(Instant.EPOCH, Instant.EPOCH.plusSeconds(125)))
        assertNull(sessionDurationSeconds(Instant.EPOCH, null))
        assertNull(sessionDurationSeconds(Instant.EPOCH, Instant.EPOCH.minusSeconds(1)))
    }
}
