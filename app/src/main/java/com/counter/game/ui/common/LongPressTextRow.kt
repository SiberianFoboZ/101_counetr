package com.counter.game.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LongPressTextRow(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
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
        val color = if (faded) Color(0xFFBDBDBD) else textColor
        Text(text = text, color = color, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun RenameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}