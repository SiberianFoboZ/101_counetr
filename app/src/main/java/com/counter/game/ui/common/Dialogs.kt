package com.counter.game.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun WinnerDialog(winnerName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Победа") },
        text = { Text("Победил $winnerName") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Да",
    dismissLabel: String = "Отмена",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}