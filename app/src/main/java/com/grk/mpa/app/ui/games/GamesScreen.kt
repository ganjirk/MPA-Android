package com.grk.mpa.app.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card

private enum class Game(@StringRes val titleRes: Int) {
    NUMBER_GUESS(R.string.game_number_guess),
    PONG(R.string.game_pong)
}

@Composable
fun GamesScreen(modifier: Modifier = Modifier) {
    var selectedName by rememberSaveable { mutableStateOf(Game.NUMBER_GUESS.name) }
    val selected = Game.valueOf(selectedName)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Game.entries.forEach { game ->
                FilterChip(
                    selected = game == selected,
                    onClick = { selectedName = game.name },
                    label = { Text(stringResource(game.titleRes)) }
                )
            }
        }
        when (selected) {
            Game.NUMBER_GUESS -> NumberGuessScreen()
            Game.PONG -> PongScreen()
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
