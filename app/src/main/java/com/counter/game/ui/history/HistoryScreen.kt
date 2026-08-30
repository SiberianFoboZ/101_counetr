package com.counter.game.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

private val CardLabels: Map<String, String> = mapOf(
    "6" to "6", "7" to "7", "8" to "8", "9" to "9", "10" to "10",
    "J" to "Валет", "A" to "Туз",
    "Q_hearts" to "Д♥", "Q_diamonds" to "Д♦", "Q_clubs" to "Д♣", "Q_spades" to "Д♠",
    "K_hearts" to "К♥", "K_diamonds" to "К♦", "K_clubs" to "К♣", "K_spades" to "К♠",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    container: AppContainer,
    gameId: Long,
    onBack: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()

    LaunchedEffect(gameId) { vm.load(gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История") },
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
            if (state.rounds.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.rounds, key = { it.roundId }) { round ->
                        RoundCard(round = round)
                    }
                    item {
                        TotalsFooter(state = state)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            "Пока ни одного раунда",
            color = Color(0xFF666666),
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun RoundCard(round: RoundSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp)),
    ) {
        // Заголовок: номер раунда + победитель.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        round.roundNumber.toString(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    "Раунд ${round.roundNumber}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                )
            }
            Text(
                if (round.winnerName != null) "победил: ${round.winnerName}" else "—",
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )
        }

        // Таблица: игрок — дельта.
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            round.entries.forEach { entry ->
                PlayerRow(entry = entry)
            }
        }
    }
}

@Composable
private fun PlayerRow(entry: RoundEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Имя + карты под именем.
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.playerName,
                    color = if (entry.isWinner) Color(0xFF666666) else Color.Black,
                    fontSize = 16.sp,
                    fontWeight = if (entry.isWinner) FontWeight.Medium else FontWeight.SemiBold,
                )
                if (entry.isWinner) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "победил",
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (entry.cards.isNotEmpty()) {
                Text(
                    text = formatCards(entry.cards),
                    color = Color(0xFF888888),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // Дельта справа — крупно.
        Text(
            formatDelta(entry.delta),
            color = if (entry.delta > 0) Color.Black else if (entry.delta < 0) Color(0xFF666666) else Color(0xFF999999),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.sizeIn(minWidth = 56.dp),
        )
    }
}

@Composable
private fun TotalsFooter(state: HistoryState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            "Итого после ${state.rounds.size} раунда(ов):",
            color = Color(0xFF666666),
            fontSize = 13.sp,
        )
        Spacer(Modifier.size(8.dp))
        state.players.forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.displayName, color = Color.Black, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    p.totalScore.toString(),
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatDelta(d: Int): String = when {
    d > 0 -> "+$d"
    d == 0 -> "0"
    else -> d.toString()
}

private fun formatCards(cards: Map<String, Int>): String {
    if (cards.isEmpty()) return ""
    return cards.entries
        .sortedBy { it.key }
        .joinToString(", ") { (code, count) ->
            val label = CardLabels[code] ?: code
            if (count == 1) label else "$label × $count"
        }
}