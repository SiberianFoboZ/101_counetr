package com.counter.game.ui.newgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NewGameScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onGameStarted: (Long) -> Unit,
) {
    val vm: NewGameViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.startedGameId) {
        state.startedGameId?.let { id ->
            onGameStarted(id)
            vm.consumeStartedGame()
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.consumeInfo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая игра") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Выберите от 2 до 10 игроков",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Выбрано: ${state.selectedIds.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(
                    onClick = { vm.addPlayer() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить игрока",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            state.errorMessage?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (state.players.isEmpty()) {
                Text(
                    "Нет ни одного игрока. Нажмите «+», чтобы добавить.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.players, key = { it.id }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.toggle(p.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = p.id in state.selectedIds,
                                onCheckedChange = { vm.toggle(p.id) },
                            )
                            Text(p.name, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                Button(
                    onClick = vm::start,
                    enabled = !state.isStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .sizeIn(minHeight = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(if (state.isStarting) "Старт…" else "Старт")
                }
            }
        }
    }
}