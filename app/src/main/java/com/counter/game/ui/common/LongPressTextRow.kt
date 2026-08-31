package com.counter.game.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LongPressTextRow(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    faded: Boolean = false,
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(text) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onTap() },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (faded) MaterialTheme.colorScheme.outline else textColor
        Text(text = text, color = color, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Диалог переименования.
 *
 * - При первом показе и при изменении `initial` поле сбрасывается в актуальное значение.
 * - Тап в поле автоматически выделяет весь текст, так что первый введённый символ
 *   сразу заменяет прежнее имя без необходимости стирать его вручную.
 */
@Composable
fun RenameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(makeSelectedFieldValue(initial)) }
    LaunchedEffect(initial) {
        value = makeSelectedFieldValue(initial)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = value,
                onValueChange = { newValue ->
                    value = if (newValue.text != initial && value.selection.collapsed.not()) {
                        // Пользователь начал ввод, выделение больше не нужно — ставим курсор в конец.
                        newValue.copy(selection = TextRange(newValue.text.length))
                    } else {
                        newValue
                    }
                },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value.text) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun makeSelectedFieldValue(text: String): TextFieldValue =
    TextFieldValue(text = text, selection = TextRange(0, text.length))

/**
 * Обёртка над Material3 OutlinedTextField с поведением "selectAllOnFocus":
 * при тапе в поле с непустым значением весь текст автоматически выделяется,
 * и первый введённый символ заменяет прежнее содержимое.
 *
 * Используйте вместо обычного `OutlinedTextField(value = ..., onValueChange = ...)`,
 * если хотите, чтобы ввод начинался с чистого поля без ручного стирания.
 *
 * При внешнем изменении `value` (например, при ресете формы родителем) —
 * содержимое пересинхронизируется и снова выделяется целиком.
 */
@Composable
fun QuickClearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
) {
    var fieldValue by remember { mutableStateOf(makeSelectedFieldValue(value)) }
    LaunchedEffect(value) {
        if (fieldValue.text != value) {
            fieldValue = makeSelectedFieldValue(value)
        }
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            fieldValue = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        modifier = modifier,
        label = label,
        singleLine = singleLine,
    )
}