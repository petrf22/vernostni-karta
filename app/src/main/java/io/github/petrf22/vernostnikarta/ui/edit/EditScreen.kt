package io.github.petrf22.vernostnikarta.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.petrf22.vernostnikarta.R
import io.github.petrf22.vernostnikarta.barcode.BarcodeRenderer
import io.github.petrf22.vernostnikarta.barcode.CodeFormat
import io.github.petrf22.vernostnikarta.data.CardEntity
import io.github.petrf22.vernostnikarta.ui.CARD_PALETTE
import io.github.petrf22.vernostnikarta.ui.DEFAULT_CARD_COLOR
import io.github.petrf22.vernostnikarta.ui.StoreColors
import io.github.petrf22.vernostnikarta.ui.appContainer
import io.github.petrf22.vernostnikarta.ui.components.LoyaltyCardView
import io.github.petrf22.vernostnikarta.ui.contentColorOn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * New card ([cardId] == 0, optionally with a scanned [initialCode]/[initialFormat]) or editing an existing one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    cardId: Long,
    initialCode: String,
    initialFormat: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val isNew = cardId == 0L

    var name by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf(initialCode) }
    // A scanned or stored code is kept exactly; only what the user typed gets trimmed.
    var codeEdited by rememberSaveable { mutableStateOf(false) }
    var formatName by rememberSaveable {
        mutableStateOf((CodeFormat.fromName(initialFormat) ?: CodeFormat.CODE_128).name)
    }
    var color by rememberSaveable { mutableIntStateOf(DEFAULT_CARD_COLOR) }
    // Until the user picks a color, it follows the store name.
    var colorChosen by rememberSaveable { mutableStateOf(!isNew) }
    var showCustomColor by rememberSaveable { mutableStateOf(false) }
    var hue by rememberSaveable { mutableFloatStateOf(0f) }
    var saturation by rememberSaveable { mutableFloatStateOf(0f) }
    var brightness by rememberSaveable { mutableFloatStateOf(0f) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var original by remember { mutableStateOf<CardEntity?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun applyColor(argb: Int) {
        color = argb
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
    }

    LaunchedEffect(cardId) {
        if (isNew) {
            if (!colorChosen) applyColor(color)
            return@LaunchedEffect
        }
        val card = container.repository.getCard(cardId)
        original = card
        if (!loaded && card != null) {
            name = card.name
            note = card.note
            code = card.code
            formatName = (CodeFormat.fromName(card.format) ?: CodeFormat.QR_CODE).name
            applyColor(card.color)
            loaded = true
        }
    }

    val format = CodeFormat.fromName(formatName) ?: CodeFormat.CODE_128
    val effectiveCode = if (codeEdited) code.trim() else code
    val nameError = name.isBlank()
    val codeError = when {
        effectiveCode.isEmpty() -> R.string.error_code_required
        BarcodeRenderer.encode(effectiveCode, format) == null -> R.string.error_code_invalid
        else -> null
    }

    fun save() {
        // The form survives rotation and process death, `original` does not: saving before it is
        // read again would insert a second card instead of updating this one.
        if (!loaded || (!isNew && original == null)) return
        showErrors = true
        if (nameError || codeError != null || saving) return
        saving = true
        val card = original?.copy(
            name = name.trim(), note = note.trim(), code = effectiveCode, format = format.name, color = color,
        ) ?: CardEntity(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            note = note.trim(),
            code = effectiveCode,
            format = format.name,
            color = color,
            createdAt = System.currentTimeMillis(),
        )
        scope.launch {
            container.repository.save(card)
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_card else R.string.edit_card)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { save() }) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoyaltyCardView(
                name = name.ifBlank { stringResource(R.string.store_name) },
                note = note,
                code = effectiveCode,
                format = format,
                color = Color(color),
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (!colorChosen) applyColor(StoreColors.suggest(it) ?: DEFAULT_CARD_COLOR)
                },
                label = { Text(stringResource(R.string.store_name)) },
                singleLine = true,
                isError = showErrors && nameError,
                supportingText = if (showErrors && nameError) {
                    { Text(stringResource(R.string.error_name_required)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeEdited = true
                },
                label = { Text(stringResource(R.string.card_number)) },
                singleLine = true,
                isError = showErrors && codeError != null,
                supportingText = if (showErrors && codeError != null) {
                    { Text(stringResource(codeError)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            FormatPicker(format = format, onPick = { formatName = it.name })

            Text(stringResource(R.string.color), style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CARD_PALETTE.forEach { argb ->
                    ColorDot(
                        color = Color(argb),
                        selected = argb == color,
                        onClick = {
                            colorChosen = true
                            applyColor(argb)
                        },
                    )
                }
            }
            TextButton(onClick = { showCustomColor = !showCustomColor }) {
                Text(stringResource(R.string.custom_color))
            }
            if (showCustomColor) {
                fun fromSliders() {
                    colorChosen = true
                    color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
                }
                LabeledSlider(stringResource(R.string.hue), hue, 0f..360f) { hue = it; fromSliders() }
                LabeledSlider(stringResource(R.string.saturation), saturation, 0f..1f) { saturation = it; fromSliders() }
                LabeledSlider(stringResource(R.string.brightness), brightness, 0f..1f) { brightness = it; fromSliders() }
            }
        }
    }
}

@Composable
private fun FormatPicker(format: CodeFormat, onPick: (CodeFormat) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = format.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.code_type)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // The read-only field swallows taps; this layer opens the menu instead.
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.DropdownList) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CodeFormat.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        expanded = false
                        onPick(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = color,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(44.dp)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
            ),
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = contentColorOn(color),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
