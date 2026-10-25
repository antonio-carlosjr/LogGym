package com.loggym.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mantém formulários abertos em falhas de escrita e impede ações duplicadas. */
class UiOperation(private val scope: CoroutineScope, private val persistence: com.loggym.domain.backup.PersistenceCoordinator = com.loggym.domain.backup.PersistenceCoordinator()) {
    private val generationContext = persistence.context()
    private val working = MutableStateFlow(false)
    val busy = working.asStateFlow()
    private val failure = MutableStateFlow(false)
    val failed = failure.asStateFlow()
    fun dismissError() { failure.value = false }

    fun launch(action: suspend () -> Unit) {
        if (!persistence.canEdit || working.value) return
        working.value = true
        failure.value = false
        persistence.command(scope, generationContext) {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                failure.value = true
            } finally { working.value = false }
        } ?: run { working.value = false }
    }
}
