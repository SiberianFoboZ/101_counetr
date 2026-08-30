package com.counter.game.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    // Автопауза: если экран уходит в фон (нажали back, свернули приложение, нажали «Домой»),
    // и игра при этом IN_PROGRESS — переводим в PAUSED. При повторном открытии юзер увидит
    // её как доступную для «Продолжить» с главного экрана.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.autoPauseIfActive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.displayName, color = if (faded) Color(0xFF8E8E8E) else Color.Black)
                                if (faded) {
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        "выбыл",
                                        color = Color(0xFF8E8E8E),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Text(
                                p.totalScore.toString(),
                                color = if (faded) Color(0xFF8E8E8E) else Color.Black,
                                fontWeight = if (faded) FontWeight.Normal else FontWeight.Bold,
                            )
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
                    onClick = { vm.finishGame(onBack) },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                ) {
                    Text("Завершить игру", color = Color.White)
                }
            }
        }
    }

    winnerShown?.let { name ->
        WinnerDialog(winnerName = name, onDismiss = { winnerShown = null })
    }
}