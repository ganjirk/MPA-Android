
package com.grk.mpa.app.ui.productivity

import com.grk.mpa.app.ui.theme.MpaDimensions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.OutlinedButton
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
import com.grk.mpa.app.core.TaskSyncState
import com.grk.mpa.app.data.DataStoreTaskRepository
import com.grk.mpa.app.data.TaskSyncCoordinator
import com.grk.mpa.app.data.api.MpaApiClient
import com.grk.mpa.app.data.auth.EncryptedAuthStorage

private val tasksViewModelFactory = viewModelFactory {
    initializer {
        val app = checkNotNull(this[APPLICATION_KEY])
        val repository = DataStoreTaskRepository(app)
        val coordinator = TaskSyncCoordinator(
            api = MpaApiClient(),
            authStorage = EncryptedAuthStorage(app),
            repository = repository
        )
        TasksViewModel(repository, syncCoordinator = coordinator)
    }
}

@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = viewModel(factory = tasksViewModelFactory)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
			.padding(horizontal = MpaDimensions.screenHorizontalPadding)
			.padding(top = MpaDimensions.screenTopPadding),
	verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            Text(
                text = stringResource(R.string.tasks_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(
                onClick = viewModel::syncTasks,
                enabled = viewModel.isLoaded && viewModel.syncStatus != TaskSyncUiStatus.SYNCING
            ) {
                Text(
                    stringResource(
                        if (viewModel.syncStatus == TaskSyncUiStatus.SYNCING) R.string.tasks_syncing
                        else R.string.tasks_sync
                    )
                )
            }
        }
        val syncMessageRes = when (viewModel.syncStatus) {
            TaskSyncUiStatus.IDLE, TaskSyncUiStatus.SYNCING -> null
            TaskSyncUiStatus.SUCCESS -> R.string.tasks_sync_success
            TaskSyncUiStatus.SIGNED_OUT -> R.string.tasks_sync_signed_out
            TaskSyncUiStatus.OFFLINE -> R.string.tasks_sync_offline
            TaskSyncUiStatus.ACCOUNT_MISMATCH -> R.string.tasks_sync_account_mismatch
            TaskSyncUiStatus.FAILED -> R.string.tasks_sync_failed
        }
        if (syncMessageRes != null) {
            Text(
                text = stringResource(syncMessageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stack the task field and Add button when horizontal space is limited.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth < 340.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
                ) {
                    OutlinedTextField(
                        value = viewModel.newTitle,
                        onValueChange = viewModel::onNewTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.tasks_new_label)) },
                        singleLine = true,
                        enabled = viewModel.isLoaded,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.addTask() }
                        )
                    )

                    Button(
                        onClick = viewModel::addTask,
                        enabled = viewModel.canAdd,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.tasks_add))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.newTitle,
                        onValueChange = viewModel::onNewTitleChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.tasks_new_label)) },
                        singleLine = true,
                        enabled = viewModel.isLoaded,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.addTask() }
                        )
                    )

                    Button(
                        onClick = viewModel::addTask,
                        enabled = viewModel.canAdd
                    ) {
                        Text(stringResource(R.string.tasks_add))
                    }
                }
            }
        }

        // Keep filters accessible on narrow screens without squeezing chips.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            TaskFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filter == viewModel.filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = { Text(stringResource(filter.labelRes())) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.tasks_remaining,
                    viewModel.activeCount,
                    viewModel.activeCount
                ),
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
        if (viewModel.canMigrateLegacyTasks) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = MpaDimensions.compactSpacing),
                verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
            ) {
                Text(
                    text = stringResource(
                        R.string.tasks_legacy_migration_explanation,
                        viewModel.localOnlyTaskCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = viewModel::prepareSelectedLegacyTasksForUpload,
                    enabled = viewModel.selectedLegacyTaskCount > 0
                ) {
                    Text(
                        stringResource(
                            R.string.tasks_legacy_migration_prepare,
                            viewModel.selectedLegacyTaskCount
                        )
                    )
                }
            }
        }
        val visible = viewModel.visibleTasks

        if (visible.isEmpty()) {
            Text(
                text = stringResource(
                    if (viewModel.isLoaded) {
                        R.string.tasks_empty
                    } else {
                        R.string.tasks_loading
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
            ) {
                items(visible, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = MpaDimensions.compactSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (
                            viewModel.canMigrateLegacyTasks &&
                            task.remoteId == null &&
                            task.syncState == TaskSyncState.LOCAL_ONLY
                        ) {
                            Checkbox(
                                checked = task.id in viewModel.selectedLegacyTaskIds,
                                onCheckedChange = {
                                    viewModel.toggleLegacyTaskSelection(task.id)
                                }
                            )
                        }
                        Checkbox(
                            checked = task.isDone,
                            onCheckedChange = { viewModel.toggle(task.id) }
                        )

                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (task.isDone) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                            color = if (task.isDone) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        )

                        IconButton(onClick = { viewModel.delete(task.id) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.tasks_delete)
                            )
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
