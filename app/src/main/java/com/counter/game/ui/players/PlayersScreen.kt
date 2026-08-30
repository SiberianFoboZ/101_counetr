package com.counter.game.ui.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.common.ConfirmDialog
import com.counter.game.ui.common.LongPressTextRow
import com.counter.game.ui.common.RenameDialog
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val vm: PlayersViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Игроки") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::add,
                containerColor = Color.Black,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Игрок")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(state.items, key = { it.id }) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LongPressTextRow(
                            text = player.name,
                            onLongPress = { vm.startRename(player) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.startArchive(player) }) {
                            Icon(Icons.Default.Archive, contentDescription = "Архивировать", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }

    state.renaming?.let { player ->
        RenameDialog(
            title = "Переименовать игрока",
            initial = player.name,
            onDismiss = vm::cancelRename,
            onConfirm = vm::confirmRename,
        )
    }
    state.archiving?.let { player ->
        ConfirmDialog(
            title = "Архивировать игрока",
            text = "Игрок «${player.name}» будет скрыт из справочника, история игр сохранится.",
            onConfirm = vm::confirmArchive,
            onDismiss = vm::cancelArchive,
        )
    }
}