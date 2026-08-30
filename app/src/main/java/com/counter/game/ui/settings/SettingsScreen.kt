package com.counter.game.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IntField("Номинал дамы", state.queenValue, vm::updateQueen)
                IntField("Номинал дамы пик", state.queenSpadesValue, vm::updateQueenSpades)
                IntField("Номинал короля", state.kingValue, vm::updateKing)
                IntField("Лимит проигрыша", state.thresholdScore, vm::updateThreshold)
                FontScaleRow(state.fontScale, vm::updateFontScale)

                Button(
                    onClick = {
                        vm.save()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@Composable
private fun IntField(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, color = Color.Black)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { txt -> txt.toIntOrNull()?.let(onChange) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FontScaleRow(scale: Float, onChange: (Float) -> Unit) {
    Column {
        Text("Размер шрифта: ${"%.2f".format(scale)}×", color = Color.Black)
        Slider(value = scale, onValueChange = onChange, valueRange = 0.85f..1.5f, steps = 13)
    }
}