package com.counter.game.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.data.entity.GameStatus

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    container: AppContainer,
    playerId: Long,
    onBack: () -> Unit,
) {
    val factory = remember(playerId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlayerDetailViewModel(container, playerId) as T
        }
    }
    val vm: PlayerDetailViewModel = viewModel(factory = factory, key = "player_detail_$playerId")
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playerName.ifBlank { "Игрок" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (state.games == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Этот игрок ещё не участвовал ни в одной игре",
                        color = Color(0xFF666666),
                        fontSize = 16.sp,
                    )
                }
                return@Surface
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    StatsHeader(state)
                }
                items(state.history, key = { it.gameId }) { game ->
                    GameHistoryCard(game)
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(state: PlayerDetailState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(
            state.playerName,
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Побед: ${state.wins} · Игр: ${state.games}",
            color = Color(0xFF666666),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun GameHistoryCard(game: PlayerGameHistory) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Игра #${game.gameId}",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                val statusLabel = when (game.status) {
                    GameStatus.FINISHED.name -> if (game.isWin) "победа" else "проигрыш"
                    GameStatus.PAUSED.name -> "на паузе"
                    else -> "в процессе"
                }
                Text(
                    statusLabel,
                    color = if (game.isWin) Color(0xFF666666) else Color(0xFF999999),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                formatDelta(game.totalDelta),
                color = if (game.totalDelta > 0) Color.Black else if (game.totalDelta < 0) Color(0xFF666666) else Color(0xFF999999),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (game.rounds.isEmpty()) {
            Text(
                "Нет раундов",
                color = Color(0xFF999999),
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                game.rounds.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Раунд ${r.roundNumber}",
                            color = Color(0xFF666666),
                            fontSize = 14.sp,
                        )
                        Text(
                            formatDelta(r.delta),
                            color = if (r.delta > 0) Color.Black else Color(0xFF666666),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDelta(d: Int): String = when {
    d > 0 -> "+$d"
    d == 0 -> "0"
    else -> d.toString()
}