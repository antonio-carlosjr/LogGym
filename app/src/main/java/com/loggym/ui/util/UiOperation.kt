package com.loggym.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Mantém formulários abertos em falhas de escrita e impede ações duplicadas. */
class UiOperation(private val scope: CoroutineScope) {
    private val working = MutableStateFlow(false)
    val busy = working.asStateFlow()
    private val failure = MutableStateFlow(false)
    val failed = failure.asStateFlow()
    fun dismissError() { failure.value = false }

    fun launch(action: suspend () -> Unit) {
        if (working.value) return
        working.value = true
        failure.value = false
        scope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                failure.value = true
            } finally { working.value = false }
        }
    }
}
