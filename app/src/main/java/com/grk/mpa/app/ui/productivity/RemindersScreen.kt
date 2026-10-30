package com.grk.mpa.app.ui.productivity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Reminder
import com.grk.mpa.app.core.ReminderOperations
import com.grk.mpa.app.data.DataStoreReminderRepository
import com.grk.mpa.app.data.ReminderAlarm
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val reminderDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

class RemindersViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val repository = DataStoreReminderRepository(appContext)
    var items by mutableStateOf<List<Reminder>>(emptyList())
        private set
    var title by mutableStateOf("")
    var dateTime by mutableStateOf(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).format(reminderDateFormat))
    var editingId by mutableStateOf<Long?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    init { viewModelScope.launch { items = repository.load(); loaded = true } }

    fun edit(item: Reminder) {
        editingId = item.id
        title = item.title
        dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(item.triggerAtMillis), ZoneId.systemDefault()).format(reminderDateFormat)
    }

    fun cancelEdit() { editingId = null; title = ""; dateTime = LocalDateTime.now().plusHours(1).format(reminderDateFormat) }

    fun save() {
        val at = runCatching { LocalDateTime.parse(dateTime.trim(), reminderDateFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return
        if (at <= System.currentTimeMillis()) return
        val id = editingId
        val updated = if (id == null) ReminderOperations.add(items, title, at)
            else ReminderOperations.update(items, id, title, at)
        if (updated == items) return
        items = updated
        viewModelScope.launch { repository.save(updated) }
        id?.let { ReminderAlarm.cancel(appContext, it) }
        updated.firstOrNull { it.triggerAtMillis == at && !it.isCompleted && (id == null || it.id == id) }?.let { ReminderAlarm.schedule(appContext, it) }
        cancelEdit()
    }

    fun complete(item: Reminder) {
        ReminderAlarm.cancel(appContext, item.id)
        items = ReminderOperations.complete(items, item.id)
        viewModelScope.launch { repository.save(items) }
    }

    fun delete(item: Reminder) {
        ReminderAlarm.cancel(appContext, item.id)
        items = ReminderOperations.delete(items, item.id)
        viewModelScope.launch { repository.save(items) }
    }
}

@Composable
fun RemindersScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val vm: RemindersViewModel = viewModel { RemindersViewModel(context) }
    var showCompleted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(ReminderAlarm.CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH)
        )
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(vm.title, { vm.title = it.take(100) }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.reminder_title_label)) }, singleLine = true)
        OutlinedTextField(vm.dateTime, { vm.dateTime = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.reminder_datetime_label)) }, supportingText = { Text(stringResource(R.string.reminder_datetime_hint)) }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::save, enabled = vm.loaded && vm.title.isNotBlank()) { Text(stringResource(if (vm.editingId == null) R.string.reminder_add else R.string.reminder_save)) }
            if (vm.editingId != null) TextButton(onClick = vm::cancelEdit) { Text(stringResource(R.string.reminder_cancel)) }
            TextButton(onClick = { showCompleted = !showCompleted }) { Text(stringResource(if (showCompleted) R.string.reminder_show_upcoming else R.string.reminder_show_completed)) }
        }
        val visible = if (showCompleted) ReminderOperations.completed(vm.items) else ReminderOperations.upcoming(vm.items)
        if (visible.isEmpty()) Text(stringResource(if (showCompleted) R.string.reminder_empty_completed else R.string.reminder_empty_upcoming), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(item.triggerAtMillis), ZoneId.systemDefault()).format(reminderDateFormat), style = MaterialTheme.typography.bodySmall)
                    Row {
                        if (!item.isCompleted) TextButton(onClick = { vm.edit(item) }) { Text(stringResource(R.string.reminder_edit)) }
                        if (!item.isCompleted) TextButton(onClick = { vm.complete(item) }) { Text(stringResource(R.string.reminder_complete)) }
                        IconButton(onClick = { vm.delete(item) }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.reminder_delete)) }
                    }
                }
            }
        }
    }
}
