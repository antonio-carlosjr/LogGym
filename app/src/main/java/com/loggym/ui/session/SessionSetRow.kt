package com.loggym.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.loggym.R
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.ui.theme.LogGymSpacing

private val LeadWidth = 40.dp
private val ColumnGap = 8.dp

/**
 * Tabela de séries: [lead] | campos | [trailing]. Os campos ficam numa linha enquanto couberem
 * valores como "9999,5"; com fonte grande descem para uma segunda linha. A árvore é a mesma em
 * todas as larguras, então foco e texto sobrevivem ao reajuste.
 */
@Composable
internal fun SetColumnsLayout(fieldCount: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val measurer = rememberTextMeasurer()
    val sample = measurer.measure("9999,5", MaterialTheme.typography.titleMedium).size.width
    Layout(content = content, modifier = modifier.fillMaxWidth()) { children, constraints ->
        val gap = ColumnGap.roundToPx()
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val lead = children[0].measure(Constraints(minWidth = LeadWidth.roundToPx(), maxWidth = width))
        val trailing = children.last().measure(loose)
        val left = lead.width + gap
        val inlineSpace = width - left - trailing.width - gap
        val minField = maxOf(56.dp.roundToPx(), sample + 24.dp.roundToPx())
        val inline = inlineSpace - gap * (fieldCount - 1) >= minField * fieldCount
        val space = if (inline) inlineSpace else width - left
        val fieldWidth = ((space - gap * (fieldCount - 1)) / fieldCount).coerceAtLeast(0)
        val fields = (1..fieldCount).map { children[it].measure(Constraints.fixedWidth(fieldWidth)) }
        val fieldsHeight = fields.maxOfOrNull { it.height } ?: 0
        val firstLine = maxOf(lead.height, trailing.height, if (inline) fieldsHeight else 0)
        val height = if (inline) firstLine else firstLine + gap / 2 + fieldsHeight
        layout(width, height) {
            lead.placeRelative(0, (firstLine - lead.height) / 2)
            trailing.placeRelative(width - trailing.width, (firstLine - trailing.height) / 2)
            val y = if (inline) (firstLine - fieldsHeight) / 2 else firstLine + gap / 2
            fields.forEachIndexed { i, field -> field.placeRelative(left + i * (fieldWidth + gap), y) }
        }
    }
}

/** Cabeçalho único das colunas de um exercício, alinhado com [SessionSetRow]. */
@Composable
internal fun SetTableHeader(timed: Boolean) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val labels = if (timed) listOf(stringResource(R.string.set_column_duration))
        else listOf(stringResource(R.string.set_column_weight), stringResource(R.string.set_column_reps))
    SetColumnsLayout(labels.size, Modifier.padding(horizontal = LogGymSpacing.small).clearAndSetSemantics {}) {
        Text("#", style = style, color = color, modifier = Modifier.width(LeadWidth),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        labels.forEach {
            Text(it, style = style, color = color, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(Modifier.width(LogGymSpacing.touchTarget))
    }
}

/** RF-164/166: apresentação somente; conclusão e escrita continuam no ViewModel. */
@Composable
internal fun SessionSetRow(exercise: SessionExercise, set: SessionSet, index: Int, entry: SetEntry,
    busy: Boolean, completing: Boolean, onToggle: () -> Unit, onRemove: () -> Unit,
    fields: @Composable () -> Unit) {
    val completionLabel = stringResource(if (set.isCompleted) R.string.set_completed else R.string.set_pending)
    val color by animateColorAsState(if (set.isCompleted) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow, tween(150), label = "set-completion")
    val toggleLabel = stringResource(R.string.ui_concluir_serie_1_s_de_2_s, index, exercise.name)
    val removeLabel = stringResource(R.string.ui_remover_serie_1_s, index)
    val optionsLabel = stringResource(R.string.set_options, index)
    val canRemove = !set.isCompleted && !busy && !completing
    var menu by remember { mutableStateOf(false) }
    Surface(color = color, modifier = Modifier.fillMaxWidth().testTag("set-row-${set.id}")
        .semantics {
            isTraversalGroup = true
            if (canRemove) customActions = listOf(CustomAccessibilityAction(removeLabel) { onRemove(); true })
        }) {
        Column(Modifier.padding(horizontal = LogGymSpacing.small, vertical = LogGymSpacing.extraSmall)) {
            SetColumnsLayout(if (exercise.trackingType == TrackingType.TIME) 1 else 2) {
                // Número da série: abre as opções (remover) enquanto a série está pendente.
                Box(Modifier.width(LeadWidth).height(LogGymSpacing.touchTarget)
                    .clickable(enabled = canRemove, onClickLabel = optionsLabel) { menu = true }
                    .semantics { contentDescription = optionsLabel },
                    contentAlignment = Alignment.Center) {
                    Text(index.toString(), style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (set.isCompleted) FontWeight.Bold else FontWeight.Normal,
                        color = if (set.isCompleted) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    DropdownMenu(expanded = menu && canRemove, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(removeLabel) }, onClick = { menu = false; onRemove() })
                    }
                }
                fields()
                // Um único alvo/estado acessível de 48 dp; desenho nativo conserva sua medida.
                Box(Modifier.size(LogGymSpacing.touchTarget)
                    .toggleable(set.isCompleted, enabled = !busy && !completing,
                        role = Role.Checkbox, onValueChange = { onToggle() })
                    .semantics {
                        contentDescription = toggleLabel
                        stateDescription = completionLabel
                    }, contentAlignment = Alignment.Center) {
                    Checkbox(checked = set.isCompleted, onCheckedChange = null,
                        enabled = !busy && !completing, modifier = Modifier.clearAndSetSemantics {})
                }
            }
            entry.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = LeadWidth + ColumnGap))
            }
            if (set.isCompleted && SetValues(set.weightKg, set.reps, set.durationSeconds).isIncomplete(exercise.trackingType))
                Text(stringResource(R.string.ui_serie_marcada_com_campos_incompletos),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = LeadWidth + ColumnGap))
        }
    }
}
