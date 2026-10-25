package com.loggym.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.loggym.R
import com.loggym.domain.logic.SetValues
import com.loggym.domain.model.*
import com.loggym.ui.theme.LogGymSpacing

/** RF-164/166: apresentação somente; conclusão e escrita continuam no ViewModel. */
@Composable
internal fun SessionSetRow(exercise: SessionExercise, set: SessionSet, index: Int, entry: SetEntry,
    busy: Boolean, completing: Boolean, onToggle: () -> Unit, onRemove: () -> Unit,
    fields: @Composable () -> Unit) {
    val completionLabel = stringResource(if (set.isCompleted) R.string.set_completed else R.string.set_pending)
    val color by animateColorAsState(if (set.isCompleted) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow, tween(150), label = "set-completion")
    val ordinalLabel = stringResource(R.string.ui_serie_1_s, index)
    val toggleLabel = stringResource(R.string.ui_concluir_serie_1_s_de_2_s, index, exercise.name)
    val timed = exercise.trackingType == TrackingType.TIME
    val measurer = rememberTextMeasurer()
    val labels = if (timed) listOf(stringResource(R.string.ui_duracao_s))
        else listOf(stringResource(R.string.ui_carga_kg), stringResource(R.string.ui_repeticoes))
    val minimumTextWidth = labels.maxOf { measurer.measure(it, MaterialTheme.typography.labelSmall).size.width }
        .coerceAtLeast(measurer.measure("999999,99", MaterialTheme.typography.bodyLarge).size.width)
    Surface(color = color, modifier = Modifier.fillMaxWidth().testTag("set-row-${set.id}")
        .semantics { isTraversalGroup = true }) {
        Column(Modifier.padding(horizontal = LogGymSpacing.small, vertical = LogGymSpacing.extraSmall)) {
            // A árvore dos campos é a mesma em todas as larguras: apenas medida/posição mudam.
            Layout(modifier = Modifier.fillMaxWidth(), content = {
                Text(index.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.semantics { contentDescription = ordinalLabel })
                fields()
                Row {
                    Checkbox(checked = set.isCompleted, onCheckedChange = { onToggle() },
                        enabled = !busy && !completing,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                            contentDescription = toggleLabel
                            stateDescription = completionLabel
                        })
                    if (!set.isCompleted) IconButton(onClick = onRemove, enabled = !busy && !completing,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.ui_remover_serie_1_s, index))
                    } else Spacer(Modifier.size(48.dp))
                }
            }) { children, constraints ->
                val gap = 8.dp.roundToPx()
                val fieldCount = if (timed) 1 else 2
                val width = constraints.maxWidth
                val loose = Constraints(maxWidth = width)
                val ordinal = children[0].measure(loose)
                val actions = children.last().measure(loose)
                val minField = maxOf(100.dp.roundToPx(), minimumTextWidth + 32.dp.roundToPx())
                val left = maxOf(24.dp.roundToPx(), ordinal.width) + gap
                val allInline = width >= left + minField * fieldCount + gap * fieldCount + actions.width
                val available = (width - left - if (allInline) actions.width + gap else 0).coerceAtLeast(0)
                val stacked = fieldCount == 2 && available < minField * 2 + gap
                val fieldWidth = if (stacked) available else (available - gap * (fieldCount - 1)).coerceAtLeast(0) / fieldCount
                val inputs = (1..fieldCount).map { children[it].measure(Constraints.fixedWidth(fieldWidth)) }
                val inputsHeight = if (stacked) inputs.sumOf { it.height } + gap else inputs.maxOf { it.height }
                val firstHeight = maxOf(inputsHeight, ordinal.height, if (allInline) actions.height else 0)
                val height = firstHeight + if (allInline) 0 else 4.dp.roundToPx() + actions.height
                layout(width, height) {
                    ordinal.placeRelative(0, (inputs.first().height - ordinal.height).coerceAtLeast(0) / 2)
                    var y = 0
                    inputs.forEachIndexed { i, input ->
                        input.placeRelative(left + if (stacked) 0 else i * (fieldWidth + gap), y)
                        if (stacked) y += input.height + gap
                    }
                    actions.placeRelative(width - actions.width, if (allInline) (height - actions.height) / 2 else firstHeight + 4.dp.roundToPx())
                }
            }
            entry.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (set.isCompleted && SetValues(set.weightKg, set.reps, set.durationSeconds).isIncomplete(exercise.trackingType))
                Text(stringResource(R.string.ui_serie_marcada_com_campos_incompletos),
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
