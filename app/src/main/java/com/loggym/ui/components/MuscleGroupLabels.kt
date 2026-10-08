package com.loggym.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.loggym.R
import com.loggym.domain.model.MuscleGroup

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_group_chest
    MuscleGroup.BACK -> R.string.muscle_group_back
    MuscleGroup.TRAPS -> R.string.muscle_group_traps
    MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
    MuscleGroup.BICEPS -> R.string.muscle_group_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_group_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_group_forearms
    MuscleGroup.ABS -> R.string.muscle_group_abs
    MuscleGroup.QUADS -> R.string.muscle_group_quads
    MuscleGroup.HAMSTRINGS -> R.string.muscle_group_hamstrings
    MuscleGroup.GLUTES -> R.string.muscle_group_glutes
    MuscleGroup.CALVES -> R.string.muscle_group_calves
    MuscleGroup.ADDUCTORS -> R.string.muscle_group_adductors
    MuscleGroup.ABDUCTORS -> R.string.muscle_group_abductors
}

@Composable
fun MuscleGroup.label(): String = stringResource(labelRes())

/** Rótulos em ordem fixa, separados por vírgula. */
@Composable
fun Set<MuscleGroup>.labels(): String =
    sortedBy { it.ordinal }.map { stringResource(it.labelRes()) }.joinToString(", ")
