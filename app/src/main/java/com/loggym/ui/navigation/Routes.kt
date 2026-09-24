package com.loggym.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data object ExerciseLibraryRoute

@Serializable
data object PlansRoute

@Serializable
data class PlanEditorRoute(val planId: Long)

@Serializable
data object SessionRoute

@Serializable
data object HistoryRoute

@Serializable
data class HistoryDetailRoute(val sessionId: Long)
