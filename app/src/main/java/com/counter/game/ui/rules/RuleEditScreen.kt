package com.counter.game.ui.rules

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.counter.game.ui.common.QuickClearTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.data.entity.CardDefinitionEntity
import com.counter.game.engine.RuleContext
import com.counter.game.engine.RuleDefinition
import com.counter.game.engine.RuleEngine
import com.counter.game.engine.RuleSerializer

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(
    container: AppContainer,
    ruleId: Long?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val factory = remember(ruleId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RuleEditViewModel(container, ruleId) as T
        }
    }
    val vm: RuleEditViewModel = viewModel(factory = factory, key = "rule_edit_${ruleId ?: -1}")
    val state by vm.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.form.isExisting) "Редактировать правило" else "Новое правило") },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard(title = "Основное") {
                    QuickClearTextField(
                        value = state.form.name,
                        onValueChange = vm::setName,
                        label = { Text("Название правила") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Top-level выбор типа правила.
                    val currentKind = state.form.form.kind
                    Column {
                        Text("Тип правила", color = Color(0xFF666666), fontSize = 13.sp)
                        Spacer(Modifier.size(4.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val kinds = RuleSerializer.Kind.entries
                            kinds.forEachIndexed { idx, k ->
                                SegmentedButton(
                                    selected = currentKind == k,
                                    onClick = { vm.setKind(k) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, kinds.size),
                                ) { Text(k.shortLabel(), fontSize = 13.sp) }
                            }
                        }
                        Text(
                            currentKind.description(),
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (currentKind == RuleSerializer.Kind.PER_CARD) {
                        CardPickerField(
                            cards = state.cards,
                            selectedCode = state.form.appliesToCard,
                            onSelected = vm::setAppliesToCard,
                        )
                    }
                }

                if (state.form.form.kind == RuleSerializer.Kind.PER_CARD) {
                    SectionCard(title = "Условие совпадения (match)") {
                        NominalSection(state, vm)
                        SuitSection(state, vm)
                    }
                }

                SectionCard(title = "Условие срабатывания") {
                    ConditionSection(state, vm)
                }

                SectionCard(title = "Действия") {
                    if (state.form.form.kind == RuleSerializer.Kind.PER_CARD) {
                        ActionSection(
                            label = "Позитивное действие (then)",
                            current = state.form.form.then,
                            onChange = vm::setThenAction,
                        )
                        ActionSection(
                            label = "Негативное действие (else)",
                            current = state.form.form.elseAction,
                            onChange = vm::setElseAction,
                        )
                        ConditionNullWarning(state.form.form)
                    } else {
                        FinalAdjustmentActionSection(
                            then = state.form.form.then,
                            elseAction = state.form.form.elseAction,
                            onSet = { then, els -> vm.setThenAction(then); vm.setElseAction(els) },
                        )
                    }
                }

                // Превью: запускаем движок на примере руки.
                PreviewCard(state = state)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = vm::save,
                        enabled = state.form.canSave,
                        modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    ) { Text("Сохранить", fontSize = 16.sp) }
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) { Text("Отмена", fontSize = 16.sp) }
                }
                if (state.form.isExisting) {
                    Button(
                        onClick = vm::requestDelete,
                        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) { Text("Удалить") }
                }
                Spacer(Modifier.size(16.dp))
            }
        }
    }

    if (state.confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("Удалить правило") },
            text = { Text("Действие необратимо.") },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelDelete) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun CardPickerField(
    cards: List<CardDefinitionEntity>,
    selectedCode: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = cards.firstOrNull { it.code == selectedCode }?.label ?: "Не выбрано"
    Column {
        Text("Применяется к карте", color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedLabel, modifier = Modifier.weight(1f), color = Color.Black, fontSize = 16.sp)
                Text("▾", color = Color.Black)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cards.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.label}  (${c.code})") },
                        onClick = {
                            onSelected(c.code)
                            expanded = false
                        },
                    )
                }
            }
        }
        val baseValue = cards.firstOrNull { it.code == selectedCode }?.baseValue
        if (baseValue != null) {
            Text(
                "base_value = $baseValue",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private enum class NominalKind(val label: String) {
    Any("Любой"),
    Eq("= N"),
    Ne("≠ N"),
    In("В наборе"),
}

@Composable
private fun NominalSection(state: RuleEditState, vm: RuleEditViewModel) {
    val current = state.form.form.nominal
    Column {
        Text("Номинал карты (match.nominal)", color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        val kind = when (current) {
            RuleSerializer.NominalForm.AnyNominal -> NominalKind.Any
            is RuleSerializer.NominalForm.Eq -> NominalKind.Eq
            is RuleSerializer.NominalForm.Ne -> NominalKind.Ne
            is RuleSerializer.NominalForm.In -> NominalKind.In
        }
        val kinds = NominalKind.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            kinds.forEachIndexed { idx, k ->
                SegmentedButton(
                    selected = kind == k,
                    onClick = {
                        val newForm: RuleSerializer.NominalForm = when (k) {
                            NominalKind.Any -> RuleSerializer.NominalForm.AnyNominal
                            NominalKind.Eq -> RuleSerializer.NominalForm.Eq(0)
                            NominalKind.Ne -> RuleSerializer.NominalForm.Ne(0)
                            NominalKind.In -> RuleSerializer.NominalForm.In(listOf(0))
                        }
                        vm.setNominal(newForm)
                    },
                    shape = SegmentedButtonDefaults.itemShape(idx, kinds.size),
                ) { Text(k.label) }
            }
        }
        when (current) {
            RuleSerializer.NominalForm.AnyNominal -> Unit
            is RuleSerializer.NominalForm.Eq -> IntRow("значение", current.value) { vm.setNominal(current.copy(value = it)) }
            is RuleSerializer.NominalForm.Ne -> IntRow("значение", current.value) { vm.setNominal(current.copy(value = it)) }
            is RuleSerializer.NominalForm.In -> {
                QuickClearTextField(
                    value = current.values.joinToString(","),
                    onValueChange = { txt ->
                        val parsed = txt.split(",").mapNotNull { it.trim().toIntOrNull() }
                        vm.setNominal(RuleSerializer.NominalForm.In(parsed))
                    },
                    label = { Text("значения через запятую") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun RuleSerializer.Kind.shortLabel(): String = when (this) {
    RuleSerializer.Kind.PER_CARD -> "По карте"
    RuleSerializer.Kind.FINAL_ADJUSTMENT -> "Итог"
}

private fun RuleSerializer.Kind.description(): String = when (this) {
    RuleSerializer.Kind.PER_CARD ->
        "Применяется к одной карте в руке игрока. Самая частая форма правила."
    RuleSerializer.Kind.FINAL_ADJUSTMENT ->
        "Применяется ко всему итогу игрока ПОСЛЕ per-card правил. " +
            "Используется для обнуления, бонусов за порог и т.п."
}

@Composable
private fun FinalAdjustmentActionSection(
    then: RuleSerializer.ActionForm,
    elseAction: RuleSerializer.ActionForm,
    onSet: (RuleSerializer.ActionForm, RuleSerializer.ActionForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Для итоговой корректировки доступно одно действие — «Вычесть итог» (delta = -totalScore).",
            color = Color(0xFF666666),
            fontSize = 13.sp,
        )
        val active = then is RuleSerializer.ActionForm.SubtractTotal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (active) Color.Black else Color.White,
                    RoundedCornerShape(8.dp),
                )
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color = if (active) Color.Black else Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable {
                    val newAction = if (active) {
                        RuleSerializer.ActionForm.Const(0)
                    } else {
                        RuleSerializer.ActionForm.SubtractTotal
                    }
                    onSet(newAction, newAction)
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Вычесть итог",
                color = if (active) Color.White else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (active) "вкл" else "выкл",
                color = if (active) Color.White else Color(0xFF666666),
                fontSize = 14.sp,
            )
        }
        Text(
            "Подсказка: условие «total_score ≥ 101 → Вычесть итог» обнуляет счёт игрока при переходе через 101.",
            color = Color(0xFF888888),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun SuitSection(state: RuleEditState, vm: RuleEditViewModel) {
    Column {
        Text("Масть (match.suit)", color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        val current = state.form.form.suit
        val card = state.cards.firstOrNull { it.code == state.form.appliesToCard }
        val suitFromCode = card?.code?.let {
            when {
                it.endsWith("_spades") -> RuleSerializer.SuitForm.SPADES
                it.endsWith("_hearts") -> RuleSerializer.SuitForm.HEARTS
                it.endsWith("_diamonds") -> RuleSerializer.SuitForm.DIAMONDS
                it.endsWith("_clubs") -> RuleSerializer.SuitForm.CLUBS
                else -> null
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RuleSerializer.SuitForm.entries.forEach { f ->
                val isSelected = current == f
                val locked = suitFromCode != null && f != suitFromCode && f != RuleSerializer.SuitForm.ANY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) Color.Black else Color.White,
                            RoundedCornerShape(4.dp),
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable(enabled = !locked) { vm.setSuit(f) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        f.label(),
                        color = if (isSelected) Color.White else if (locked) Color(0xFFBDBDBD) else Color.Black,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

private fun RuleSerializer.SuitForm.label(): String = when (this) {
    RuleSerializer.SuitForm.ANY -> "Любая"
    RuleSerializer.SuitForm.SPADES -> "♠"
    RuleSerializer.SuitForm.HEARTS -> "♥"
    RuleSerializer.SuitForm.DIAMONDS -> "♦"
    RuleSerializer.SuitForm.CLUBS -> "♣"
    RuleSerializer.SuitForm.RED -> "Красн."
    RuleSerializer.SuitForm.BLACK -> "Чёрн."
    RuleSerializer.SuitForm.NON_SPADES -> "Не пики"
}

@Composable
private fun ConditionSection(state: RuleEditState, vm: RuleEditViewModel) {
    val enabled = state.form.form.condition != null
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = enabled,
                onCheckedChange = { vm.setConditionEnabled(it) },
            )
            Text("Добавить условие (match.condition)", color = Color.Black, fontSize = 14.sp)
        }
        val cond = state.form.form.condition
        if (cond != null) {
            DropdownRow(
                label = "Левый операнд",
                value = RuleLabels.operands[cond.left]?.label ?: cond.left.name,
                options = RuleSerializer.OperandForm.entries.map {
                    val l = RuleLabels.operands[it]!!
                    it to l.label
                },
                onSelected = { vm.setConditionLeft(it) },
                description = RuleLabels.operands[cond.left]?.description ?: "",
            )
            DropdownRow(
                label = "Оператор",
                value = RuleLabels.ops[cond.op]?.label ?: cond.op.name,
                options = RuleSerializer.OpForm.entries.map { it to (RuleLabels.ops[it]?.label ?: it.name) },
                onSelected = { vm.setConditionOp(it) },
                description = "",
            )
            IntRow("Правое значение", cond.rightValue, vm::setConditionRight)
            // Сводка условия человеческим языком.
            Text(
                text = "Условие: «${RuleLabels.operands[cond.left]?.label ?: cond.left.name}» " +
                    "${RuleLabels.ops[cond.op]?.label ?: cond.op.name} ${cond.rightValue}",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEFEF), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

private fun RuleSerializer.OperandForm.label(): String = RuleLabels.operands[this]?.label ?: this.name

@Composable
private fun <T> DropdownRow(
    label: String,
    value: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit,
    description: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, modifier = Modifier.weight(1f), color = Color.Black)
                Text("▾", color = Color.Black)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (item, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = {
                        onSelected(item)
                        expanded = false
                    })
                }
            }
        }
        if (description.isNotEmpty()) {
            Text(
                description,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ActionSection(
    label: String,
    current: RuleSerializer.ActionForm,
    onChange: (RuleSerializer.ActionForm) -> Unit,
) {
    var kind by remember(current) {
        mutableStateOf(
            when (current) {
                is RuleSerializer.ActionForm.Const -> ActionKind.Const
                RuleSerializer.ActionForm.BaseValue -> ActionKind.BaseValue
                is RuleSerializer.ActionForm.Setting -> ActionKind.Setting
                RuleSerializer.ActionForm.SubtractTotal -> ActionKind.Const
            },
        )
    }
    Column {
        Text(label, color = Color(0xFF666666), fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ActionKind.entries.forEachIndexed { idx, k ->
                SegmentedButton(
                    selected = kind == k,
                    onClick = {
                        kind = k
                        onChange(defaultFor(k))
                    },
                    shape = SegmentedButtonDefaults.itemShape(idx, ActionKind.entries.size),
                ) { Text(k.label, fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.size(6.dp))
        when (val c = current) {
            is RuleSerializer.ActionForm.Const -> IntRow("значение", c.value) { onChange(c.copy(value = it)) }
            RuleSerializer.ActionForm.BaseValue -> Text(
                "base_value × count этой карты",
                color = Color(0xFF888888),
                fontSize = 13.sp,
            )
            is RuleSerializer.ActionForm.Setting -> DropdownRow(
                label = "Ключ настройки",
                value = RuleLabels.settings[c.key]?.label ?: c.key,
                options = listOf("queen_value", "queen_spades_value", "king_value", "threshold_score")
                    .map { it to (RuleLabels.settings[it]?.label ?: it) },
                onSelected = { onChange(c.copy(key = it)) },
                description = RuleLabels.settings[c.key]?.description ?: "",
            )
            RuleSerializer.ActionForm.SubtractTotal -> Text(
                "Для этого правила используется отдельный раздел «Действия».",
                color = Color(0xFF888888),
                fontSize = 13.sp,
            )
        }
    }
}

private enum class ActionKind(val label: String) {
    Const("Конст."),
    BaseValue("Баз."),
    Setting("Настр."),
}

private fun defaultFor(kind: ActionKind): RuleSerializer.ActionForm = when (kind) {
    ActionKind.Const -> RuleSerializer.ActionForm.Const(0)
    ActionKind.BaseValue -> RuleSerializer.ActionForm.BaseValue
    ActionKind.Setting -> RuleSerializer.ActionForm.Setting("queen_value")
}

@Composable
private fun IntRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickClearTextField(
            value = value.toString(),
            onValueChange = { txt ->
                val cleaned = txt.trim()
                when {
                    cleaned.isEmpty() -> Unit
                    cleaned == "-" -> Unit
                    else -> cleaned.toIntOrNull()?.let(onChange)
                }
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { onChange(-value) },
            modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text("±") }
    }
}

@Composable
private fun ConditionNullWarning(form: RuleSerializer.Form) {
    if (form.condition != null) return
    if (form.then == form.elseAction) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1), RoundedCornerShape(4.dp))
            .padding(12.dp),
    ) {
        Text(
            "Условие не задано — будет использоваться только then. Ветка else никогда не сработает.",
            color = Color(0xFF665500),
            fontSize = 13.sp,
        )
    }
}

/**
 * Превью: запускает движок на синтетическом примере руки, чтобы пользователь видел,
 * что насчитает правило без необходимости заводить раунд.
 */
@Composable
private fun PreviewCard(state: RuleEditState) {
    val json = state.form.form.let { RuleSerializer.encode(it) }
    val cardDefs = state.cards.associateBy { it.code }
    val settings = com.counter.game.data.entity.SettingsEntity()
    val engine = remember(json, state.cards) { RuleEngine() }

    // Для FINAL_ADJUSTMENT: показываем примеры на разных totalScore до раунда.
    if (state.form.form.kind == RuleSerializer.Kind.FINAL_ADJUSTMENT) {
        val rule = com.counter.game.data.entity.RuleEntity(
            id = 0,
            name = state.form.name,
            appliesToCard = "_final",
            priority = 100,
            enabled = true,
            definitionJson = json,
        )
        val totals = listOf(50, 80, 100, 101, 110, 150)
        val results = totals.map { totalBefore ->
            val ctx = RuleContext(
                settings = settings,
                cardDefs = cardDefs,
                hand = com.counter.game.engine.RoundHand(cards = emptyList()),
                totalScoreBeforeRound = totalBefore,
            )
            val delta = runCatching {
                engine.compute(listOf(rule), ctx)
            }.getOrDefault(0)
            totalBefore to delta
        }
        SectionCard(title = "Превью итоговой корректировки") {
            Text(
                "Правило применяется к итогу игрока ПОСЛЕ per-card правил. Примеры для разного totalScore до раунда:",
                color = Color(0xFF666666),
                fontSize = 13.sp,
            )
            Spacer(Modifier.size(8.dp))
            results.forEach { (total, delta) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("итог до: $total", color = Color.Black, fontSize = 15.sp)
                    Text(
                        if (delta > 0) "+$delta" else delta.toString(),
                        color = if (delta > 0) Color.Black else if (delta < 0) Color(0xFF666666) else Color(0xFF999999),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                "Условие: ${describeCondition(state.form.form.condition)}",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        return
    }

    // Для PER_CARD: прежнее превью на руке.
    val card = state.cards.firstOrNull { it.code == state.form.appliesToCard } ?: run {
        SectionCard(title = "Превью") {
            Text(
                "Выберите карту, чтобы увидеть превью правила.",
                color = Color(0xFF666666),
                fontSize = 13.sp,
            )
        }
        return
    }
    val rule = com.counter.game.data.entity.RuleEntity(
        id = 0,
        name = state.form.name,
        appliesToCard = state.form.appliesToCard,
        priority = 100,
        enabled = true,
        definitionJson = json,
    )
    val counts = listOf(1, 2, 3, 4)
    val results = counts.map { count ->
        val hand = com.counter.game.engine.RoundHand(
            cards = listOf(
                com.counter.game.data.entity.RoundCardEntity(
                    roundEntryId = 0L,
                    cardCode = state.form.appliesToCard,
                    count = count,
                ),
            ),
        )
        val delta = runCatching {
            engine.compute(listOf(rule), RuleContext(settings = settings, cardDefs = cardDefs, hand = hand))
        }.getOrDefault(0)
        count to delta
    }

    SectionCard(title = "Превью: рука только из ${card.label}") {
        Text(
            "Правило применяется к одной карте «${card.label}». Сколько очков начислит при разном количестве на руках:",
            color = Color(0xFF666666),
            fontSize = 13.sp,
        )
        Spacer(Modifier.size(8.dp))
        results.forEach { (count, delta) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$count × ${card.label}",
                    color = Color.Black,
                    fontSize = 15.sp,
                )
                Text(
                    if (delta > 0) "+$delta" else delta.toString(),
                    color = if (delta > 0) Color.Black else if (delta < 0) Color(0xFF666666) else Color(0xFF999999),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            "Условие: ${describeCondition(state.form.form.condition)}",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun describeCondition(c: RuleSerializer.ConditionForm?): String {
    if (c == null) return "без условия"
    val op = RuleLabels.ops[c.op]?.label ?: c.op.name
    val left = RuleLabels.operands[c.left]?.label ?: c.left.name
    return "$left $op ${c.rightValue}"
}

private fun describeActionShort(a: RuleSerializer.ActionForm): String = when (a) {
    is RuleSerializer.ActionForm.Const -> a.value.toString()
    RuleSerializer.ActionForm.BaseValue -> "base×count"
    is RuleSerializer.ActionForm.Setting -> a.key
    RuleSerializer.ActionForm.SubtractTotal -> "−total"
}