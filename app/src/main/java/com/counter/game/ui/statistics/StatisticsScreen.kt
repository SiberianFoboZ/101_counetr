package com.counter.game.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.data.dao.GameWithWinner
import com.counter.game.data.dao.PlayerStatsRow
import com.counter.game.data.entity.GameStatus
import com.counter.game.ui.viewModelFactory

private enum class StatsTab(val label: String) {
    Games("Игры"),
    Players("Игроки"),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenGame: (Long) -> Unit,
    onOpenPlayer: (Long) -> Unit,
    onResumeGame: (Long) -> Unit,
) {
    val vm: StatisticsViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    var tab by remember { mutableStateOf(StatsTab.Games) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
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
            Column(modifier = Modifier.fillMaxSize()) {
                TabsRow(selected = tab, onSelect = { tab = it })
                when (tab) {
                    StatsTab.Games -> GamesList(
                        games = state.games,
                        onOpen = onOpenGame,
                        onResume = onResumeGame,
                    )
                    StatsTab.Players -> PlayersList(state.players, onOpen = onOpenPlayer)
                }
            }
        }
    }
}

@Composable
private fun TabsRow(selected: StatsTab, onSelect: (StatsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsTab.entries.forEach { t ->
            val isSelected = t == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Color.Black else Color.White,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(t) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.label,
                    color = if (isSelected) Color.White else Color.Black,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun GamesList(
    games: List<GameWithWinner>,
    onOpen: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    if (games.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пока ни одной игры", color = Color(0xFF666666), fontSize = 16.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(games, key = { it.id }) { g -> GameCard(g, onOpen, onResume) }
    }
}

@Composable
private fun GameCard(
    g: GameWithWinner,
    onOpen: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    val isPaused = g.status == GameStatus.PAUSED.name
    val isFinished = g.status == GameStatus.FINISHED.name
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable {
                when {
                    isPaused -> onResume(g.id)
                    isFinished -> onOpen(g.id)
                }
            }
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Игра #${g.id}",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            StatusBadge(status = g.status)
        }
        val subtitle = when {
            isFinished && g.winnerName != null -> "Победил: ${g.winnerName}"
            isFinished -> "Завершена"
            isPaused -> "На паузе"
            else -> "В процессе"
        }
        Text(subtitle, color = Color(0xFF666666), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Text(
            "Лимит ${g.thresholdScore}",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        GameStatus.PAUSED.name -> Color(0xFFE8E8E8) to Color.Black
        GameStatus.FINISHED.name -> Color(0xFFCCCCCC) to Color.Black
        else -> Color.Black to Color.White
    }
    Text(
        when (status) {
            GameStatus.PAUSED.name -> "пауза"
            GameStatus.FINISHED.name -> "финал"
            else -> "идёт"
        },
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun PlayersList(players: List<PlayerStatsRow>, onOpen: (Long) -> Unit) {
    if (players.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Нет ни одного игрока", color = Color(0xFF666666), fontSize = 16.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(players, key = { it.playerId }) { p ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .clickable { onOpen(p.playerId) }
                    .padding(16.dp),
            ) {
                Text(
                    p.name,
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Побед: ${p.wins} · Игр: ${p.games}",
                    color = Color(0xFF666666),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}