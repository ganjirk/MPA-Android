package com.grk.mpa.app.ui.recap

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.Money
import com.grk.mpa.app.core.Moods
import com.grk.mpa.app.core.RecapHighlight
import com.grk.mpa.app.core.WeekRecap
import com.grk.mpa.app.data.DeviceRecapSource
import com.grk.mpa.app.ui.theme.MpaDimensions
import java.time.format.TextStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

private val recapViewModelFactory = viewModelFactory {
    initializer { RecapViewModel(DeviceRecapSource(checkNotNull(this[APPLICATION_KEY]))) }
}

private val moodLabels = mapOf(
    1 to R.string.recap_mood_1, 2 to R.string.recap_mood_2, 3 to R.string.recap_mood_3,
    4 to R.string.recap_mood_4, 5 to R.string.recap_mood_5
)

/** Whole rupees read better in a summary; keep the paise only when there are some. */
private fun rupees(paise: Long): String =
    if (paise % 100 == 0L) "${Money.SYMBOL}${paise / 100}" else Money.format(paise)

@Composable
fun RecapScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecapViewModel = viewModel(factory = recapViewModelFactory)
) {
    LaunchedEffect(Unit) { viewModel.refresh() }
    val recap = viewModel.recap

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(top = MpaDimensions.compactSpacing, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.support_back)) }
        if (recap == null) return@Column

        val range = DateTimeFormatter.ofPattern("d MMM")
        Text(
            stringResource(if (recap.isCurrent) R.string.recap_title_current else R.string.recap_title_past),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = viewModel::previousWeek) { Text("‹ " + stringResource(R.string.recap_prev)) }
            Text(
                "${recap.start.format(range)} – ${recap.end.format(range)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = viewModel::nextWeek, enabled = viewModel.canGoNext) {
                Text(stringResource(R.string.recap_next) + " ›")
            }
        }

        if (recap.isCurrent) MoodPicker(selected = viewModel.todayMood, onPick = viewModel::setMood)

        if (recap.isEmpty) {
            Text(stringResource(R.string.recap_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            StatCards(recap)
            Highlights(recap)
            DayByDay(recap)
            if (recap.topCategories.isNotEmpty()) MoneySection(recap)
        }
        recap.budget?.let { Budget(it.spentPaise, it.amountPaise, it.percent, it.over) }
        Text(
            stringResource(R.string.recap_device_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(@StringRes title: Int, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun MoodPicker(selected: Int?, onPick: (Int) -> Unit) {
    SectionCard(R.string.recap_mood_title) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Moods.RANGE.forEach { mood ->
                val chosen = selected == mood
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { onPick(mood) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(Moods.EMOJI.getValue(mood), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(moodLabels.getValue(mood)), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (selected != null) {
            Text(
                stringResource(R.string.recap_mood_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCards(recap: WeekRecap) {
    Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
        Stat(Modifier.weight(1f), R.string.recap_stat_tasks, recap.tasksDone.toString())
        Stat(Modifier.weight(1f), R.string.recap_stat_spent, rupees(recap.spentPaise))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
        Stat(Modifier.weight(1f), R.string.recap_stat_games, recap.games.toString())
        Stat(Modifier.weight(1f), R.string.recap_stat_active, stringResource(R.string.recap_stat_active_value, recap.activeDays))
    }
}

@Composable
private fun Stat(modifier: Modifier, @StringRes label: Int, value: String) {
    Card(
        modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Highlights(recap: WeekRecap) {
    if (recap.highlights.isEmpty()) return
    SectionCard(R.string.recap_highlights) {
        recap.highlights.forEach { Text("• " + highlightText(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun highlightText(h: RecapHighlight): String = when (h) {
    is RecapHighlight.TasksFinished -> stringResource(R.string.recap_hl_tasks_finished, h.count, h.previous)
    is RecapHighlight.TasksAdded -> stringResource(R.string.recap_hl_tasks_added, h.count)
    is RecapHighlight.Spent -> {
        val money = rupees(h.totalPaise)
        val change = h.changePercent
        val base = when {
            change == null || change == 0 -> stringResource(R.string.recap_hl_spent, money)
            change > 0 -> stringResource(R.string.recap_hl_spent_up, money, change)
            else -> stringResource(R.string.recap_hl_spent_down, money, -change)
        }
        val top = h.topCategory
        if (top != null) base + " " + stringResource(R.string.recap_hl_spent_top, top.label, rupees(h.topPaise)) else base
    }
    is RecapHighlight.Games -> stringResource(R.string.recap_hl_games, h.count, h.wins)
    is RecapHighlight.Active -> stringResource(R.string.recap_hl_active, h.days, h.of)
    is RecapHighlight.AverageMood -> stringResource(
        R.string.recap_hl_mood,
        Moods.EMOJI.getValue(h.mood),
        stringResource(moodLabels.getValue(h.mood))
    )
}

@Composable
private fun DayByDay(recap: WeekRecap) {
    SectionCard(R.string.recap_by_day) {
        recap.days.forEach { day ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.padding(end = 8.dp).weight(0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                )
                Bar(Modifier.weight(3f), day.barPercent, dim = day.isFuture)
                Text(
                    if (day.spentPaise > 0) rupees(day.spentPaise) else "–",
                    modifier = Modifier.weight(1.4f).padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(day.mood?.let { Moods.EMOJI.getValue(it) } ?: "", modifier = Modifier.weight(0.6f))
            }
        }
    }
}

@Composable
private fun MoneySection(recap: WeekRecap) {
    SectionCard(R.string.recap_money) {
        recap.topCategories.forEach { share ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(share.category.label, style = MaterialTheme.typography.bodyMedium)
                    Text("${rupees(share.totalPaise)} · ${share.percent}%", style = MaterialTheme.typography.labelMedium)
                }
                Bar(Modifier.fillMaxWidth(), share.percent)
            }
        }
    }
}

@Composable
private fun Budget(spent: Long, amount: Long, percent: Int, over: Boolean) {
    SectionCard(R.string.recap_budget) {
        Bar(Modifier.fillMaxWidth(), percent, warn = over)
        Text(stringResource(R.string.recap_budget_line, rupees(spent), rupees(amount)), style = MaterialTheme.typography.bodyMedium)
        if (over) Text(stringResource(R.string.recap_budget_over), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Bar(modifier: Modifier, percent: Int, dim: Boolean = false, warn: Boolean = false) {
    Box(
        modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                .background(
                    when {
                        warn -> MaterialTheme.colorScheme.error
                        dim -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
        )
    }
}
