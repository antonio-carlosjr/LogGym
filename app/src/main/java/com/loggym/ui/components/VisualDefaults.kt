package com.loggym.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing

/** Only shared visual defaults, no navigation/state/interaction abstraction. */
object LogGymCardDefaults {
    @Composable fun colors(selected: Boolean = false) = CardDefaults.cardColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface)
    @Composable fun elevation() = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
}

/** Read-only titles/actions can reflow; workout input fields never use this component. */
@Composable
fun AdaptiveActionHeader(title: String, actionsWidth: Dp, modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < actionsWidth + 128.dp * fontScale + LogGymSpacing.small) {
            Column(verticalArrangement = Arrangement.spacedBy(LogGymSpacing.extraSmall)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
            }
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() })
            actions()
        }
    }
}
