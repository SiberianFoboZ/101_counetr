package com.counter.game.ui.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.counter.game.AppContainer
import com.counter.game.ui.theme.ThemePalette
import com.counter.game.ui.theme.ThemePreset
import com.counter.game.ui.theme.ThemeToken
import com.counter.game.ui.theme.parseHexColor
import com.counter.game.ui.theme.presetColors
import com.counter.game.ui.viewModelFactory

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val vm: ThemeViewModel = viewModel(factory = viewModelFactory(container))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тема") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            // Один общий LazyColumn: пресеты, превью и (если CUSTOM) токены
            // скроллятся единым списком. Это решает проблему «экран не скроллится
            // в режиме CUSTOM» — раньше шапка с пресетами и LazyColumn с токенами
            // были отдельными контейнерами, и при переполнении токены уезжали за экран.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionHeader("Пресеты")
                    Spacer(Modifier.size(8.dp))
                    PresetGrid(selected = state.preset, onSelect = vm::selectPreset)
                }
                item {
                    Spacer(Modifier.size(4.dp))
                    SectionHeader("Предпросмотр")
                    Spacer(Modifier.size(8.dp))
                    ThemePreview(palette = ThemePalette(state.preset, state.tokens))
                }
                if (state.preset == ThemePreset.CUSTOM) {
                    item {
                        Spacer(Modifier.size(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeader("Тонкая настройка")
                            Button(
                                onClick = vm::resetCustomToDefaults,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            ) { Text("Сброс") }
                        }
                    }
                    items(ThemeToken.entries.toList()) { token ->
                        TokenEditor(
                            token = token,
                            color = state.tokens[token] ?: Color.Black,
                            onChange = { vm.updateToken(token, it) },
                        )
                    }
                } else {
                    item {
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = { vm.selectPreset(ThemePreset.CUSTOM) },
                            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) { Text("Создать свою") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun PresetGrid(selected: ThemePreset, onSelect: (ThemePreset) -> Unit) {
    val presets = ThemePreset.entries.filter { it != ThemePreset.CUSTOM }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        isSelected = preset == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(preset) },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = presetColors(preset)
    val bg = palette[ThemeToken.BACKGROUND] ?: Color.White
    val fg = palette[ThemeToken.TEXT] ?: Color.Black
    val outline = palette[ThemeToken.OUTLINE] ?: Color.Gray
    Column(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else outline,
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(bg, RoundedCornerShape(4.dp))
                .border(1.dp, outline, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("A a", color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.size(6.dp))
        Text(
            preset.displayName,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ThemePreview(palette: ThemePalette) {
    val bg = palette.color(ThemeToken.BACKGROUND)
    val fg = palette.color(ThemeToken.TEXT)
    val outline = palette.color(ThemeToken.OUTLINE)
    val surface = palette.color(ThemeToken.SURFACE)
    val onSurface = palette.color(ThemeToken.ON_SURFACE)
    val surfaceVariant = palette.color(ThemeToken.SURFACE_VARIANT)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(1.dp, outline, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceVariant, RoundedCornerShape(6.dp))
                .padding(8.dp),
        ) {
            Text("Заголовок секции", color = onSurface, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.size(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(fg, CircleShape),
            )
            Spacer(Modifier.size(8.dp))
            Text("Обычный текст на фоне", color = fg)
        }
        Spacer(Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(fg, RoundedCornerShape(6.dp))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Кнопка", color = bg)
        }
    }
}

/**
 * Редактор одного токена: превью цвета, три слайдера R/G/B и hex-поле.
 * Слайдеры позволяют плавно подобрать оттенок, hex — точно ввести значение.
 */
@Composable
private fun TokenEditor(
    token: ThemeToken,
    color: Color,
    onChange: (Color) -> Unit,
) {
    val argb = color.toArgb()
    var r by remember(color) { mutableStateOf(((argb shr 16) and 0xFF).toFloat()) }
    var g by remember(color) { mutableStateOf(((argb shr 8) and 0xFF).toFloat()) }
    var b by remember(color) { mutableStateOf((argb and 0xFF).toFloat()) }
    var hexInput by remember(color) { mutableStateOf(colorToHexInput(color)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                tokenLabel(token),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        ChannelSlider("R", r) { newR ->
            r = newR
            onChange(Color(red = newR / 255f, green = g / 255f, blue = b / 255f, alpha = 1f))
        }
        ChannelSlider("G", g) { newG ->
            g = newG
            onChange(Color(red = r / 255f, green = newG / 255f, blue = b / 255f, alpha = 1f))
        }
        ChannelSlider("B", b) { newB ->
            b = newB
            onChange(Color(red = r / 255f, green = g / 255f, blue = newB / 255f, alpha = 1f))
        }
        OutlinedTextField(
            value = hexInput,
            onValueChange = { txt ->
                hexInput = txt
                val parsed = parseHexColor(txt, color)
                if (parsed != color) {
                    val p = parsed.toArgb()
                    r = ((p shr 16) and 0xFF).toFloat()
                    g = ((p shr 8) and 0xFF).toFloat()
                    b = (p and 0xFF).toFloat()
                    onChange(parsed)
                }
            },
            singleLine = true,
            label = { Text("HEX #RRGGBB") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChannelSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.sizeIn(minWidth = 18.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            steps = 255,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Text(
            value.toInt().toString().padStart(3, ' '),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.sizeIn(minWidth = 28.dp),
        )
    }
}

private fun colorToHexInput(c: Color): String {
    val argb = c.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

private fun tokenLabel(token: ThemeToken): String = when (token) {
    ThemeToken.TEXT -> "Текст (основной цвет)"
    ThemeToken.BACKGROUND -> "Фон экрана"
    ThemeToken.SURFACE -> "Поверхность (карточки)"
    ThemeToken.ON_SURFACE -> "Текст на поверхности"
    ThemeToken.SURFACE_VARIANT -> "Приглушённый фон"
    ThemeToken.OUTLINE -> "Рамки и разделители"
}