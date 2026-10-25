package com.loggym.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing
import androidx.compose.ui.res.stringResource
import com.loggym.R

@Composable
fun LoadError(onRetry: () -> Unit, message: String = stringResource(R.string.load_failed)) {
    Column(Modifier.padding(vertical = LogGymSpacing.small), verticalArrangement = Arrangement.spacedBy(LogGymSpacing.extraSmall)) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = LogGymSpacing.screen), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LogGymSpacing.medium)) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(message, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.padding(vertical = LogGymSpacing.screen))
}

@Composable
fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

@Composable
fun OperationError(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.operation_failed)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss_error)) } })
}
