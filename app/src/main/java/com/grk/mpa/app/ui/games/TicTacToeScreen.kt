package com.grk.mpa.app.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.GameRecord
import com.grk.mpa.app.core.Mark
import com.grk.mpa.app.core.MatchWinner
import com.grk.mpa.app.core.NameError
import com.grk.mpa.app.core.TicTacToeStatsOperations
import com.grk.mpa.app.data.DataStoreTicTacToeRepository
import com.grk.mpa.app.ui.theme.MpaDimensions
import java.text.DateFormat
import java.util.Date

private val ticTacToeViewModelFactory = viewModelFactory {
    initializer {
        TicTacToeViewModel(DataStoreTicTacToeRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@Composable
fun TicTacToeScreen(
    modifier: Modifier = Modifier,
    viewModel: TicTacToeViewModel = viewModel(factory = ticTacToeViewModelFactory)
) {
    val local = viewModel.mode == TicTacToeMode.LOCAL
    val outcome = viewModel.outcome
    val leaderboard = TicTacToeStatsOperations.leaderboard(viewModel.stats.players)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MpaDimensions.screenHorizontalPadding),
        contentPadding = PaddingValues(top = MpaDimensions.compactSpacing, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.ttt_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        item(key = "modes") {
            Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                    FilterChip(
                        selected = !local,
                        onClick = { viewModel.onModeChange(TicTacToeMode.COMPUTER) },
                        label = { Text(stringResource(R.string.ttt_mode_computer)) }
                    )
                    FilterChip(
                        selected = local,
                        onClick = { viewModel.onModeChange(TicTacToeMode.LOCAL) },
                        label = { Text(stringResource(R.string.ttt_mode_local)) }
                    )
                }
                Text(
                    stringResource(if (local) R.string.ttt_description_local else R.string.ttt_description_computer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (local) {
            item(key = "names") {
                Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                    OutlinedTextField(
                        value = viewModel.playerXName,
                        onValueChange = { viewModel.onNameChange(Mark.X, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ttt_player1_label)) },
                        singleLine = true,
                        enabled = viewModel.canEditNames,
                        isError = viewModel.nameError != null
                    )
                    OutlinedTextField(
                        value = viewModel.playerOName,
                        onValueChange = { viewModel.onNameChange(Mark.O, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ttt_player2_label)) },
                        singleLine = true,
                        enabled = viewModel.canEditNames,
                        isError = viewModel.nameError != null,
                        supportingText = viewModel.nameError?.let { error ->
                            {
                                Text(
                                    stringResource(
                                        if (error == NameError.SAME) R.string.ttt_error_same_names else R.string.ttt_error_name_length
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }

        item(key = "status") {
            val text = when {
                outcome.isDraw -> stringResource(R.string.ttt_status_draw)
                outcome.winner != null -> if (local) {
                    stringResource(
                        R.string.ttt_status_player_wins,
                        (if (outcome.winner == Mark.X) viewModel.playerXName else viewModel.playerOName).trim()
                    )
                } else {
                    stringResource(if (outcome.winner == Mark.X) R.string.ttt_status_you_win else R.string.ttt_status_computer_wins)
                }
                local -> stringResource(
                    R.string.ttt_status_turn,
                    (if (viewModel.state.nextMark == Mark.X) viewModel.playerXName else viewModel.playerOName).trim(),
                    viewModel.state.nextMark.name
                )
                else -> stringResource(R.string.ttt_status_your_turn)
            }
            Text(
                text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (outcome.isOver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
        }

        item(key = "board") {
            val board = viewModel.state.board
            Column(
                Modifier.fillMaxWidth().widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
            ) {
                for (row in 0 until 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                        for (column in 0 until 3) {
                            val cell = row * 3 + column + 1
                            val mark = board[cell]
                            val inWinningLine = cell in outcome.line
                            val description = stringResource(
                                R.string.ttt_cell_description,
                                cell,
                                mark?.name ?: stringResource(R.string.ttt_cell_empty)
                            )
                            Card(
                                onClick = { viewModel.onCellClick(cell) },
                                enabled = mark == null && !outcome.isOver && viewModel.isLoaded,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .semantics { contentDescription = description },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (inWinningLine) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    disabledContainerColor = if (inWinningLine) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (mark != null) {
                                        Text(
                                            mark.name,
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mark == Mark.X) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "new-game") {
            Button(onClick = viewModel::newGame) { Text(stringResource(R.string.ttt_new_game)) }
        }

        item(key = "stats-title") {
            Text(
                stringResource(if (local) R.string.ttt_leaderboard_title else R.string.ttt_your_record_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (!local) {
            item(key = "computer-record") { RecordCard(viewModel.stats.computer) }
        } else {
            if (leaderboard.isEmpty()) {
                item(key = "empty-leaderboard") {
                    Text(
                        stringResource(R.string.ttt_leaderboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(leaderboard, key = { it.normalizedName }) { player ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(player.displayName, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(
                                R.string.ttt_record_line,
                                player.record.games, player.record.wins, player.record.losses, player.record.draws
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val recent = viewModel.stats.recentMatches.take(20)
            if (recent.isNotEmpty()) {
                item(key = "recent-title") {
                    Text(
                        stringResource(R.string.ttt_recent_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(recent, key = { "${it.playedAtMillis}-${it.playerX}-${it.playerO}" }) { match ->
                    val result = when (match.winner) {
                        MatchWinner.X -> stringResource(R.string.ttt_recent_won, match.playerX)
                        MatchWinner.O -> stringResource(R.string.ttt_recent_won, match.playerO)
                        MatchWinner.DRAW -> stringResource(R.string.ttt_status_draw)
                    }
                    Column {
                        Text(
                            stringResource(R.string.ttt_recent_players, match.playerX, match.playerO),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            result + " · " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(match.playedAtMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: GameRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RecordCell(stringResource(R.string.ttt_played), record.games)
            RecordCell(stringResource(R.string.ttt_won), record.wins)
            RecordCell(stringResource(R.string.ttt_lost), record.losses)
            RecordCell(stringResource(R.string.ttt_drawn), record.draws)
        }
    }
}

@Composable
private fun RecordCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
