package com.grk.mpa.app.ui.utilities

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grk.mpa.app.R
import com.grk.mpa.app.core.NumberFormatter
import com.grk.mpa.app.core.UnitCategories
import com.grk.mpa.app.core.UnitCategory

private enum class UtilityTool(@StringRes val titleRes: Int) {
    UNITS(R.string.tool_units),
    BINARY(R.string.tool_binary)
}

@Composable
fun UtilitiesScreen(modifier: Modifier = Modifier) {
    var selectedName by rememberSaveable { mutableStateOf(UtilityTool.UNITS.name) }
    val selected = UtilityTool.valueOf(selectedName)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UtilityTool.entries.forEach { tool ->
                FilterChip(
                    selected = tool == selected,
                    onClick = { selectedName = tool.name },
                    label = { Text(stringResource(tool.titleRes)) }
                )
            }
        }
        when (selected) {
            UtilityTool.UNITS -> UnitConverterScreen()
            UtilityTool.BINARY -> BinaryConverterScreen()
        }
    }
}

@Composable
private fun ToolColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) { content() }
}

@Composable
fun UnitConverterScreen(viewModel: UnitConverterViewModel = viewModel()) {
    // Store IDs as a saveable string so each card can independently retain its open/closed state.
    var expandedIds by rememberSaveable { mutableStateOf("length") }
    val expanded = expandedIds.split(',').filter { it.isNotBlank() }.toSet()

    ToolColumn {
        Text(
            stringResource(R.string.units_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        UnitCategories.all.forEach { category ->
            val isExpanded = category.id in expanded
            val style = UnitCategoryStyles.of(category.id)
            val dark = isSystemInDarkTheme()
            val accent = style.accent(dark)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = style.tint(dark))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = if (isExpanded) expanded - category.id else expanded + category.id
                            expandedIds = next.joinToString(",")
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text(style.emoji, style = MaterialTheme.typography.titleMedium) }
                        Text(
                            category.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        if (isExpanded) "⌃" else "⌄",
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }
                if (isExpanded) {
                    Column(
                        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        category.units.forEach { unit ->
                            val isSource = viewModel.isSource(category, unit)
                            val invalid = isSource && viewModel.isInvalid(category)
                            val belowMinimum = isSource && viewModel.isBelowMinimum(category, unit)
                            val surface = MaterialTheme.colorScheme.surface
                            // The field being typed in gets a stronger tint; results stay on plain surface.
                            val fieldColor = if (isSource) accent.copy(alpha = 0.18f).compositeOver(surface) else surface
                            OutlinedTextField(
                                value = viewModel.displayValue(category, unit),
                                onValueChange = { viewModel.onValueChange(category, unit, it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(unit.label) },
                                singleLine = true,
                                isError = invalid,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = fieldColor,
                                    unfocusedContainerColor = fieldColor,
                                    focusedBorderColor = accent,
                                    unfocusedBorderColor = if (isSource) accent else accent.copy(alpha = 0.35f),
                                    focusedLabelColor = accent
                                ),
                                supportingText = when {
                                    invalid -> ({ Text(stringResource(R.string.error_invalid_number)) })
                                    belowMinimum -> ({
                                        Text(stringResource(
                                            R.string.units_minimum,
                                            NumberFormatter.format(unit.minimum!!)
                                        ))
                                    })
                                    else -> null
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (category.allowsNegative) {
                                        KeyboardType.Text
                                    } else {
                                        KeyboardType.Decimal
                                    }
                                )
                            )
                        }
                        category.note?.let { note ->
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BinaryConverterScreen(viewModel: BinaryConverterViewModel = viewModel()) {
    ToolColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.mode == BinaryMode.DECIMAL_TO_BINARY,
                onClick = { viewModel.onModeChange(BinaryMode.DECIMAL_TO_BINARY) },
                label = { Text(stringResource(R.string.binary_mode_dec_to_bin)) }
            )
            FilterChip(
                selected = viewModel.mode == BinaryMode.BINARY_TO_DECIMAL,
                onClick = { viewModel.onModeChange(BinaryMode.BINARY_TO_DECIMAL) },
                label = { Text(stringResource(R.string.binary_mode_bin_to_dec)) }
            )
        }
        val toBinary = viewModel.mode == BinaryMode.DECIMAL_TO_BINARY
        OutlinedTextField(
            value = viewModel.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(if (toBinary) R.string.binary_input_decimal else R.string.binary_input_binary))
            },
            singleLine = true,
            isError = viewModel.isInvalid,
            supportingText = if (viewModel.isInvalid) {
                {
                    Text(
                        stringResource(
                            if (toBinary) R.string.error_invalid_decimal else R.string.error_invalid_binary
                        )
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (toBinary) KeyboardType.Decimal else KeyboardType.Number)
        )
        viewModel.result?.let { result ->
            ResultCard {
                Text(
                    stringResource(if (toBinary) R.string.binary_result_binary else R.string.binary_result_decimal),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(result, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
