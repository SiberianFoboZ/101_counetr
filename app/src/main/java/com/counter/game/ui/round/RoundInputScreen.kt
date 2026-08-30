package com.counter.game.ui.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

private val CardRows = listOf(
    "6", "7", "8", "9", "10", "J", "A",
    "Q_hearts", "Q_diamonds", "Q_clubs", "Q_spades",
    "K_hearts", "K_diamonds", "K_clubs", "K_spades",
)

private val CardLabels = mapOf(
    "6" to "6", "7" to "7", "8" to "8", "9" to "9", "10" to "10",
    "J" to "Валет", "A" to "Туз",
    "Q_hearts" to "Дама ♥", "Q_diamonds" to "Дама ♦", "Q_clubs" to "Дама ♣", "Q_spades" to "Дама ♠",
    "K_hearts" to "Король ♥", "K_diamonds" to "Король ♦", "K_clubs" to "Король ♣", "K_spades" to "Король ♠",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RoundInputScreen(
    container: AppContainer,
    gameId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: RoundInputViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    var dialogShownFor by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(gameId) { vm.load(gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый раунд") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Выберите победителя раунда (не получает штраф):", color = Color.Black)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.players, key = { it.id }) { p ->
                        Button(
                            onClick = { vm.selectWinner(p.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.winnerGamePlayerId == p.id) Color.Black else Color.White,
                                contentColor = if (state.winnerGamePlayerId == p.id) Color.White else Color.Black,
                            ),
                        ) {
                            Text(p.displayName + " (счёт ${p.totalScore})")
                        }
                    }
                }

                Text("Карты на руках у проигравших (для победителя — пропускается):", color = Color.Black)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.players.filter { it.id != state.winnerGamePlayerId }, key = { it.id }) { p ->
                        PlayerHandEditor(
                            playerName = p.displayName,
                            counts = state.handCounts[p.id].orEmpty(),
                            onChange = { code, count -> vm.changeCount(p.id, code, count) },
                        )
                    }
                }

                Button(
                    onClick = {
                        vm.save()
                        onSaved()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBDBDBD),
                    ),
                ) {
                    Text(if (state.isSaving) "Сохранение…" else "Сохранить раунд")
                }
            }
        }
    }

    val dialog = state.suitDialog
    if (dialog != null && dialogShownFor != dialog.gamePlayerId) {
        dialogShownFor = dialog.gamePlayerId
        AlertDialog(
            onDismissRequest = vm::dismissSuitDialog,
            title = { Text("Выберите масть") },
            text = { Text("На руках осталась одна карта — ${CardLabels[dialog.currentCode]}. Подтвердите масть.") },
            confirmButton = {
                Row {
                    SuitButton("♠") { vm.chooseSuit("spades") }
                    SuitButton("♥") { vm.chooseSuit("hearts") }
                    SuitButton("♦") { vm.chooseSuit("diamonds") }
                    SuitButton("♣") { vm.chooseSuit("clubs") }
                }
            },
        )
    }
}

@Composable
private fun SuitButton(symbol: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
    ) { Text(symbol) }
}

@Composable
private fun PlayerHandEditor(
    playerName: String,
    counts: Map<String, Int>,
    onChange: (String, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(playerName, color = Color.Black, style = MaterialTheme.typography.titleLarge)
        CardRows.forEach { code ->
            val value = counts[code] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(CardLabels[code] ?: code, modifier = Modifier.weight(1f), color = Color.Black)
                IconButton(onClick = { onChange(code, (value - 1).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Убрать", tint = Color.Black)
                }
                OutlinedTextField(
                    value = value.toString(),
                    onValueChange = { txt -> txt.toIntOrNull()?.let { onChange(code, it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onChange(code, value + 1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) { Text("+") }
            }
        }
    }
}