package com.loggym.ui.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.*
import com.loggym.data.repository.PlanRepositoryImpl
import com.loggym.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private lateinit var db: LogGymDatabase
    @Before fun setup() { db = inMemoryDatabase() }
    @After fun close() { db.close() }

    @Test fun retryAfterPreferenceFailureDoesNotCreateAnotherTemplateCopy() = runTest {
        val plans = PlanRepositoryImpl(db, db.planDao(), db.exerciseDao(), fixedClock)
        var fail = true
        val preferences = object : com.loggym.FakeUserPreferences() {
            override suspend fun setOnboardingCompleted() {
                if (fail) throw java.io.IOException("Falha simulada")
                onboardingCompleted.value = true
            }
        }
        val model = OnboardingViewModel(plans, preferences)
        model.selectTemplate("full_body") { error("Não deve navegar após falha") }
        model.failed.awaitFirst { it }
        model.isWorking.awaitFirst { !it }
        assertEquals(1, plans.observePlans().awaitFirst { it.isNotEmpty() }.size)
        fail = false
        val done = CompletableDeferred<Unit>()
        model.selectTemplate("full_body") { done.complete(Unit) }
        done.await()
        assertTrue(preferences.onboardingCompleted.value)
        assertEquals(1, plans.observePlans().awaitFirst { it.isNotEmpty() }.size)
    }
}
