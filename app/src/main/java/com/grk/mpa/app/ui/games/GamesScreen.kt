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

@Composable
fun NumberGuessScreen(
    modifier: Modifier = Modifier,
    viewModel: NumberGuessViewModel = viewModel()
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            stringResource(R.string.guess_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.guess_instructions, viewModel.range.first, viewModel.range.last),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = viewModel.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.guess_input_label)) },
            singleLine = true,
            enabled = !viewModel.isSolved,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.submit() })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = viewModel::submit, enabled = !viewModel.isSolved) {
                Text(stringResource(R.string.guess_submit))
            }
            OutlinedButton(onClick = viewModel::reset) {
                Text(stringResource(R.string.guess_new_game))
            }
        }
        feedbackText(viewModel)?.let { message ->
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = if (viewModel.isSolved) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
        if (viewModel.attempts > 0) {
            Text(
                stringResource(R.string.guess_attempts, viewModel.attempts),
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
    GuessFeedback.OUT_OF_RANGE ->
        stringResource(R.string.guess_out_of_range, viewModel.range.first, viewModel.range.last)
    GuessFeedback.TOO_LOW -> stringResource(R.string.guess_too_low)
    GuessFeedback.TOO_HIGH -> stringResource(R.string.guess_too_high)
    GuessFeedback.CORRECT -> stringResource(R.string.guess_correct)
}
