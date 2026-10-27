package com.loggym.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loggym.R

internal val LocalRootNavigationLock = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** Estado pertence à entrada de navegação, inclusive durante a transição de saída. */
@Composable
fun RootNavigationLock(locked: Boolean) {
    val owner = LocalRootNavigationLock.current
    SideEffect { owner?.value = locked }
    DisposableEffect(owner) { onDispose { owner?.value = false } }
}

@Composable
internal fun RootNavigationBar(selected: TopLevelDestination, enabled: Boolean, onSelect: (TopLevelDestination) -> Unit) {
    // Altura flexível: não cortar rótulos quando a fonte do sistema cresce.
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup().testTag("root-navigation")) {
            TopLevelDestination.entries.forEach { destination ->
                val current = selected == destination
                val description = stringResource(if (destination == TopLevelDestination.SETTINGS) R.string.settings_title else destination.label)
                Column(Modifier.weight(1f).heightIn(min = 72.dp)
                    .selectable(current, enabled = enabled, role = Role.Tab, onClick = { onSelect(destination) })
                    .testTag("tab-${destination.name.lowercase()}")
                    .semantics { contentDescription = description }
                    .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.width(56.dp).height(28.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center) {
                        Icon(painterResource(destination.icon), null, Modifier.size(24.dp),
                            tint = if (current) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(destination.label), style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
