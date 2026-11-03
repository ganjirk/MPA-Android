package com.grk.mpa.app.ui.utilities

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.text.TextUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grk.mpa.app.core.DecisionCategory
import com.grk.mpa.app.core.DecisionWheelMath
import com.grk.mpa.app.data.DataStoreDecisionMakerRepository
import java.text.DateFormat
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DecisionMakerScreen(
    modifier: Modifier = Modifier,
    viewModel: DecisionMakerViewModel = decisionMakerViewModel()
) {
    var optionInput by rememberSaveable { mutableStateOf("") }
    var categoryDialog by rememberSaveable { mutableStateOf(false) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var editingCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var optionDialogIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var optionDialogValue by rememberSaveable { mutableStateOf("") }
    var deleteCategory by remember { mutableStateOf<DecisionCategory?>(null) }
    var clearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    val wheelRotation = remember { Animatable(0f) }
    LaunchedEffect(viewModel.spinToken, viewModel.spinTargetIndex, viewModel.displayMode) {
        val optionCount = viewModel.selectedCategory?.options?.size ?: 0
        val winnerIndex = viewModel.spinTargetIndex
        if (
            viewModel.winners.isNotEmpty() &&
            viewModel.displayMode == DecisionDisplayMode.WHEEL &&
            winnerIndex != null &&
            optionCount > 0
        ) {
            wheelRotation.animateTo(
                targetValue = DecisionWheelMath.targetRotation(
                    currentRotation = wheelRotation.value,
                    winnerIndex = winnerIndex,
                    optionCount = optionCount
                ),
                animationSpec = tween(durationMillis = 4800)
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Decision Maker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (viewModel.isLoaded) "Saved on this device" else "Loading…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.categories.forEach { category ->
                FilterChip(
                    selected = category.id == viewModel.selectedCategoryId,
                    onClick = { viewModel.selectCategory(category.id) },
                    label = { Text(category.name) }
                )
            }
        }

        val category = viewModel.selectedCategory
        if (category != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (category.isCustom) {
                    TextButton(onClick = {
                        editingCategoryId = category.id
                        categoryName = category.name
                        categoryDialog = true
                    }) { Text("Rename") }
                    TextButton(onClick = { deleteCategory = category }) { Text("Delete") }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    editingCategoryId = null
                    categoryName = ""
                    categoryDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Category")
            }

            if (category != null) {
                if (!category.isCustom) {
                    OutlinedButton(
                        onClick = viewModel::restoreDefaults,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore selected category defaults")
                    }
                }

                OutlinedButton(
                    onClick = viewModel::resetOptions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset options")
                }
            }
        }

        Text("Number of winners", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { count ->
                FilterChip(
                    selected = viewModel.winnerCount == count,
                    enabled = (category?.options?.size ?: 0) >= count,
                    onClick = { viewModel.updateWinnerCount(count) },
                    label = { Text(count.toString()) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.displayMode == DecisionDisplayMode.WHEEL,
                onClick = { viewModel.updateDisplayMode(DecisionDisplayMode.WHEEL) },
                label = { Text("Wheel") }
            )
            FilterChip(
                selected = viewModel.displayMode == DecisionDisplayMode.LIST,
                onClick = { viewModel.updateDisplayMode(DecisionDisplayMode.LIST) },
                label = { Text("List") }
            )
        }

        if (viewModel.displayMode == DecisionDisplayMode.WHEEL) {
            val wheelOptions = category?.options.orEmpty()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier.size(260.dp).graphicsLayer { rotationZ = wheelRotation.value }
                ) {
                    val colors = listOf(
                        Color(0xFF477BBA), Color(0xFF4F9B83), Color(0xFFE0A34B),
                        Color(0xFF9A70B5), Color(0xFFCF6D69), Color(0xFF5596A5)
                    )
                    val optionCount = wheelOptions.size.coerceAtLeast(1)
                    val sweep = 360f / optionCount
                    wheelOptions.forEachIndexed { index, _ ->
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = -90f + index * sweep,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                    }

                    // Draw each option on its own slice. Labels follow the slice
                    // angle and are flipped when needed to remain readable.
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = size.minDimension / 2f
                    val labelRadius = radius * 0.57f
                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textAlign = AndroidPaint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = radius * 0.14f
                        setShadowLayer(radius * 0.025f, 0f, radius * 0.01f, android.graphics.Color.BLACK)
                    }

                    wheelOptions.forEachIndexed { index, option ->
                        val middleAngle = -90f + (index + 0.5f) * sweep
                        val radians = Math.toRadians(middleAngle.toDouble())
                        val labelX = centerX + cos(radians).toFloat() * labelRadius
                        val labelY = centerY + sin(radians).toFloat() * labelRadius
                        val maxLabelWidth = (
                            2f * labelRadius * sin(Math.PI / optionCount).toFloat() * 0.88f
                        ).coerceAtLeast(radius * 0.12f)
                        paint.textSize = min(radius * 0.14f, maxLabelWidth * 0.34f)
                            .coerceAtLeast(radius * 0.075f)
                        val label = TextUtils.ellipsize(
                            option,
                            paint,
                            maxLabelWidth,
                            TextUtils.TruncateAt.END
                        ).toString()

                        var textRotation = (middleAngle + 90f + 360f) % 360f
                        val flipText = textRotation in 90f..270f
                        if (flipText) textRotation = (textRotation + 180f) % 360f

                        nativeCanvas.save()
                        nativeCanvas.rotate(textRotation, labelX, labelY)
                        nativeCanvas.drawText(
                            label,
                            labelX,
                            labelY + paint.textSize * 0.35f,
                            paint
                        )
                        nativeCanvas.restore()
                    }
                }
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {}
                Text("▼", modifier = Modifier.align(Alignment.TopCenter), color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "The wheel spins for about five seconds. Each slice shows an option; the pointer stops on the selected winner.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text("A random selection is made from the available options.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::pickWinners, enabled = viewModel.canPick, modifier = Modifier.weight(1f)) {
                Text("Pick winner")
            }
            OutlinedButton(onClick = viewModel::pickAgain, enabled = viewModel.canPick) {
                Text("Pick Again")
            }
        }

        if (viewModel.winners.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (viewModel.winners.size == 1) "The winner is…" else "The winners are…", style = MaterialTheme.typography.titleMedium)
                    if (viewModel.winners.size > 1 && viewModel.displayMode == DecisionDisplayMode.WHEEL) {
                        Text(
                            "The wheel pointer highlights the first winner; the remaining winners are selected uniquely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    viewModel.winners.forEachIndexed { index, winner ->
                        Text("${index + 1}. $winner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = viewModel::clearWinners) { Text("Reset choices") }
                }
            }
        }

        viewModel.errorMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
        }

        Text("Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (category?.options.isNullOrEmpty()) {
            Text("No options yet. Add at least two options to make a decision.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            category!!.options.forEachIndexed { index, option ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(option, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        optionDialogIndex = index
                        optionDialogValue = option
                    }) { Text("Edit") }
                    TextButton(onClick = { viewModel.removeOption(index) }) { Text("Remove") }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = optionInput,
                onValueChange = { optionInput = it.take(100) },
                modifier = Modifier.weight(1f),
                label = { Text("Add an option") },
                singleLine = true
            )
            Button(
                onClick = {
                    viewModel.addOption(optionInput)
                    if (viewModel.errorMessage == null) optionInput = ""
                },
                enabled = viewModel.canAddOption && optionInput.isNotBlank()
            ) { Text("Add") }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (viewModel.history.isNotEmpty()) {
                TextButton(onClick = { clearHistoryDialog = true }) { Text("Clear history") }
            }
        }
        if (viewModel.history.isEmpty()) {
            Text("Your recent picks will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            viewModel.history.forEach { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.categoryName, fontWeight = FontWeight.SemiBold)
                        Text(entry.winners.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.pickedAtMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (categoryDialog) {
        AlertDialog(
            onDismissRequest = { categoryDialog = false },
            title = { Text(if (editingCategoryId == null) "Create category" else "Rename category") },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it.take(50) },
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingCategoryId
                    if (id == null) viewModel.addCategory(categoryName) else viewModel.renameCategory(id, categoryName)
                    categoryDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { categoryDialog = false }) { Text("Cancel") } }
        )
    }

    optionDialogIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { optionDialogIndex = null },
            title = { Text("Edit option") },
            text = {
                OutlinedTextField(
                    value = optionDialogValue,
                    onValueChange = { optionDialogValue = it.take(100) },
                    label = { Text("Option") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editOption(index, optionDialogValue)
                    optionDialogIndex = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { optionDialogIndex = null }) { Text("Cancel") } }
        )
    }

    deleteCategory?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCategory = null },
            title = { Text("Delete category?") },
            text = { Text("Delete \"${target.name}\" and its options?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(target.id)
                    deleteCategory = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCategory = null }) { Text("Cancel") } }
        )
    }

    if (clearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { clearHistoryDialog = false },
            title = { Text("Clear history?") },
            text = { Text("This removes your saved recent picks from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    clearHistoryDialog = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { clearHistoryDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun decisionMakerViewModel(): DecisionMakerViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) { DataStoreDecisionMakerRepository(context) }
    return viewModel(factory = remember(repository) { DecisionMakerViewModel.Factory(repository) })
}
