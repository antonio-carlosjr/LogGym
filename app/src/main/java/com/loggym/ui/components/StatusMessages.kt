package com.loggym.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.loggym.R

@Composable
fun LoadError(onRetry: () -> Unit) {
    Column {
        Text(stringResource(R.string.load_failed), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun OperationError(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss_error)) } })
}
