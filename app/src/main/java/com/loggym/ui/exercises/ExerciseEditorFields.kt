package com.loggym.ui.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.loggym.R
import com.loggym.domain.model.*
import com.loggym.domain.repository.SaveExerciseResult
import com.loggym.ui.components.label

internal fun SaveExerciseResult.editorError(): EditorError? = when (this) {
    is SaveExerciseResult.Saved -> null
    SaveExerciseResult.BlankName -> EditorError.BLANK_NAME
    SaveExerciseResult.DuplicateName -> EditorError.DUPLICATE_NAME
    SaveExerciseResult.NotEditable -> EditorError.NOT_EDITABLE
}

@Composable
internal fun ExerciseEditorFields(editor: ExerciseDialog.Editor,
    onChange: ((ExerciseDialog.Editor) -> ExerciseDialog.Editor) -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
            Column(
                modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { value -> onChange { it.copy(name = value) } },
                    label = { Text(resources.getString(R.string.ui_nome)) },
                    singleLine = true,
                    isError = editor.error != null,
                    supportingText = {
                        when (editor.error) {
                            EditorError.BLANK_NAME -> Text(resources.getString(R.string.ui_informe_um_nome))
                            EditorError.DUPLICATE_NAME -> Text(resources.getString(R.string.ui_ja_existe_um_exercicio_com_esse_nome))
                            EditorError.NOT_EDITABLE -> Text(resources.getString(R.string.ui_este_exercicio_nao_pode_ser_editado))
                            null -> Text(resources.getString(R.string.ui_ex_movimento_variacao_equipamento))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(resources.getString(R.string.ui_tipo_de_registro_fixo_apos_criar))
                TrackingType.entries.forEach { type ->
                    FilterChip(
                        selected = editor.trackingType == type,
                        enabled = editor.editingId == null,
                        onClick = { onChange { it.copy(trackingType = type) } },
                        label = { Text(if (type == TrackingType.TIME) resources.getString(R.string.ui_tempo_segundos) else resources.getString(R.string.ui_carga_e_repeticoes)) },
                    )
                }
                Text(resources.getString(R.string.ui_grupos_musculares_opcional), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MuscleGroup.entries.forEach { group ->
                        val selected = group in editor.muscleGroups
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onChange {
                                    it.copy(muscleGroups = if (selected) it.muscleGroups - group else it.muscleGroups + group)
                                }
                            },
                            label = { Text(group.label()) },
                        )
                    }
                }
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = { value -> onChange { it.copy(description = value) } },
                    label = { Text(resources.getString(R.string.ui_descricao_ou_instrucao_opcional)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
}
