package com.loggym.ui.plans

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.MainDispatcherRule
import com.loggym.awaitFirst
import com.loggym.data.local.LogGymDatabase
import com.loggym.data.local.fixedClock
import com.loggym.data.local.inMemoryDatabase
import com.loggym.data.repository.PlanRepositoryImpl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlansViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: LogGymDatabase
    private lateinit var repository: PlanRepositoryImpl
    private lateinit var viewModel: PlansViewModel

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        repository = PlanRepositoryImpl(database, database.planDao(), database.exerciseDao(), fixedClock)
        viewModel = PlansViewModel(repository)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `nome em branco mostra erro e nao cria plano - RF-24`() = runTest {
        viewModel.openCreatePlan()
        viewModel.onPlanNameChange("   ")
        viewModel.createPlan(onCreated = { error("Não deveria criar") })

        assertTrue((viewModel.dialog.value as PlansDialog.CreatePlan).showBlankError)
    }

    @Test
    fun `criar plano personalizado abre o editor do novo plano - RF-23`() = runTest {
        val created = CompletableDeferred<Long>()

        viewModel.openCreatePlan()
        viewModel.onPlanNameChange("Meu ABC")
        viewModel.createPlan(onCreated = { created.complete(it) })

        val plans = viewModel.uiState.awaitFirst { it.plans.isNotEmpty() }.plans
        assertEquals(listOf("Meu ABC"), plans.map { it.name })
        assertEquals(plans.single().id, created.await())
    }

    @Test
    fun `template pergunta se deve ativar e ativa o plano - RF-37 e RF-146`() = runTest {
        repository.setActivePlan(repository.createPlan("Antigo"))

        viewModel.createFromTemplate("ppl")
        val ask = viewModel.dialog.awaitFirst { it is PlansDialog.AskActivate } as PlansDialog.AskActivate
        viewModel.activate(ask.planId)

        val plans = viewModel.uiState.awaitFirst { state -> state.plans.any { it.id == ask.planId && it.isActive } }
        assertEquals(listOf(ask.planId), plans.plans.filter { it.isActive }.map { it.id })
        viewModel.dialog.awaitFirst { it == null }
    }

    @Test
    fun `excluir plano exige confirmacao - RF-26 e RF-27`() = runTest {
        repository.createPlan("Temporário")
        val plan = viewModel.uiState.awaitFirst { it.plans.isNotEmpty() }.plans.single()

        viewModel.requestDelete(plan)
        assertEquals(PlansDialog.ConfirmDelete(plan), viewModel.dialog.value)
        viewModel.confirmDelete()

        viewModel.uiState.awaitFirst { it.plans.isEmpty() }
    }
}
