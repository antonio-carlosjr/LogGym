package com.loggym.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.loggym.R

/** RF-163: seleção sempre derivada da rota efetiva, sem índice paralelo. */
enum class TopLevelDestination(val route: Any, @StringRes val label: Int, @DrawableRes val icon: Int) {
    HOME(HomeRoute, R.string.nav_home, R.drawable.ic_nav_home),
    PLANS(PlansRoute, R.string.ui_planos, R.drawable.ic_nav_plans),
    EXERCISES(ExerciseLibraryRoute, R.string.ui_exercicios, R.drawable.ic_nav_exercises),
    HISTORY(HistoryRoute, R.string.history_title, R.drawable.ic_history),
    SETTINGS(SettingsRoute, R.string.nav_settings, R.drawable.ic_nav_settings);

    companion object {
        fun from(destination: NavDestination?): TopLevelDestination? = when {
            destination == null -> null
            destination.hasRoute<HomeRoute>() -> HOME
            destination.hasRoute<PlansRoute>() -> PLANS
            destination.hasRoute<ExerciseLibraryRoute>() -> EXERCISES
            destination.hasRoute<HistoryRoute>() -> HISTORY
            destination.hasRoute<SettingsRoute>() -> SETTINGS
            else -> null
        }
    }
}

fun NavController.selectRoot(destination: TopLevelDestination) {
    if (TopLevelDestination.from(currentDestination) == destination) return
    navigate(destination.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo<HomeRoute> { saveState = true }
    }
}

/** Limpa também os stores de entradas que não estão na pilha ativa. */
fun NavController.clearRootStacks() {
    clearBackStack<HomeRoute>()
    clearBackStack<PlansRoute>()
    clearBackStack<ExerciseLibraryRoute>()
    clearBackStack<HistoryRoute>()
    clearBackStack<SettingsRoute>()
    popBackStack(graph.id, inclusive = true)
}
