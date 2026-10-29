package com.grk.mpa.app.ui.productivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.TaskFilter
import com.grk.mpa.app.data.DataStoreTaskRepository

private val tasksViewModelFactory = viewModelFactory {
    initializer {
        TasksViewModel(DataStoreTaskRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
fun ProductivityScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel(factory = tasksViewModelFactory)
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.tasks_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = viewModel.newTitle,
                onValueChange = viewModel::onNewTitleChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.tasks_new_label)) },
                singleLine = true,
                enabled = viewModel.isLoaded,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.addTask() })
            )
            Button(onClick = viewModel::addTask, enabled = viewModel.canAdd) {
                Text(stringResource(R.string.tasks_add))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == viewModel.filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes())) }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(R.plurals.tasks_remaining, viewModel.activeCount, viewModel.activeCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.hasCompleted) {
                TextButton(onClick = viewModel::clearCompleted) {
                    Text(stringResource(R.string.tasks_clear_completed))
                }
            }
        }
        val visible = viewModel.visibleTasks
        if (visible.isEmpty()) {
            Text(
                stringResource(if (viewModel.isLoaded) R.string.tasks_empty else R.string.tasks_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(visible, key = { it.id }) { task ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.isDone, onCheckedChange = { viewModel.toggle(task.id) })
                        Text(
                            task.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { viewModel.delete(task.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.tasks_delete))
                        }
                    }
                }
            }
        }
    }
}

private fun TaskFilter.labelRes(): Int = when (this) {
    TaskFilter.ALL -> R.string.tasks_filter_all
    TaskFilter.ACTIVE -> R.string.tasks_filter_active
    TaskFilter.DONE -> R.string.tasks_filter_done
}
