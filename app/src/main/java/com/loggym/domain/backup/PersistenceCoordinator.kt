package com.loggym.domain.backup

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PersistencePhase { READY, PREPARING, CAPTURING, RESTORING, RECOVERING, BLOCKED }
fun interface PreparedWrites { fun release() }
fun interface PendingWrites { suspend fun prepare(): PreparedWrites }
enum class PreparationFailure { INVALID_INPUT, STORAGE, OWNER_CHANGED }
class PreparationException(val reason: PreparationFailure) : IllegalStateException(reason.name)

/** Ordem: comandos aceitos → mutexes dos editores/flush → mutex global. Nunca o inverso. */
@Singleton
class PersistenceCoordinator @Inject constructor() {
    private val gate = Mutex()
    private val preparation = Mutex()
    private val owners = linkedSetOf<PendingWrites>()
    private val commands = linkedSetOf<Job>()
    private val writeFailures = java.util.concurrent.atomic.AtomicLong()
    private val state = MutableStateFlow(PersistencePhase.READY)
    val phase = state.asStateFlow()
    private val epoch = MutableStateFlow(0L)
    val generation = epoch.asStateFlow()
    val canEdit get() = state.value == PersistencePhase.READY
    private class Generation(val value: Long) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Generation>
    }
    private class Lease(val coordinator: PersistenceCoordinator) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Lease>
    }
    fun context() : CoroutineContext = Generation(epoch.value)
    fun register(owner: PendingWrites): AutoCloseable {
        synchronized(owners) { owners.add(owner) }
        return AutoCloseable { synchronized(owners) { owners.remove(owner) } }
    }
    fun command(scope: CoroutineScope, context: CoroutineContext, action: suspend () -> Unit): Job? {
        val job = synchronized(commands) {
            if (!canEdit) return null
            scope.launch(context, start = CoroutineStart.LAZY) { action() }.also { commands.add(it) }
        }
        job.invokeOnCompletion { synchronized(commands) { commands.remove(job) } }
        job.start()
        return job
    }
    /** Mesmo ponto de corte da admissão de comandos, sem suspender durante a edição local. */
    fun edit(action: () -> Unit) = synchronized(commands) { if (canEdit) action() }
    suspend fun <T> write(block: suspend () -> T): T {
        val context = currentCoroutineContext()
        if (context[Lease]?.coordinator === this) return block()
        val revision = context[Generation]?.value ?: epoch.value
        if (state.value !in listOf(PersistencePhase.READY, PersistencePhase.PREPARING))
            throw CancellationException("Persistência indisponível durante backup/restauração")
        return gate.withLock {
            context.ensureActive()
            if (revision != epoch.value) throw CancellationException("Geração de dados substituída")
            try { block() } catch (error: Exception) {
                if (error !is CancellationException) writeFailures.incrementAndGet()
                throw error
            }
        }
    }
    suspend fun <T> capture(block: suspend () -> T): T = prepared(false, block)
    suspend fun <T> restore(block: suspend () -> T): T = prepared(true, block)
    private suspend fun <T> prepared(replace: Boolean, block: suspend () -> T): T = preparation.withLock {
        check(state.value == PersistencePhase.READY) { "Há uma operação de persistência em andamento" }
        val leases = mutableListOf<PreparedWrites>()
        val failuresBefore = writeFailures.get()
        var exclusiveStarted = false
        val accepted = synchronized(commands) { state.value = PersistencePhase.PREPARING; commands.toList() }
        try {
            accepted.joinAll()
            val participants = synchronized(owners) { owners.toList() }
            for (owner in participants) {
                // O retorno de uma lease não pode ser perdido por cancelamento na troca de dispatcher.
                leases.add(withContext(NonCancellable) { owner.prepare() })
                currentCoroutineContext().ensureActive()
            }
            if (!synchronized(owners) { owners.toList() == participants }) throw PreparationException(PreparationFailure.OWNER_CHANGED)
            if (writeFailures.get() != failuresBefore) throw PreparationException(PreparationFailure.STORAGE)
            gate.withLock {
                exclusiveStarted = true
                state.value = if (replace) PersistencePhase.RESTORING else PersistencePhase.CAPTURING
                if (replace) epoch.value++
                withContext(Lease(this@PersistenceCoordinator)) { block() }.also {
                    if (!replace) check(synchronized(owners) { owners.toList() == participants }) { "Editor foi encerrado durante captura" }
                    exclusiveStarted = false
                }
            }
        } finally {
            leases.asReversed().forEach { it.release() }
            state.value = if (replace && exclusiveStarted) PersistencePhase.BLOCKED else PersistencePhase.READY
        }
    }
    suspend fun <T> recover(block: suspend () -> T): T = preparation.withLock {
        state.value = PersistencePhase.RECOVERING
        try {
            gate.withLock { withContext(Lease(this@PersistenceCoordinator)) { block() } }
                .also { state.value = PersistencePhase.READY }
        } catch (error: Throwable) { state.value = PersistencePhase.BLOCKED; throw error }
    }
}
