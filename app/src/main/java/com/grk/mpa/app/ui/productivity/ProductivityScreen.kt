package com.grk.mpa.app.ui.productivity

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.grk.mpa.app.R
import com.grk.mpa.app.ui.theme.MpaDimensions

private enum class ProductivityTool(@StringRes val titleRes: Int) {
    TASKS(R.string.tool_tasks),
    EXPENSES(R.string.tool_expenses)
}

/** The Productivity tab: a switcher between the to-do list and the Expense Tracker. */
@Composable
fun ProductivityScreen(modifier: Modifier = Modifier) {
    var selectedName by rememberSaveable { mutableStateOf(ProductivityTool.TASKS.name) }
    val selected = ProductivityTool.valueOf(selectedName)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = MpaDimensions.screenHorizontalPadding, vertical = MpaDimensions.sectionSpacing),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            ProductivityTool.entries.forEach { tool ->
                FilterChip(
                    selected = tool == selected,
                    onClick = { selectedName = tool.name },
                    label = { Text(stringResource(tool.titleRes)) }
                )
            }
        }
        when (selected) {
            ProductivityTool.TASKS -> TasksScreen()
            ProductivityTool.EXPENSES -> ExpensesScreen()
        }
    }
}
