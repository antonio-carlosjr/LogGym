package com.loggym.ui.theme

import android.content.res.Resources
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.loggym.R

/** Presentation only: brand resources follow the resolved preference, not just system night mode. */
internal val LocalLogGymDarkTheme = staticCompositionLocalOf { false }
private val LogGymTypography = Typography()
private val LogGymShapes = Shapes(medium = RoundedCornerShape(12.dp))

internal fun logGymColorScheme(resources: Resources, dark: Boolean): ColorScheme {
    fun color(id: Int) = Color(resources.getColor(id, null))
    return if (dark) darkColorScheme(
        primary = color(R.color.lg_dark_primary),
        onPrimary = color(R.color.lg_dark_bg),
        primaryContainer = color(R.color.lg_dark_primary_container),
        onPrimaryContainer = color(R.color.lg_dark_text),
        inversePrimary = color(R.color.lg_dark_inverse_primary),
        secondary = color(R.color.lg_dark_secondary),
        onSecondary = color(R.color.lg_dark_bg),
        secondaryContainer = color(R.color.lg_dark_secondary_container),
        onSecondaryContainer = color(R.color.lg_dark_text),
        tertiary = color(R.color.lg_dark_primary),
        onTertiary = color(R.color.lg_dark_bg),
        tertiaryContainer = color(R.color.lg_dark_primary_container),
        onTertiaryContainer = color(R.color.lg_dark_text),
        background = color(R.color.lg_dark_bg),
        onBackground = color(R.color.lg_dark_text),
        surface = color(R.color.lg_dark_surface),
        onSurface = color(R.color.lg_dark_text),
        surfaceVariant = color(R.color.lg_dark_container_highest),
        onSurfaceVariant = color(R.color.lg_dark_secondary),
        surfaceTint = color(R.color.lg_dark_primary),
        inverseSurface = color(R.color.lg_dark_inverse_surface),
        inverseOnSurface = color(R.color.lg_dark_inverse_text),
        error = color(R.color.lg_dark_error),
        onError = color(R.color.lg_dark_on_error),
        errorContainer = color(R.color.lg_dark_error_container),
        onErrorContainer = color(R.color.lg_dark_on_error_container),
        outline = color(R.color.lg_dark_secondary),
        outlineVariant = color(R.color.lg_dark_outline),
        scrim = color(R.color.lg_scrim),
        surfaceBright = color(R.color.lg_dark_surface_bright),
        surfaceDim = color(R.color.lg_dark_surface_dim),
        surfaceContainer = color(R.color.lg_dark_container),
        surfaceContainerHigh = color(R.color.lg_dark_container_high),
        surfaceContainerHighest = color(R.color.lg_dark_container_highest),
        surfaceContainerLow = color(R.color.lg_dark_container_low),
        surfaceContainerLowest = color(R.color.lg_dark_bg),
        primaryFixed = color(R.color.lg_light_primary_container),
        primaryFixedDim = color(R.color.lg_light_fixed_dim),
        onPrimaryFixed = color(R.color.lg_light_text),
        onPrimaryFixedVariant = color(R.color.lg_light_primary),
        secondaryFixed = color(R.color.lg_light_primary_container),
        secondaryFixedDim = color(R.color.lg_light_fixed_dim),
        onSecondaryFixed = color(R.color.lg_light_text),
        onSecondaryFixedVariant = color(R.color.lg_light_primary),
        tertiaryFixed = color(R.color.lg_light_primary_container),
        tertiaryFixedDim = color(R.color.lg_light_fixed_dim),
        onTertiaryFixed = color(R.color.lg_light_text),
        onTertiaryFixedVariant = color(R.color.lg_light_primary),
    ) else lightColorScheme(
        primary = color(R.color.lg_light_primary),
        onPrimary = color(R.color.lg_light_surface),
        primaryContainer = color(R.color.lg_light_primary_container),
        onPrimaryContainer = color(R.color.lg_light_text),
        inversePrimary = color(R.color.lg_light_inverse_primary),
        secondary = color(R.color.lg_light_secondary),
        onSecondary = color(R.color.lg_light_surface),
        secondaryContainer = color(R.color.lg_light_secondary_container),
        onSecondaryContainer = color(R.color.lg_light_text),
        tertiary = color(R.color.lg_light_primary),
        onTertiary = color(R.color.lg_light_surface),
        tertiaryContainer = color(R.color.lg_light_primary_container),
        onTertiaryContainer = color(R.color.lg_light_text),
        background = color(R.color.lg_light_bg),
        onBackground = color(R.color.lg_light_text),
        surface = color(R.color.lg_light_surface),
        onSurface = color(R.color.lg_light_text),
        surfaceVariant = color(R.color.lg_light_container_highest),
        onSurfaceVariant = color(R.color.lg_light_secondary),
        surfaceTint = color(R.color.lg_light_primary),
        inverseSurface = color(R.color.lg_light_inverse_surface),
        inverseOnSurface = color(R.color.lg_light_inverse_text),
        error = color(R.color.lg_light_error),
        onError = color(R.color.lg_light_on_error),
        errorContainer = color(R.color.lg_light_error_container),
        onErrorContainer = color(R.color.lg_light_on_error_container),
        outline = color(R.color.lg_light_secondary),
        outlineVariant = color(R.color.lg_light_outline),
        scrim = color(R.color.lg_scrim),
        surfaceBright = color(R.color.lg_light_surface_bright),
        surfaceDim = color(R.color.lg_light_surface_dim),
        surfaceContainer = color(R.color.lg_light_container),
        surfaceContainerHigh = color(R.color.lg_light_container_high),
        surfaceContainerHighest = color(R.color.lg_light_container_highest),
        surfaceContainerLow = color(R.color.lg_light_container_low),
        surfaceContainerLowest = color(R.color.lg_light_surface),
        primaryFixed = color(R.color.lg_light_primary_container),
        primaryFixedDim = color(R.color.lg_light_fixed_dim),
        onPrimaryFixed = color(R.color.lg_light_text),
        onPrimaryFixedVariant = color(R.color.lg_light_primary),
        secondaryFixed = color(R.color.lg_light_primary_container),
        secondaryFixedDim = color(R.color.lg_light_fixed_dim),
        onSecondaryFixed = color(R.color.lg_light_text),
        onSecondaryFixedVariant = color(R.color.lg_light_primary),
        tertiaryFixed = color(R.color.lg_light_primary_container),
        tertiaryFixedDim = color(R.color.lg_light_fixed_dim),
        onTertiaryFixed = color(R.color.lg_light_text),
        onTertiaryFixedVariant = color(R.color.lg_light_primary),
    )
}

@Composable
fun LogGymTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val colors = remember(resources, darkTheme) { logGymColorScheme(resources, darkTheme) }
    CompositionLocalProvider(LocalLogGymDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colors, typography = LogGymTypography, shapes = LogGymShapes, content = content)
    }
}
