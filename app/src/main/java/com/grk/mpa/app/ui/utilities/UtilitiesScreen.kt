package com.grk.mpa.app.ui.utilities

import androidx.annotation.StringRes
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
import com.grk.mpa.app.core.LengthUnit
import com.grk.mpa.app.core.NumberFormatter

private enum class UtilityTool(@StringRes val titleRes: Int) {
    LENGTH(R.string.tool_length),
    BINARY(R.string.tool_binary)
}

@Composable
fun UtilitiesScreen(modifier: Modifier = Modifier) {
    var selectedName by rememberSaveable { mutableStateOf(UtilityTool.LENGTH.name) }
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
            UtilityTool.LENGTH -> LengthConverterScreen()
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
fun LengthConverterScreen(viewModel: LengthConverterViewModel = viewModel()) {
    ToolColumn {
        OutlinedTextField(
            value = viewModel.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.length_input_label)) },
            singleLine = true,
            isError = viewModel.isInvalid,
            supportingText = if (viewModel.isInvalid) {
                { Text(stringResource(R.string.error_invalid_number)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Text(
            stringResource(R.string.length_from),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LengthUnit.entries.forEach { unit ->
                FilterChip(
                    selected = unit == viewModel.fromUnit,
                    onClick = { viewModel.onUnitChange(unit) },
                    label = { Text(unit.label) }
                )
            }
        }
        val results = viewModel.results
        if (results.isNotEmpty()) {
            ResultCard {
                results.forEach { (unit, converted) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(unit.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(NumberFormatter.format(converted), fontWeight = FontWeight.SemiBold)
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
