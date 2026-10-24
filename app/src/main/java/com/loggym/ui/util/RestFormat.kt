package com.loggym.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.loggym.R
import java.util.Locale

/** Exibe o descanso como "1:30"; zero significa cronômetro desativado (RF-56). */
fun formatRest(seconds: Long): String =
    String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

@Composable
fun restLabel(seconds: Long): String = if (seconds == 0L) stringResource(R.string.no_rest) else formatRest(seconds)
