package com.loggym.ui.exercises

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loggym.R
import com.loggym.ui.components.OperationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCreateScreen(onBack: () -> Unit, onSaved: (Long) -> Unit, viewModel: ExerciseCreateViewModel = hiltViewModel()) {
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val created by viewModel.createdId.collectAsStateWithLifecycle()
    LaunchedEffect(created) { created?.let(onSaved) }
    BackHandler { if (!busy) onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.create_exercise)) }, navigationIcon = {
        IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
    }) }, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_cancelar)) }
            Button(onClick = viewModel::save, enabled = !busy, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_salvar)) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp)) {
            if (failed) OperationError(viewModel::dismissError)
            ExerciseEditorFields(editor, viewModel::update, Modifier.weight(1f))
        }
    }
}
