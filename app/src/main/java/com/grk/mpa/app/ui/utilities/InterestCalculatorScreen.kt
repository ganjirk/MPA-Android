package com.grk.mpa.app.ui.utilities

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Compounding
import com.grk.mpa.app.core.IndianMoney
import com.grk.mpa.app.core.InterestError
import com.grk.mpa.app.core.InterestOutcome
import com.grk.mpa.app.core.InterestResult
import com.grk.mpa.app.core.InterestType
import com.grk.mpa.app.core.RateUnit

private fun InterestError.messageRes(): Int = when (this) {
    InterestError.PRINCIPAL_INVALID -> R.string.interest_error_principal
    InterestError.PRINCIPAL_TOO_LARGE -> R.string.interest_error_principal_large
    InterestError.RATE_INVALID -> R.string.interest_error_rate
    InterestError.RATE_TOO_HIGH -> R.string.interest_error_rate_high
    InterestError.TIME_NEGATIVE -> R.string.interest_error_time_negative
    InterestError.TIME_ZERO -> R.string.interest_error_time_zero
    InterestError.TIME_TOO_LONG -> R.string.interest_error_time_long
}

private fun RateUnit.labelRes(): Int = when (this) {
    RateUnit.RS_PER_100_MONTH -> R.string.interest_rate_unit_rs
    RateUnit.PERCENT_MONTH -> R.string.interest_rate_unit_month
    RateUnit.PERCENT_YEAR -> R.string.interest_rate_unit_year
}

private fun Compounding.labelRes(): Int = when (this) {
    Compounding.MONTHLY -> R.string.interest_every_month
    Compounding.QUARTERLY -> R.string.interest_every_3_months
    Compounding.HALF_YEARLY -> R.string.interest_every_6_months
    Compounding.YEARLY -> R.string.interest_every_year
}

@Composable
fun InterestCalculatorScreen(viewModel: InterestCalculatorViewModel = viewModel()) {
    ToolColumn {
        Text(
            stringResource(R.string.interest_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InputCard(viewModel)
        ResultCard(viewModel)
        Text(
            stringResource(R.string.interest_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InputCard(vm: InterestCalculatorViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = vm.principal,
                onValueChange = vm::onPrincipalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.interest_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(stringResource(R.string.interest_rate), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RateUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = vm.rateUnit == unit,
                        onClick = { vm.onRateUnitChange(unit) },
                        label = { Text(stringResource(unit.labelRes())) }
                    )
                }
            }
            OutlinedTextField(
                value = vm.rate,
                onValueChange = vm::onRateChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.interest_rate)) },
                singleLine = true,
                supportingText = {
                    val note = rateNote(vm)
                    Text(note ?: stringResource(R.string.interest_rate_pick_hint))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(stringResource(R.string.interest_time), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeBox(Modifier.weight(1f), vm.years, vm::onYearsChange, R.string.interest_years)
                TimeBox(Modifier.weight(1f), vm.months, vm::onMonthsChange, R.string.interest_months)
                TimeBox(Modifier.weight(1f), vm.days, vm::onDaysChange, R.string.interest_days)
            }

            Text(stringResource(R.string.interest_type), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.type == InterestType.SIMPLE,
                    onClick = { vm.onTypeChange(InterestType.SIMPLE) },
                    label = { Text(stringResource(R.string.interest_simple)) }
                )
                FilterChip(
                    selected = vm.type == InterestType.COMPOUND,
                    onClick = { vm.onTypeChange(InterestType.COMPOUND) },
                    label = { Text(stringResource(R.string.interest_compound)) }
                )
            }
            if (vm.type == InterestType.COMPOUND) {
                Text(stringResource(R.string.interest_added), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Compounding.entries.forEach { option ->
                        FilterChip(
                            selected = vm.compounding == option,
                            onClick = { vm.onCompoundingChange(option) },
                            label = { Text(stringResource(option.labelRes())) }
                        )
                    }
                }
            }
            OutlinedButton(onClick = vm::clear) { Text(stringResource(R.string.interest_clear)) }
        }
    }
}

@Composable
private fun rateNote(vm: InterestCalculatorViewModel): String? {
    val (monthly, annual) = vm.rateEquivalent ?: return null
    return when (vm.rateUnit) {
        RateUnit.RS_PER_100_MONTH ->
            stringResource(R.string.interest_rate_note_rs, vm.rate.trim(), IndianMoney.percent(monthly), IndianMoney.percent(annual))
        RateUnit.PERCENT_MONTH ->
            stringResource(R.string.interest_rate_note_month, IndianMoney.percent(monthly), IndianMoney.percent(annual))
        RateUnit.PERCENT_YEAR ->
            stringResource(R.string.interest_rate_note_year, IndianMoney.percent(annual), IndianMoney.percent(monthly))
    }
}

@Composable
private fun TimeBox(modifier: Modifier, value: String, onChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ResultCard(vm: InterestCalculatorViewModel) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.interest_result),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            when (val outcome = vm.outcome) {
                null -> Text(
                    stringResource(R.string.interest_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is InterestOutcome.Invalid -> Text(
                    stringResource(outcome.error.messageRes()),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                is InterestOutcome.Ok -> ResultBody(outcome.result)
            }
        }
    }
}

@Composable
private fun ResultBody(result: InterestResult) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.interest_result_interest), style = MaterialTheme.typography.labelLarge)
            Text(
                IndianMoney.format(result.interest),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
    Figure(R.string.interest_result_total, IndianMoney.format(result.total))
    Figure(R.string.interest_result_amount, IndianMoney.format(result.principal))

    val monthly = IndianMoney.percent(result.monthlyPercent)
    val annual = IndianMoney.percent(result.annualPercent)
    Text(
        if (result.type == InterestType.COMPOUND) {
            stringResource(R.string.interest_rate_line_compound, monthly, annual, IndianMoney.percent(result.effectiveAnnualPercent))
        } else {
            stringResource(R.string.interest_rate_line, monthly, annual)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        stringResource(R.string.interest_year_by_year),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    result.rows.forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(if (row.partial) R.string.interest_year_label_part else R.string.interest_year_label, row.year),
                modifier = Modifier.weight(1.2f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(IndianMoney.format(row.interest), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Text(IndianMoney.format(row.total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Figure(labelRes: Int, value: String) {
    Column {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
