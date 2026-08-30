package com.counter.game.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    container: AppContainer,
    gameId: Long,
    onBack: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(gameId) { vm.load(gameId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История") },
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
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Левая фиксированная колонка.
                    LazyColumn(
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, Color.Black)
                            .sizeIn(minWidth = 120.dp)
                            .fillMaxSize(),
                    ) {
                        item { HeaderCell("Игрок", isHeader = true) }
                        items(state.players, key = { it.id }) { p ->
                            HeaderCell(p.displayName + "  " + p.totalScore, isHeader = false)
                        }
                    }
                    // Правая прокручиваемая область с колонками раундов.
                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(start = 4.dp),
                    ) {
                        items(state.rounds.size) { roundIdx ->
                            Column(modifier = Modifier.border(1.dp, Color.Black).padding(4.dp)) {
                                HeaderCell("R${roundIdx + 1}", isHeader = true)
                                state.players.forEach { p ->
                                    val v = state.rounds[roundIdx][state.players.indexOf(p)]
                                    val text = v?.let { if (it > 0) "+$it" else it.toString() } ?: "—"
                                    Box(
                                        modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 40.dp).padding(4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(text, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    "Всего раундов: ${state.rounds.size}",
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, isHeader: Boolean) {
    Box(
        modifier = Modifier
            .sizeIn(minHeight = 40.dp)
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = Color.Black,
            style = if (isHeader) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
        )
    }
}