package com.loggym.ui.util

/** Exibe o descanso como "1:30"; zero significa cronômetro desativado (RF-56). */
fun formatRest(seconds: Long): String =
    if (seconds == 0L) "sem descanso" else "%d:%02d".format(seconds / 60, seconds % 60)
