package com.grk.mpa.app.ui.productivity

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Expense
import com.grk.mpa.app.core.ExpenseCategory
import com.grk.mpa.app.core.Money
import com.grk.mpa.app.data.DataStoreExpenseRepository
import com.grk.mpa.app.ui.theme.MpaDimensions
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val expensesViewModelFactory = viewModelFactory {
    initializer {
        ExpensesViewModel(DataStoreExpenseRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = viewModel(factory = expensesViewModelFactory)
) {
    val context = LocalContext.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // The system "save file" screen lets the person choose where the CSV goes; no storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(viewModel.csvContent().toByteArray(Charsets.UTF_8))
                } != null
            }.getOrDefault(false)
            if (!saved) Toast.makeText(context, R.string.expenses_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val month = viewModel.viewedMonth
    val monthLabel = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MpaDimensions.screenHorizontalPadding),
        contentPadding = PaddingValues(top = MpaDimensions.compactSpacing, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        item(key = "title") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.expenses_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(R.string.expenses_device_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "month") {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::previousMonth) { Text(stringResource(R.string.expenses_previous)) }
                    Text(
                        monthLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = viewModel::nextMonth, enabled = !viewModel.isCurrentMonth) {
                        Text(stringResource(R.string.expenses_next))
                    }
                }
                if (!viewModel.isCurrentMonth) {
                    TextButton(onClick = viewModel::goToCurrentMonth, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.expenses_this_month))
                    }
                }
            }
        }

        item(key = "stats") {
            val remaining = viewModel.remainingPaise
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                    StatRow(
                        label = if (viewModel.isCurrentMonth) stringResource(R.string.expenses_spent_this_month)
                        else stringResource(R.string.expenses_spent_in, monthName),
                        value = Money.format(viewModel.spentPaise)
                    )
                    StatRow(
                        label = stringResource(R.string.expenses_monthly_budget),
                        value = viewModel.budgetPaise?.let { Money.format(it) } ?: stringResource(R.string.expenses_not_set)
                    )
                    StatRow(
                        label = when {
                            remaining == null -> stringResource(R.string.expenses_budget_status)
                            remaining < 0 -> stringResource(R.string.expenses_over_budget)
                            else -> stringResource(R.string.expenses_budget_remaining)
                        },
                        value = if (remaining == null) stringResource(R.string.expenses_set_a_budget)
                        else Money.format(kotlin.math.abs(remaining)),
                        highlightError = remaining != null && remaining < 0
                    )
                }
            }
        }

        val breakdown = viewModel.breakdown
        if (breakdown.isNotEmpty()) {
            item(key = "breakdown") {
                Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                    SectionTitle(stringResource(R.string.expenses_breakdown_title))
                    breakdown.forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.category.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.expenses_breakdown_value, Money.format(row.totalPaise), row.percent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(progress = { row.percent / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        item(key = "budget") {
            Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                OutlinedTextField(
                    value = viewModel.budgetText,
                    onValueChange = viewModel::onBudgetChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.expenses_budget_label)) },
                    singleLine = true,
                    enabled = viewModel.isLoaded,
                    isError = viewModel.budgetError,
                    supportingText = if (viewModel.budgetError) {
                        { Text(stringResource(R.string.expenses_budget_error)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveBudget() })
                )
                OutlinedButton(onClick = viewModel::saveBudget, enabled = viewModel.isLoaded) {
                    Text(stringResource(R.string.expenses_budget_save))
                }
            }
        }

        item(key = "form") {
            ExpenseForm(viewModel = viewModel, onPickDate = { showDatePicker = true })
        }

        item(key = "list-title") { SectionTitle(stringResource(R.string.expenses_list_title)) }

        val expenses = viewModel.monthExpenses
        if (expenses.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.expenses_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    onEdit = { viewModel.startEdit(expense.id) },
                    onDelete = { pendingDeleteId = expense.id }
                )
            }
            item(key = "export") {
                OutlinedButton(onClick = { exportLauncher.launch(viewModel.csvFileName) }) {
                    Text(stringResource(R.string.expenses_export))
                }
            }
        }
    }

    if (showDatePicker) {
        ExpenseDatePicker(
            initial = viewModel.spentOn,
            onPick = viewModel::onDateChange,
            onDismiss = { showDatePicker = false }
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.expenses_delete_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(id)
                    pendingDeleteId = null
                }) { Text(stringResource(R.string.expenses_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(stringResource(R.string.expenses_cancel)) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun StatRow(label: String, value: String, highlightError: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlightError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExpenseForm(viewModel: ExpensesViewModel, onPickDate: () -> Unit) {
    val editing = viewModel.editingId != null
    val error = viewModel.formError
    Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
        SectionTitle(stringResource(if (editing) R.string.expenses_form_edit_title else R.string.expenses_form_add_title))
        OutlinedTextField(
            value = viewModel.amountText,
            onValueChange = viewModel::onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.expenses_amount_label)) },
            singleLine = true,
            enabled = viewModel.isLoaded,
            isError = error == ExpenseFormError.AMOUNT,
            supportingText = if (error == ExpenseFormError.AMOUNT) {
                { Text(stringResource(R.string.expenses_error_amount)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = viewModel.descriptionText,
            onValueChange = viewModel::onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.expenses_description_label)) },
            singleLine = true,
            enabled = viewModel.isLoaded,
            isError = error == ExpenseFormError.DESCRIPTION,
            supportingText = if (error == ExpenseFormError.DESCRIPTION) {
                { Text(stringResource(R.string.expenses_error_description)) }
            } else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() })
        )
        Text(
            stringResource(R.string.expenses_category_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            ExpenseCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == viewModel.category,
                    onClick = { viewModel.onCategoryChange(category) },
                    label = { Text(category.label) }
                )
            }
        }
        OutlinedButton(onClick = onPickDate) {
            Text(stringResource(R.string.expenses_date_label) + ": " + formatDate(viewModel.spentOn))
        }
        if (error == ExpenseFormError.FUTURE_DATE) {
            Text(
                stringResource(R.string.expenses_error_future),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)) {
            Button(onClick = viewModel::submit, enabled = viewModel.isLoaded) {
                Text(stringResource(if (editing) R.string.expenses_save_changes else R.string.expenses_add))
            }
            if (editing) {
                OutlinedButton(onClick = viewModel::cancelEdit) { Text(stringResource(R.string.expenses_cancel)) }
            }
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(expense.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                formatDate(expense.spentOn) + " · " + expense.category.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(Money.format(expense.amountPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        TextButton(onClick = onEdit) { Text(stringResource(R.string.expenses_edit)) }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.expenses_delete))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePicker(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(stringResource(R.string.expenses_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.expenses_cancel)) } }
    ) {
        DatePicker(state = state)
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
