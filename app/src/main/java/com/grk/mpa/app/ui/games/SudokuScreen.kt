package com.grk.mpa.app.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.SudokuDifficulty
import com.grk.mpa.app.core.SudokuGame
import com.grk.mpa.app.core.SudokuOperations
import com.grk.mpa.app.core.SudokuStatus
import com.grk.mpa.app.core.SudokuTime
import com.grk.mpa.app.data.DataStoreSudokuRepository
import com.grk.mpa.app.ui.theme.MpaDimensions
import kotlinx.coroutines.delay

private val sudokuViewModelFactory = viewModelFactory {
    initializer {
        SudokuViewModel(DataStoreSudokuRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
fun SudokuScreen(
    modifier: Modifier = Modifier,
    viewModel: SudokuViewModel = viewModel(factory = sudokuViewModelFactory)
) {
    // The clock runs only while this screen is showing and the app is in front.
    LifecycleStartEffect(viewModel) {
        viewModel.onStart()
        onStopOrDispose { viewModel.onStop() }
    }
    LaunchedEffect(viewModel) {
        while (true) {
            delay(1000)
            viewModel.refreshTime()
        }
    }

    val game = viewModel.game
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Text(
            stringResource(R.string.sudoku_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.sudoku_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SudokuDifficulty.entries.forEach { level ->
                FilterChip(
                    selected = level == viewModel.difficulty,
                    onClick = { viewModel.onDifficultyChange(level) },
                    label = { Text(stringResource(level.labelRes())) }
                )
            }
            OutlinedButton(onClick = viewModel::newPuzzle) { Text(stringResource(R.string.sudoku_new_puzzle)) }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val best = viewModel.bestMs[viewModel.difficulty]?.let(SudokuTime::format)
                ?: stringResource(R.string.sudoku_no_best)
            Text(stringResource(R.string.sudoku_time, SudokuTime.format(viewModel.elapsedMs)), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.sudoku_best, best), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.sudoku_progress, game?.filledCount ?: 0), style = MaterialTheme.typography.labelLarge)
        }
        Text(
            statusText(viewModel.status),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (game == null) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.sudoku_generating), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            SudokuBoard(game, onSelect = viewModel::select, modifier = Modifier.widthIn(max = 460.dp))
            NumberPad(onNumber = viewModel::enter)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewModel.notesMode,
                    onClick = viewModel::toggleNotes,
                    label = {
                        Text(stringResource(if (viewModel.notesMode) R.string.sudoku_notes_on else R.string.sudoku_notes_off))
                    }
                )
                OutlinedButton(onClick = viewModel::undo) { Text(stringResource(R.string.sudoku_undo)) }
                OutlinedButton(onClick = viewModel::check) { Text(stringResource(R.string.sudoku_check)) }
                OutlinedButton(onClick = viewModel::hint) {
                    Text(
                        if (game.hints > 0) stringResource(R.string.sudoku_hint_used, game.hints)
                        else stringResource(R.string.sudoku_hint)
                    )
                }
                OutlinedButton(onClick = viewModel::reset) { Text(stringResource(R.string.sudoku_reset)) }
            }
            Text(
                stringResource(R.string.sudoku_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun SudokuDifficulty.labelRes(): Int = when (this) {
    SudokuDifficulty.EASY -> R.string.sudoku_easy
    SudokuDifficulty.MEDIUM -> R.string.sudoku_medium
    SudokuDifficulty.HARD -> R.string.sudoku_hard
}

@Composable
private fun statusText(status: SudokuStatus): String = when (status) {
    SudokuStatus.Prompt -> stringResource(R.string.sudoku_status_prompt)
    SudokuStatus.SelectEmpty -> stringResource(R.string.sudoku_status_select_empty)
    SudokuStatus.Conflict -> stringResource(R.string.sudoku_status_conflict)
    SudokuStatus.Entered -> stringResource(R.string.sudoku_status_entered)
    SudokuStatus.Cleared -> stringResource(R.string.sudoku_status_cleared)
    SudokuStatus.Undone -> stringResource(R.string.sudoku_status_undone)
    SudokuStatus.Reset -> stringResource(R.string.sudoku_status_reset)
    SudokuStatus.HintFilled -> stringResource(R.string.sudoku_status_hint)
    SudokuStatus.AllCorrect -> stringResource(R.string.sudoku_status_all_correct)
    is SudokuStatus.Wrong -> pluralStringResource(R.plurals.sudoku_status_wrong, status.count, status.count)
    is SudokuStatus.Completed -> {
        val time = SudokuTime.format(status.elapsedMs)
        val base = if (status.hints > 0) {
            pluralStringResource(R.plurals.sudoku_status_complete_hints, status.hints, time, status.hints)
        } else {
            stringResource(R.string.sudoku_status_complete, time)
        }
        if (status.newBest) base + " " + stringResource(R.string.sudoku_status_new_best) else base
    }
}

@Composable
private fun SudokuBoard(game: SudokuGame, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val thinLine = MaterialTheme.colorScheme.outlineVariant
    val thickLine = MaterialTheme.colorScheme.outline
    val selectedValue = game.values.getOrElse(game.selected) { 0 }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .drawWithContent {
                drawContent()
                val cell = size.width / 9
                for (k in 0..9) {
                    val thick = k % 3 == 0
                    val width = (if (thick) 3.dp else 1.dp).toPx()
                    val color = if (thick) thickLine else thinLine
                    drawLine(color, Offset(k * cell, 0f), Offset(k * cell, size.height), strokeWidth = width)
                    drawLine(color, Offset(0f, k * cell), Offset(size.width, k * cell), strokeWidth = width)
                }
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 9) {
                        val index = row * 9 + col
                        SudokuCell(
                            game = game,
                            index = index,
                            selectedValue = selectedValue,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = { onSelect(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCell(
    game: SudokuGame,
    index: Int,
    selectedValue: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val value = game.values[index]
    val given = game.isGiven(index)
    val isSelected = index == game.selected
    val related = game.selected >= 0 &&
        (index / 9 == game.selected / 9 || index % 9 == game.selected % 9)
    val sameNumber = value != 0 && value == selectedValue
    val conflict = value != 0 && SudokuOperations.conflict(game.values, index, value)
    val wrong = index in game.wrong

    val background = when {
        wrong -> colors.errorContainer
        isSelected -> colors.primaryContainer
        sameNumber -> colors.secondaryContainer
        related -> colors.surfaceContainerHigh
        else -> colors.surface
    }
    val description = if (value != 0) {
        stringResource(R.string.sudoku_cell_filled, index / 9 + 1, index % 9 + 1, value)
    } else {
        stringResource(R.string.sudoku_cell_empty, index / 9 + 1, index % 9 + 1)
    }

    Box(
        modifier
            .background(background)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                value.toString(),
                fontSize = 22.sp,
                fontWeight = if (given) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    conflict || wrong -> colors.error
                    given -> colors.onSurface
                    else -> colors.primary
                }
            )
        } else if (game.notes[index].isNotEmpty()) {
            NotesGrid(game.notes[index])
        }
    }
}

@Composable
private fun NotesGrid(notes: Set<Int>) {
    Column(Modifier.fillMaxSize().padding(1.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        for (r in 0 until 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (c in 0 until 3) {
                    val n = r * 3 + c + 1
                    Text(
                        if (n in notes) n.toString() else " ",
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPad(onNumber: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
        Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            for (n in 1..5) NumberKey(n.toString(), Modifier.weight(1f)) { onNumber(n) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            for (n in 6..9) NumberKey(n.toString(), Modifier.weight(1f)) { onNumber(n) }
            NumberKey(stringResource(R.string.sudoku_erase), Modifier.weight(1f)) { onNumber(0) }
        }
    }
}

@Composable
private fun NumberKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(label, maxLines = 1)
    }
}
