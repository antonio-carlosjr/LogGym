package com.loggym.domain.backup

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceCoordinatorTest {
    @Test fun failedAcceptedWritePreventsCaptureEvenWhenEditorHandlesTheError() = runTest {
        val c = PersistenceCoordinator(); val gate = CompletableDeferred<Unit>()
        c.command(this, c.context()) { runCatching { c.write { gate.await(); throw java.io.IOException() } } }
        val snapshot = async { runCatching { c.capture { error("não capturar") } } }
        runCurrent(); gate.complete(Unit)
        assertEquals(PreparationFailure.STORAGE, (snapshot.await().exceptionOrNull() as PreparationException).reason)
        c.capture { assertFalse(c.canEdit) }
    }
    @Test fun acceptedCommandFinishesBeforeFlushAndGlobalCapture() = runTest {
        val coordinator = PersistenceCoordinator(); val gate = CompletableDeferred<Unit>(); val events = mutableListOf<String>()
        coordinator.command(this, coordinator.context()) { coordinator.write { gate.await(); events.add("write") } }
        coordinator.register(PendingWrites { coordinator.write { events.add("flush") }; PreparedWrites { events.add("release") } })
        val export = launch { coordinator.capture { events.add("snapshot") } }
        runCurrent(); assertTrue(events.isEmpty()); assertNull(coordinator.command(this, coordinator.context()) { error("não aceito") })
        gate.complete(Unit); export.join(); assertEquals(listOf("write", "flush", "snapshot", "release"), events)
    }
    @Test fun cancellationReleasesEditorsAndAllowsNewCapture() = runTest {
        val c = PersistenceCoordinator(); var released = false; val gate = CompletableDeferred<Unit>()
        c.register(PendingWrites { gate.await(); PreparedWrites { released = true } })
        val exporting = launch { c.capture { error("cancelado") } }; runCurrent(); exporting.cancel(); gate.complete(Unit); exporting.join()
        assertTrue(released); assertEquals(PersistencePhase.READY, c.phase.value)
        c.capture { assertFalse(c.canEdit) }
    }
    @Test fun disappearingParticipantFailsSafely() = runTest {
        val c = PersistenceCoordinator(); lateinit var registration: AutoCloseable; var released = false
        registration = c.register(PendingWrites { registration.close(); PreparedWrites { released = true } })
        assertTrue(runCatching { c.capture { error("não capturar") } }.exceptionOrNull() is PreparationException)
        assertTrue(released); assertEquals(PersistencePhase.READY, c.phase.value)
    }
    @Test fun staleGenerationCannotWriteAfterRestoreAndFailureBlocksNormalWrites() = runTest {
        val c = PersistenceCoordinator(); val stale = c.context(); c.restore { c.write {} }
        assertTrue(runCatching { withContext(stale) { c.write { error("obsoleto") } } }.exceptionOrNull() is CancellationException)
        assertTrue(runCatching { c.restore { throw IllegalStateException() } }.isFailure)
        assertEquals(PersistencePhase.BLOCKED, c.phase.value); c.recover {}; assertEquals(PersistencePhase.READY, c.phase.value)
    }
}
