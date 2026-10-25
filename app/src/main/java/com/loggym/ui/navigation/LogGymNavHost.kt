package com.loggym.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loggym.ui.exercises.ExerciseLibraryScreen
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
) {
    val navigateBack: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = startDestination) {
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
        composable<HomeRoute> {
            HomeScreen(
                onOpenSession = { navController.navigate(SessionRoute) },
                onOpenPlans = { navController.navigate(PlansRoute) },
                onOpenExercises = { navController.navigate(ExerciseLibraryRoute) },
                onOpenHistory = { navController.navigate(HistoryRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
            )
        }
        composable<ExerciseLibraryRoute> {
            ExerciseLibraryScreen(onBack = navigateBack)
        }
        composable<PlansRoute> {
            PlansScreen(
                onBack = navigateBack,
                onOpenPlan = { planId -> navController.navigate(PlanEditorRoute(planId)) },
            )
        }
        composable<PlanEditorRoute> {
            PlanEditorScreen(onBack = navigateBack)
        }
        composable<SessionRoute> {
            SessionScreen(onBack = navigateBack)
        }
        composable<HistoryRoute> {
            HistoryScreen(onBack = navigateBack, onOpenSession = { navController.navigate(HistoryDetailRoute(it)) })
        }
        composable<HistoryDetailRoute> {
            HistoryDetailScreen(onBack = navigateBack)
        }
        composable<SettingsRoute> { SettingsScreen(onBack = navigateBack) }
    }
}
