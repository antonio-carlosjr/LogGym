package com.loggym

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Substitui o Dispatchers.Main durante o teste, para o viewModelScope funcionar na JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Espera o primeiro valor que satisfaz [predicate] em tempo real.
 * O Room responde em threads próprias, então o tempo virtual do runTest não serve.
 */
suspend fun <T> Flow<T>.awaitFirst(timeoutMillis: Long = 5_000, predicate: (T) -> Boolean): T =
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) { first(predicate) }
    }
