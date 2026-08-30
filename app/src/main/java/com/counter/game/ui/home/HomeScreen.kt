package com.counter.game.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

@Composable
fun HomeScreen(
    container: AppContainer,
    onContinue: (Long) -> Unit,
    onNewGame: () -> Unit,
    onPlayers: () -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    val pendingResumeId = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }
    androidx.compose.runtime.LaunchedEffect(pendingResumeId.value) {
        pendingResumeId.value?.let { id ->
            onContinue(id)
            pendingResumeId.value = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "101 Counter",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer24()

            HomeButton(
                label = "Продолжить игру",
                enabled = state.hasPausedGame,
                onClick = { vm.requestResume { id -> pendingResumeId.value = id } },
            )
            HomeButton("Новая игра", onClick = onNewGame)
            HomeButton("Игроки", onClick = onPlayers)
            HomeButton("Статистика", onClick = onStatistics)
            HomeButton("Настройки", onClick = onSettings)
        }
    }
}

@Composable
private fun HomeButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFBDBDBD),
            disabledContentColor = Color.White,
        ),
    ) {
        Text(label)
    }
}

@Composable
private fun Spacer24() = androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))