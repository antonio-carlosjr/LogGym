package com.loggym.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loggym.R
import com.loggym.ui.theme.LocalLogGymDarkTheme
import com.loggym.ui.theme.LogGymSpacing

/** Original two-color wordmark; actual Compose theme may differ from Android night resources. */
@Composable
fun LogGymLogo(modifier: Modifier = Modifier) {
    Image(painterResource(if (LocalLogGymDarkTheme.current) R.drawable.ic_loggym_logo_dark else R.drawable.ic_loggym_logo_light),
        contentDescription = null, modifier = modifier.widthIn(max = 240.dp).fillMaxWidth()
            .aspectRatio(538f / 120f).testTag("loggym-logo"))
}

@Composable
fun LogGymHomeTitle() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LogGymSpacing.small)) {
        Icon(painterResource(R.drawable.ic_loggym_symbol), contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(width = 40.dp, height = 20.dp))
        Text(stringResource(R.string.app_name))
    }
}
