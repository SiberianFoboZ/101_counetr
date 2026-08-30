package com.counter.game.ui.game

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.common.WinnerDialog
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    container: AppContainer,
    gameId: Long,
    onBack: () -> Unit,
    onRound: (Long) -> Unit,
    onHistory: (Long) -> Unit,
) {
    val vm: GameViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    var winnerShown by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(gameId) { vm.load(gameId) }

    LaunchedEffect(state.winnerName) {
        state.winnerName?.let {
            if (winnerShown != it) {
                winnerShown = it
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Игра #$gameId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "В меню")
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
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Очки (лимит ${state.threshold})", color = Color.Black)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.players, key = { it.id }) { p ->
                        val faded = p.totalScore > state.threshold
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(p.displayName, color = if (faded) Color(0xFFBDBDBD) else Color.Black)
                            Text(p.totalScore.toString(), color = if (faded) Color(0xFFBDBDBD) else Color.Black)
                        }
                    }
                }
                Button(
                    onClick = { onRound(gameId) },
                    enabled = state.game?.status != com.counter.game.data.entity.GameStatus.FINISHED.name,
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBDBDBD),
                    ),
                ) {
                    Text("Раунд")
                }
                Button(
                    onClick = { onHistory(gameId) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Black)
                    Text(" История", color = Color.Black)
                }
                Button(
                    onClick = { vm.pauseAndExit(onBack) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) {
                    Text("В главное меню (пауза)", color = Color.Black)
                }
            }
        }
    }

    winnerShown?.let { name ->
        WinnerDialog(winnerName = name, onDismiss = { winnerShown = null })
    }
}