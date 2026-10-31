package com.grk.mpa.app.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grk.mpa.app.R
import com.grk.mpa.app.ui.theme.MpaDimensions
import com.grk.mpa.app.data.DataStoreGuessStatsRepository
import com.grk.mpa.app.core.GuessLevel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.compose.ui.res.pluralStringResource

private enum class Game(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val emoji: String
) {
    NUMBER_GUESS(R.string.game_number_guess, R.string.game_number_guess_description, "🎯"),
    PONG(R.string.game_pong, R.string.game_pong_description, "🏓"),
    TIC_TAC_TOE(R.string.game_tic_tac_toe, R.string.game_tic_tac_toe_description, "⭕"),
    LUDO(R.string.game_ludo, R.string.game_ludo_description, "🎲"),
    SUDOKU(R.string.game_sudoku, R.string.game_sudoku_description, "🔢")
}

@Composable
fun GamesScreen(modifier: Modifier = Modifier) {
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = Game.entries.firstOrNull { it.name == selectedName }

    if (selected == null) {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MpaDimensions.screenHorizontalPadding)
                .padding(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.games_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.games_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameTile(
                    game = Game.NUMBER_GUESS,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = Game.NUMBER_GUESS.name }
                )
                GameTile(
                    game = Game.PONG,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = Game.PONG.name }
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameTile(
                    game = Game.TIC_TAC_TOE,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = Game.TIC_TAC_TOE.name }
                )
                GameTile(
                    game = Game.LUDO,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = Game.LUDO.name }
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameTile(
                    game = Game.SUDOKU,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = Game.SUDOKU.name }
                )
                Spacer(Modifier.weight(1f))
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MpaDimensions.screenHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { selectedName = null }) {
                    Text(stringResource(R.string.games_back))
                }
                Text(
                    stringResource(selected.titleRes),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            when (selected) {
                Game.NUMBER_GUESS -> NumberGuessScreen()
                Game.PONG -> PongScreen()
                Game.TIC_TAC_TOE -> TicTacToeScreen()
                Game.LUDO -> LudoScreen()
                Game.SUDOKU -> SudokuScreen()
            }
        }
    }
}

@Composable
private fun GameTile(
    game: Game,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 174.dp),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(game.emoji, style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                stringResource(game.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(game.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2
            )
            Text(
                stringResource(R.string.games_play_now),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private val numberGuessViewModelFactory = viewModelFactory {
    initializer {
        NumberGuessViewModel(DataStoreGuessStatsRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
fun NumberGuessScreen(
    modifier: Modifier = Modifier,
    viewModel: NumberGuessViewModel = viewModel(factory = numberGuessViewModelFactory)
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(bottom = MpaDimensions.screenTopPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.guess_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GuessLevel.entries.forEach { level ->
                FilterChip(
                    selected = level == viewModel.level,
                    onClick = { viewModel.onLevelChange(level) },
                    label = { Text(level.label) }
                )
            }
        }
        Text(
            stringResource(R.string.guess_instructions, viewModel.maxNumber, viewModel.level.chances),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = viewModel.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guess_input_label)) },
            singleLine = true,
            enabled = !viewModel.isFinished,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::submit, enabled = !viewModel.isFinished) {
                Text(stringResource(R.string.guess_submit))
            }
            OutlinedButton(onClick = viewModel::newGame) {
                Text(stringResource(R.string.guess_new_game))
            }
        }
        Text(
            pluralStringResource(R.plurals.guess_chances_left, viewModel.chancesLeft, viewModel.chancesLeft),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        feedbackText(viewModel)?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = if (viewModel.feedback == GuessFeedback.CORRECT) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
        if (viewModel.isLastChance) {
            Text(
                stringResource(R.string.guess_last_chance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        StatsCard(viewModel)
    }
}

@Composable
private fun StatsCard(viewModel: NumberGuessViewModel) {
    val summary = viewModel.summary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.guess_stats_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.guess_stats_summary, summary.total, summary.wins, summary.winRatePercent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val best = summary.bestAttempts[viewModel.level]
            Text(
                if (best != null) stringResource(R.string.guess_stats_best, viewModel.level.label, best)
                else stringResource(R.string.guess_stats_no_win, viewModel.level.label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun feedbackText(viewModel: NumberGuessViewModel): String? = when (viewModel.feedback) {
    GuessFeedback.NONE -> null
    GuessFeedback.INVALID -> stringResource(R.string.guess_invalid)
    GuessFeedback.OUT_OF_RANGE -> stringResource(R.string.guess_out_of_range, viewModel.maxNumber)
    GuessFeedback.TOO_LOW -> stringResource(R.string.guess_too_low)
    GuessFeedback.TOO_HIGH -> stringResource(R.string.guess_too_high)
    GuessFeedback.CORRECT ->
        pluralStringResource(R.plurals.guess_correct, viewModel.attempts, viewModel.attempts)
    GuessFeedback.LOST -> stringResource(R.string.guess_lost, viewModel.revealedTarget ?: 0)
}
