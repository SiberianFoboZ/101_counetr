package com.counter.game.ui.newgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(state.startedGameId) {
        state.startedGameId?.let { id ->
            onGameStarted(id)
            vm.consumeStartedGame()
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
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                ),
            )
        },
        floatingActionButton = {},
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Выберите от 2 до 10 игроков", color = Color.Black)
                Text("Выбрано: ${state.selectedIds.size}", color = Color(0xFF666666))
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                                Text(p.name, color = Color.Black)
                            }
                        }
                    }
                }
                state.errorMessage?.let { msg ->
                    Text(msg, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = vm::start,
                    enabled = !state.isStarting,
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBDBDBD),
                    ),
                ) {
                    Text(if (state.isStarting) "Старт…" else "Старт")
                }
            }
        }
    }
}