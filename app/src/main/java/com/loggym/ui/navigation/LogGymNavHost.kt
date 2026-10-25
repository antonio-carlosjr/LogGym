package com.loggym.ui.navigation

import kotlinx.coroutines.flow.first
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavBackStackEntry
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loggym.ui.exercises.ExerciseLibraryScreen
import com.loggym.ui.exercises.ExerciseCreateScreen
import com.loggym.ui.plans.PlanEditorViewModel
import com.loggym.ui.history.HistoryDetailScreen
import com.loggym.ui.history.HistoryScreen
import com.loggym.ui.home.HomeScreen
import com.loggym.ui.onboarding.OnboardingScreen
import com.loggym.ui.plans.PlanEditorScreen
import com.loggym.ui.plans.PlansScreen
import com.loggym.ui.session.SessionScreen
import com.loggym.ui.settings.SettingsScreen

@Composable
fun LogGymNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
    openSettings: Boolean = false,
    onSettingsOpened: () -> Unit = {},
    navigationEnabled: Boolean = true,
) {
    val current by navController.currentBackStackEntryAsState()
    val selected = TopLevelDestination.from(current?.destination)
    val locks = remember { mutableStateMapOf<String, MutableState<Boolean>>() }
    val locked = !navigationEnabled || locks[current?.id]?.value == true
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val select: (TopLevelDestination) -> Unit = { if (!locked) navController.selectRoot(it) }
    BackHandler(enabled = selected != null && !keyboardOpen && (selected != TopLevelDestination.HOME || locked)) {
        select(TopLevelDestination.HOME)
    }
    var skipRestoredFade by remember(navController) { mutableStateOf(openSettings) }
    val navigateBack: () -> Unit = { navController.popBackStack() }

    LaunchedEffect(openSettings) {
        if (openSettings) {
            skipRestoredFade = true
            navController.selectRoot(TopLevelDestination.SETTINGS)
            onSettingsOpened()
        }
    }

    LaunchedEffect(current, skipRestoredFade) {
        val entry = current
        if (skipRestoredFade && selected == TopLevelDestination.SETTINGS && entry != null) {
            entry.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
            skipRestoredFade = false
        }
    }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
        if (selected != null && !keyboardOpen) RootNavigationBar(selected, !locked, select)
    }) { padding ->
    NavHost(navController = navController, startDestination = startDestination,
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        enterTransition = {
            if (!skipRestoredFade && TopLevelDestination.from(initialState.destination) != null &&
                TopLevelDestination.from(targetState.destination) != null &&
                TopLevelDestination.from(initialState.destination) != TopLevelDestination.from(targetState.destination))
                fadeIn(tween(120)) else EnterTransition.None
        },
        exitTransition = {
            if (!skipRestoredFade && TopLevelDestination.from(initialState.destination) != null &&
                TopLevelDestination.from(targetState.destination) != null &&
                TopLevelDestination.from(initialState.destination) != TopLevelDestination.from(targetState.destination))
                fadeOut(tween(120)) else ExitTransition.None
        },
    ) {
        composable<OnboardingRoute> {
            val goHome: () -> Unit = {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            }
            OnboardingScreen(
                onPlanSelected = goHome,
                onCreateCustomPlan = {
                    goHome()
                    navController.navigate(PlansRoute)
                },
                onSkip = goHome,
            )
        }
        composable<HomeRoute> { entry -> RootEntry(entry, locks) {
            HomeScreen(
                onOpenSession = { navController.navigate(SessionRoute) },
                onOpenPlans = { select(TopLevelDestination.PLANS) },
            )
        } }
        composable<ExerciseLibraryRoute> { entry -> RootEntry(entry, locks) {
            ExerciseLibraryScreen(onBack = { select(TopLevelDestination.HOME) }, showBack = false)
        } }
        composable<PlansRoute> { entry -> RootEntry(entry, locks) {
            PlansScreen(
                onBack = { select(TopLevelDestination.HOME) }, showBack = false,
                onOpenPlan = { planId -> navController.navigate(PlanEditorRoute(planId)) },
            )
        } }
        composable<PlanEditorRoute> { entry ->
            val viewModel = hiltViewModel<PlanEditorViewModel>(entry)
            val lifecycle by entry.lifecycle.currentStateFlow.collectAsState()
            val created by entry.savedStateHandle.getStateFlow<Long?>("createdExerciseId", null).collectAsState()
            LaunchedEffect(lifecycle, created) {
                if (lifecycle == Lifecycle.State.RESUMED) {
                    viewModel.resumePicker(created)
                    entry.savedStateHandle["createdExerciseId"] = null
                }
            }
            PlanEditorScreen(onBack = navigateBack, viewModel = viewModel, onCreateExercise = {
                viewModel.suspendPicker()
                navController.navigate(ExerciseCreateRoute)
            })
        }
        composable<ExerciseCreateRoute> {
            ExerciseCreateScreen(onBack = navigateBack, onSaved = { id ->
                navController.previousBackStackEntry?.savedStateHandle?.set("createdExerciseId", id)
                navController.popBackStack()
            })
        }
        composable<SessionRoute> {
            SessionScreen(onBack = navigateBack)
        }
        composable<HistoryRoute> { entry -> RootEntry(entry, locks) {
            HistoryScreen(onBack = { select(TopLevelDestination.HOME) }, showBack = false,
                onOpenSession = { navController.navigate(HistoryDetailRoute(it)) })
        } }
        composable<HistoryDetailRoute> {
            HistoryDetailScreen(onBack = navigateBack)
        }
        composable<SettingsRoute> { entry -> RootEntry(entry, locks) {
            SettingsScreen(onBack = { select(TopLevelDestination.HOME) }, showBack = false)
        } }
    }
    }
}


@Composable
private fun RootEntry(entry: NavBackStackEntry,
    locks: SnapshotStateMap<String, MutableState<Boolean>>, content: @Composable () -> Unit) {
    val lock = remember(entry) { mutableStateOf(false) }
    DisposableEffect(entry) {
        locks[entry.id] = lock
        onDispose { locks.remove(entry.id) }
    }
    CompositionLocalProvider(LocalRootNavigationLock provides lock, content = content)
}
