package com.counter.game.ui.rules

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.data.entity.RuleEntity
import com.counter.game.engine.RuleSerializer
import com.counter.game.ui.common.ConfirmDialog
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onEditRule: (Long?) -> Unit,
) {
    val vm: RulesViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Правила подсчёта") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onEditRule(null) },
                containerColor = Color.Black,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Новое правило")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (state.rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Правил пока нет", color = Color(0xFF666666))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.rules, key = { it.id }) { rule ->
                        RuleListCard(
                            rule = rule,
                            cardLabel = state.cardLabels[rule.appliesToCard] ?: rule.appliesToCard,
                            onClick = { onEditRule(rule.id) },
                            onMoveUp = { vm.movePriority(rule, +1) },
                            onMoveDown = { vm.movePriority(rule, -1) },
                            onToggle = { vm.toggleEnabled(rule) },
                            onDelete = { vm.requestDelete(rule) },
                        )
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { rule ->
        ConfirmDialog(
            title = "Удалить правило",
            text = "Правило «${rule.name}» будет удалено без возможности восстановления.",
            confirmLabel = "Удалить",
            onConfirm = vm::confirmDelete,
            onDismiss = vm::cancelDelete,
        )
    }
}

@Composable
private fun RuleListCard(
    rule: RuleEntity,
    cardLabel: String,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val summary = remember(rule.definitionJson) { ruleSummary(rule, cardLabel) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                rule.name,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "приоритет ${rule.priority}",
                color = Color(0xFF666666),
                fontSize = 12.sp,
            )
        }
        Text(
            summary,
            color = Color(0xFF666666),
            fontSize = 13.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                IconButton(onClick = onMoveUp, modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Повысить приоритет", tint = Color.Black)
                }
                IconButton(onClick = onMoveDown, modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Понизить приоритет", tint = Color.Black)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Вкл", color = Color.Black, fontSize = 14.sp)
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
                Button(
                    onClick = onDelete,
                    modifier = Modifier.sizeIn(minHeight = 36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) { Text("Удалить") }
            }
        }
    }
}

private fun ruleSummary(rule: RuleEntity, cardLabel: String): String {
    val form = RuleSerializer.decode(rule.definitionJson) ?: return "${cardLabel} · (правило не парсится)"
    val parts = mutableListOf<String>()
    parts += cardLabel

    when (val n = form.nominal) {
        RuleSerializer.NominalForm.AnyNominal -> Unit
        is RuleSerializer.NominalForm.Eq -> parts += "номинал = ${n.value}"
        is RuleSerializer.NominalForm.Ne -> parts += "номинал ≠ ${n.value}"
        is RuleSerializer.NominalForm.In -> parts += "номинал ∈ {${n.values.joinToString(",")}}"
    }
    when (form.suit) {
        RuleSerializer.SuitForm.ANY -> Unit
        else -> parts += form.suit.label()
    }
    form.condition?.let { c ->
        parts += "${c.left.short()} ${c.op.symbol()} ${c.rightValue}"
    }
    val then = describeAction(form.then)
    val els = describeAction(form.elseAction)
    parts += if (form.condition == null && then == els) {
        "→ $then"
    } else {
        "→ then $then; иначе $els"
    }
    return parts.joinToString(" · ")
}

private fun describeAction(a: RuleSerializer.ActionForm): String = when (a) {
    is RuleSerializer.ActionForm.Const -> a.value.toString()
    RuleSerializer.ActionForm.BaseValue -> "base_value × count"
    is RuleSerializer.ActionForm.Setting -> a.key
    RuleSerializer.ActionForm.SubtractTotal -> "−total"
}

private fun RuleSerializer.SuitForm.label(): String = when (this) {
    RuleSerializer.SuitForm.ANY -> "любая"
    RuleSerializer.SuitForm.SPADES -> "♠"
    RuleSerializer.SuitForm.HEARTS -> "♥"
    RuleSerializer.SuitForm.DIAMONDS -> "♦"
    RuleSerializer.SuitForm.CLUBS -> "♣"
    RuleSerializer.SuitForm.RED -> "красные"
    RuleSerializer.SuitForm.BLACK -> "чёрные"
    RuleSerializer.SuitForm.NON_SPADES -> "не пики"
}

private fun RuleSerializer.OperandForm.short(): String = when (this) {
    RuleSerializer.OperandForm.CARD_COUNT -> "card_count"
    RuleSerializer.OperandForm.ROUND_DELTA_SO_FAR -> "round_delta_so_far"
    RuleSerializer.OperandForm.DISTINCT_CARD_CODES -> "distinct_card_codes"
    RuleSerializer.OperandForm.TOTAL_SCORE -> "total_score"
}

private fun RuleSerializer.OpForm.symbol(): String = when (this) {
    RuleSerializer.OpForm.EQ -> "=="
    RuleSerializer.OpForm.NE -> "!="
    RuleSerializer.OpForm.LT -> "<"
    RuleSerializer.OpForm.LE -> "<="
    RuleSerializer.OpForm.GT -> ">"
    RuleSerializer.OpForm.GE -> ">="
}