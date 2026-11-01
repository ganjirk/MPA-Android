package com.grk.mpa.app.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun DataExportAction(viewModel: AccountViewModel) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val export = viewModel.exportJson
        if (uri != null && export != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(export) }
            }
        }
    }
    Column {
        Button(onClick = viewModel::prepareDataExport, modifier = Modifier.fillMaxWidth()) {
            Text("Prepare data export")
        }
        if (viewModel.exportJson != null) {
            OutlinedButton(
                onClick = { saveLauncher.launch("mpa-account-export.json") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save account data as JSON") }
        }
    }
}
