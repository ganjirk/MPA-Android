package com.grk.mpa.app.ui.games

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.grk.mpa.app.core.MatchDifficulty
import com.grk.mpa.app.core.MatchHistory
import com.grk.mpa.app.core.MatchResult
import com.grk.mpa.app.data.DataStoreMatchCardsRepository
import com.grk.mpa.app.ui.theme.MpaDimensions
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private const val COLUMNS = 4

private val matchCardsViewModelFactory = viewModelFactory {
    initializer {
        MatchCardsViewModel(DataStoreMatchCardsRepository(checkNotNull(this[APPLICATION_KEY])))
    }
}

@StringRes
private fun MatchDifficulty.chipRes(): Int = when (this) {
    MatchDifficulty.EASY -> R.string.matchcards_easy
    MatchDifficulty.MEDIUM -> R.string.matchcards_medium
    MatchDifficulty.HARD -> R.string.matchcards_hard
}

@StringRes
private fun MatchDifficulty.nameRes(): Int = when (this) {
    MatchDifficulty.EASY -> R.string.matchcards_difficulty_easy
    MatchDifficulty.MEDIUM -> R.string.matchcards_difficulty_medium
    MatchDifficulty.HARD -> R.string.matchcards_difficulty_hard
}

@Composable
fun MatchCardsScreen(
    modifier: Modifier = Modifier,
    viewModel: MatchCardsViewModel = viewModel(factory = matchCardsViewModelFactory)
) {
    val game = viewModel.game

    // Two different cards stay face up for a moment, then turn back
    LaunchedEffect(game.mismatchPending, game.moves) {
        if (game.mismatchPending) {
            delay(750)
            viewModel.hideMismatch()
        }
    }
    // The clock on screen
    LaunchedEffect(viewModel.status == MatchStatus.DONE) {
        while (viewModel.status != MatchStatus.DONE) {
            viewModel.tick()
            delay(500)
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Text(
            stringResource(R.string.matchcards_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            stringResource(R.string.matchcards_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MatchDifficulty.entries.forEach { level ->
                FilterChip(
                    selected = viewModel.difficulty == level,
                    onClick = { viewModel.onDifficultyChange(level) },
                    label = { Text(stringResource(level.chipRes())) }
                )
            }
        }
        OutlinedButton(onClick = viewModel::newGame) { Text(stringResource(R.string.matchcards_restart)) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)) {
            Stat(stringResource(R.string.matchcards_moves), game.moves.toString(), Modifier.weight(1f))
            Stat(
                stringResource(R.string.matchcards_matched),
                stringResource(R.string.matchcards_progress, game.matches, game.difficulty.pairs),
                Modifier.weight(1f)
            )
            Stat(stringResource(R.string.matchcards_time), MatchHistory.formatTime(viewModel.elapsedSeconds), Modifier.weight(1f))
        }
        Text(
            stringResource(
                when (viewModel.status) {
                    MatchStatus.PROMPT -> R.string.matchcards_status_prompt
                    MatchStatus.MATCH -> R.string.matchcards_status_match
                    MatchStatus.NO_MATCH -> R.string.matchcards_status_no_match
                    MatchStatus.DONE -> R.string.matchcards_status_done
                }
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            game.cards.indices.chunked(COLUMNS).forEach { rowIndices ->
                Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
                    rowIndices.forEach { index ->
                        CardTile(
                            number = index + 1,
                            symbol = game.cards[index].symbol,
                            revealed = game.isRevealed(index),
                            matched = game.cards[index].matched,
                            onClick = { viewModel.onCardClick(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(COLUMNS - rowIndices.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        if (viewModel.status == MatchStatus.DONE) CompleteCard(viewModel)
        HistoryCard(viewModel.history)
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(10.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CardTile(
    number: Int,
    symbol: String,
    revealed: Boolean,
    matched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val description = when {
        matched -> stringResource(R.string.matchcards_card_matched, symbol)
        revealed -> stringResource(R.string.matchcards_card_shown, number, symbol)
        else -> stringResource(R.string.matchcards_card_hidden, number)
    }
    val shape = MaterialTheme.shapes.medium
    val container = when {
        matched -> MaterialTheme.colorScheme.tertiaryContainer
        revealed -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier
            .aspectRatio(1f)
            .background(container, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(enabled = !matched, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (revealed) symbol else "?",
            fontSize = if (revealed) 30.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (revealed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun CompleteCard(viewModel: MatchCardsViewModel) {
    val game = viewModel.game
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(
                stringResource(R.string.matchcards_complete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(
                    R.string.matchcards_summary,
                    game.moves,
                    MatchHistory.formatTime(viewModel.elapsedSeconds),
                    stringResource(game.difficulty.nameRes())
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(R.string.matchcards_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(onClick = viewModel::newGame) { Text(stringResource(R.string.matchcards_play_again)) }
        }
    }
}

@Composable
private fun HistoryCard(history: List<MatchResult>) {
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.matchcards_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.matchcards_best_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            MatchDifficulty.entries.forEach { level ->
                val best = MatchHistory.best(history, level)
                Text(
                    stringResource(level.nameRes()) + ": " +
                        if (best == null) stringResource(R.string.matchcards_best_none)
                        else stringResource(R.string.matchcards_best_line, best.moves, best.elapsedSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.matchcards_recent_title),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (history.isEmpty()) {
                Text(
                    stringResource(R.string.matchcards_recent_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            history.forEach { r ->
                Text(
                    stringResource(
                        R.string.matchcards_recent_line,
                        format.format(Date(r.completedAtMillis)),
                        stringResource(r.difficulty.nameRes()),
                        r.moves,
                        r.elapsedSeconds
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
