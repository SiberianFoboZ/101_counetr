package com.counter.game.ui.round

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.data.dao.GamePlayerWithScore
import com.counter.game.ui.viewModelFactory

private const val KEY_SAVED_ROUND = "saved_round_number"

/**
 * Структура строки в редакторе карт. Группа — базовые карты, дамы и короли
 * визуально идут отдельными строками (с разной логикой счёта).
 */
private sealed interface CardGroup {
    val label: String
    val codes: List<String>

    data class Basic(
        override val codes: List<String>,
    ) : CardGroup {
        override val label: String = "Карты на руках"
    }

    data class QueensNonSpades(
        override val codes: List<String>,
    ) : CardGroup {
        override val label: String = "Дама (♥ ♦ ♣)"
    }

    data class QueensSpades(
        override val codes: List<String>,
    ) : CardGroup {
        override val label: String = "Дама ♠"
    }

    data class KingsNonSpades(
        override val codes: List<String>,
    ) : CardGroup {
        override val label: String = "Король (♥ ♦ ♣)"
    }

    data class KingsSpades(
        override val codes: List<String>,
    ) : CardGroup {
        override val label: String = "Король ♠"
    }
}

private val CardGroups: List<CardGroup> = listOf(
    CardGroup.Basic(codes = listOf("6", "7", "8", "9", "10", "J", "A")),
    CardGroup.QueensNonSpades(codes = listOf("Q_hearts", "Q_diamonds", "Q_clubs")),
    CardGroup.QueensSpades(codes = listOf("Q_spades")),
    CardGroup.KingsNonSpades(codes = listOf("K_hearts", "K_diamonds", "K_clubs")),
    CardGroup.KingsSpades(codes = listOf("K_spades")),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RoundInputScreen(
    container: AppContainer,
    gameId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    navBackStackEntry: androidx.navigation.NavBackStackEntry? = null,
) {
    val vm: RoundInputViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var expandedPlayerId by remember { mutableStateOf<Long?>(null) }
    // По умолчанию блок «Кто выиграл раунд?» развёрнут, чтобы пользователь сразу
    // мог выбрать победителя. После выбора — сворачивается в компактную строку,
    // освобождая максимум места для карт проигравших.
    var winnerSectionExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(gameId) { vm.load(gameId) }
    // Сразу после успешного сохранения раунда: кладём номер в savedStateHandle
    // предыдущего backstack entry (GameScreen покажет snackbar у себя) и вызываем
    // onSaved() — навигация назад не должна ждать авто-скрытия snackbar (~4 сек).
    LaunchedEffect(state.savedRoundNumber) {
        val n = state.savedRoundNumber ?: return@LaunchedEffect
        navBackStackEntry?.savedStateHandle?.set(KEY_SAVED_ROUND, n)
        vm.consumeSaved()
        onSaved()
    }
    LaunchedEffect(state.limitMessage) {
        state.limitMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.consumeLimitMessage()
        }
    }

    val aliveLosers = state.players.filter { it.id != state.winnerGamePlayerId }
    val orderedLosers = if (expandedPlayerId == null) {
        aliveLosers
    } else {
        val expanded = aliveLosers.firstOrNull { it.id == expandedPlayerId }
        if (expanded == null) aliveLosers
        else listOf(expanded) + aliveLosers.filter { it.id != expandedPlayerId }
    }
    val hasWinner = state.winnerGamePlayerId != null
    val hasAnyCards = when (state.inputMode) {
        InputMode.CARDS -> state.handCounts.values.any { it.isNotEmpty() }
        InputMode.MANUAL -> state.manualDelta.values.any { it != 0 }
    }
    val canSave = !state.isSaving && (hasWinner || hasAnyCards)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый раунд") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                WinnerSelectionSection(
                    players = state.players,
                    selectedId = state.winnerGamePlayerId,
                    winnerDelta = state.winnerDelta,
                    expanded = winnerSectionExpanded,
                    onToggleExpand = { winnerSectionExpanded = !winnerSectionExpanded },
                    onSelectWinner = vm::selectWinner,
                    onSelectDelta = vm::setWinnerDelta,
                )

                SectionHeader(
                    stepNumber = 2,
                    title = "Карты у проигравших",
                )

                InputModeSwitcher(
                    mode = state.inputMode,
                    onChange = vm::setInputMode,
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                ) {
                    when (state.inputMode) {
                        InputMode.CARDS -> {
                            items(orderedLosers, key = { it.id }) { p ->
                                PlayerCard(
                                    player = p,
                                    counts = state.handCounts[p.id].orEmpty(),
                                    cardDefs = state.cardDefs,
                                    expanded = expandedPlayerId == p.id,
                                    onToggleExpand = {
                                        expandedPlayerId = if (expandedPlayerId == p.id) null else p.id
                                    },
                                    onAdd = { code -> vm.tryAdd(p.id, code) },
                                    onRemove = { code -> vm.tryRemove(p.id, code) },
                                )
                            }
                        }
                        InputMode.MANUAL -> {
                            items(orderedLosers, key = { it.id }) { p ->
                                ManualDeltaRow(
                                    player = p,
                                    value = state.manualDelta[p.id] ?: 0,
                                    onChange = { newValue -> vm.setManualDelta(p.id, newValue) },
                                    onIncrement = { vm.changeManualDelta(p.id, +1) },
                                    onDecrement = { vm.changeManualDelta(p.id, -1) },
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Button(
                        onClick = vm::save,
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (state.isSaving) "Сохранение…" else "Сохранить раунд",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(stepNumber: Int, title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stepNumber.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.size(4.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 38.dp),
            )
        }
    }
}

/**
 * Сворачиваемый блок выбора победителя и его бонуса.
 *
 * Развёрнут по умолчанию — пользователь сразу видит сетку игроков.
 * После выбора победителя схлопывается в одну строку «Победитель: X · бонус Y»,
 * максимально освобождая место для карт проигравших. По тапу — раскрывается
 * обратно, чтобы можно было поменять победителя или скорректировать бонус.
 */
@Composable
private fun WinnerSelectionSection(
    players: List<GamePlayerWithScore>,
    selectedId: Long?,
    winnerDelta: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectWinner: (Long) -> Unit,
    onSelectDelta: (Int) -> Unit,
) {
    val selectedPlayer = players.firstOrNull { it.id == selectedId }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // Заголовок-шеврон: либо приглашение выбрать, либо компактная сводка.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "1",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(10.dp))
            if (selectedPlayer == null) {
                Text(
                    "Кто выиграл раунд?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Победитель",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Text(
                        selectedPlayer.displayName + " · " + if (winnerDelta == 0) "бонус 0" else "бонус $winnerDelta",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                WinnerGrid(
                    players = players,
                    selectedId = selectedId,
                    onSelect = onSelectWinner,
                )

                if (selectedId != null) {
                    WinnerDeltaRow(
                        current = winnerDelta,
                        onSelect = onSelectDelta,
                    )
                }
            }
        }
    }
}

@Composable
private fun WinnerGrid(
    players: List<GamePlayerWithScore>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    val rows = players.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPair.forEach { p ->
                    WinnerCard(
                        player = p,
                        isSelected = selectedId == p.id,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(p.id) },
                    )
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WinnerDeltaRow(
    current: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(0, -20, -40, -50)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Бонус победителю (по умолчанию 0):",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { v ->
                val isSelected = v == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onSelect(v) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (v == 0) "0" else v.toString(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

    }
}

@Composable
private fun WinnerCard(
    player: GamePlayerWithScore,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            player.displayName,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
        Text(
            player.totalScore.toString(),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PlayerCard(
    player: GamePlayerWithScore,
    counts: Map<String, Int>,
    cardDefs: Map<String, com.counter.game.data.entity.CardDefinitionEntity>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val totalCards = counts.values.sum()
    val previewDelta = previewDelta(counts, cardDefs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (expanded) 2.dp else 1.dp,
                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.displayName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Карт: $totalCards" + if (previewDelta != 0) " · итого ${if (previewDelta > 0) "+" else ""}$previewDelta" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CardGroups.forEach { group ->
                    if (group is CardGroup.Basic) {
                        BasicCardsGrid(
                            codes = group.codes,
                            counts = counts,
                            cardDefs = cardDefs,
                            onAdd = onAdd,
                            onRemove = onRemove,
                        )
                    } else {
                        FaceCardRow(
                            label = group.label,
                            codes = group.codes,
                            counts = counts,
                            cardDefs = cardDefs,
                            onAdd = onAdd,
                            onRemove = onRemove,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BasicCardsGrid(
    codes: List<String>,
    counts: Map<String, Int>,
    cardDefs: Map<String, com.counter.game.data.entity.CardDefinitionEntity>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Карты на руках",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        // Две строки по 4 и 3 чипа, чтобы влезало на телефон без переноса.
        val chunks = codes.chunked(4)
        chunks.forEachIndexed { idx, chunk ->
            if (idx > 0) Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chunk.forEach { code ->
                    val value = counts[code] ?: 0
                    val label = cardDefs[code]?.label ?: code
                    CounterChip(
                        modifier = Modifier.weight(1f),
                        label = label,
                        value = value,
                        onIncrement = { onAdd(code) },
                        onDecrement = { onRemove(code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FaceCardRow(
    label: String,
    codes: List<String>,
    counts: Map<String, Int>,
    cardDefs: Map<String, com.counter.game.data.entity.CardDefinitionEntity>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val sum = codes.sumOf { counts[it] ?: 0 }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        // Уменьшаем «последний» ненулевой код, чтобы не дёргать рандом.
                        val victim = codes.lastOrNull { (counts[it] ?: 0) > 0 }
                        if (victim != null) onRemove(victim)
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Убрать", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    sum.toString(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                IconButton(
                    onClick = {
                        // Добавляем в первый код группы — для дам/королей не-пик это Q_hearts.
                        onAdd(codes.first())
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            val preview = previewDelta(
                codes.associateWith { counts[it] ?: 0 },
                cardDefs,
            )
            Text(
                if (preview == 0) "0" else "${if (preview > 0) "+" else ""}$preview",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CounterChip(
    label: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                if (value > 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrement,
            enabled = value > 0,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = null,
                tint = if (value > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                value.toString(),
                color = if (value > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/**
 * Лёгкий preview дельты без движка правил. Считает базовые номиналы × count,
 * для дам и королей — единое значение из настроек (если они non-spades) или
 * queen_spades_value / king_value (если spades). Это приблизительная цифра;
 * точная появится после saveRound.
 */
private fun previewDelta(
    counts: Map<String, Int>,
    cardDefs: Map<String, com.counter.game.data.entity.CardDefinitionEntity>,
): Int {
    if (counts.isEmpty()) return 0
    var total = 0
    counts.forEach { (code, count) ->
        if (count <= 0) return@forEach
        val def = cardDefs[code] ?: return@forEach
        when {
            code == "Q_spades" -> total += def.baseValue * count
            code.startsWith("Q_") -> total += def.baseValue * count
            code == "K_spades" -> total += def.baseValue * count
            code.startsWith("K_") -> total += def.baseValue * count
            else -> total += def.baseValue * count
        }
    }
    return total
}

/**
 * Сегментированный переключатель «Карты / Вручную» над списком проигравших.
 *
 * При смене режима [RoundInputViewModel.setInputMode] сбрасывает уже введённые данные
 * (карты или числа), чтобы не смешивать две независимые «корзины» в одном раунде.
 * Этот композабл только переключает — никаких данных здесь не хранится.
 */
@Composable
private fun InputModeSwitcher(
    mode: InputMode,
    onChange: (InputMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = mode == InputMode.CARDS,
                onClick = { onChange(InputMode.CARDS) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Карты") }
            SegmentedButton(
                selected = mode == InputMode.MANUAL,
                onClick = { onChange(InputMode.MANUAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Вручную") }
        }

    }
}

/**
 * Строка проигравшего в ручном режиме: имя, числовое поле и кнопки «+/−».
 *
 * Поле принимает произвольные целые (включая отрицательные — например, штраф −5).
 * Под полем — строка-превью «итого очков» с подписью; для 0 показывается «0».
 * При изменении значения через «+/−» фокус с текстового поля не сбрасывается —
 * каждый чип имеет собственный click-handler.
 */
@Composable
private fun ManualDeltaRow(
    player: GamePlayerWithScore,
    value: Int,
    onChange: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    var draft by rememberSaveable(player.id) { mutableStateOf(value.toString()) }
    // Синхронизируем локальный черновик с состоянием VM, если оно изменилось
    // снаружи (например, после save() или переключения режима).
    LaunchedEffect(value) { draft = value.toString() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                player.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "текущий счёт: ${player.totalScore}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Уменьшить",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { newText ->
                    // Разрешаем только то, что может распарситься как Int (включая «-»).
                    val sanitized = newText.filter { it.isDigit() || it == '-' }
                    draft = sanitized
                    val parsed = sanitized.toIntOrNull()
                    if (parsed != null) onChange(parsed)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Итого очков") },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Увеличить",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            "Дельта: " + if (value > 0) "+$value" else value.toString(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}