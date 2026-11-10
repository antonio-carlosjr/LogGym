package com.loggym.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loggym.ui.theme.LogGymSpacing

/**
 * Campo numérico sem rótulo para tabelas de séries. O rótulo fica no cabeçalho da coluna;
 * [description] mantém o nome anunciado pelo leitor de tela. Ao receber foco, seleciona o
 * valor para que o pré-preenchimento possa ser sobrescrito sem apagar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Number,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // O texto externo (ViewModel) é a fonte de verdade; a seleção local só vale enquanto coincide.
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    )
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = shown,
        onValueChange = { next ->
            field = next
            if (next.text != value) onValueChange(next.text)
        },
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }, onNext = { onImeAction() }),
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LogGymSpacing.touchTarget)
            .semantics { contentDescription = description }
            .onFocusChanged { state ->
                if (state.isFocused && !focused) field = shown.copy(selection = TextRange(0, shown.text.length))
                focused = state.isFocused
            },
    ) { inner ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = shown.text,
            innerTextField = inner,
            enabled = enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interaction,
            isError = isError,
            colors = colors,
            contentPadding = PaddingValues(horizontal = LogGymSpacing.small, vertical = 12.dp),
            container = {
                OutlinedTextFieldDefaults.Container(enabled, isError, interaction, colors = colors)
            },
        )
    }
}
