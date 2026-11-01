package com.grk.mpa.app.ui.recap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.Moods
import com.grk.mpa.app.core.RecapInput
import com.grk.mpa.app.core.WeekRecap
import com.grk.mpa.app.core.WeekRecaps
import com.grk.mpa.app.data.RecapSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * @param coroutineScope scope used for reading and saving; null means [viewModelScope]. Tests pass their own.
 * @param today the current date, asked again every time so a screen left open overnight stays right.
 */
class RecapViewModel(
    private val source: RecapSource,
    private val coroutineScope: CoroutineScope? = null,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    private var input = RecapInput()

    var weekStart by mutableStateOf(WeekRecaps.weekStart(today()))
        private set
    var recap by mutableStateOf<WeekRecap?>(null)
        private set
    var todayMood by mutableStateOf<Int?>(null)
        private set

    val canGoNext: Boolean get() = weekStart.isBefore(WeekRecaps.weekStart(today()))

    init {
        refresh()
    }

    /** Reads the device data again; the screen calls this whenever it opens. */
    fun refresh() {
        scope.launch {
            input = source.load()
            rebuild()
        }
    }

    fun previousWeek() {
        weekStart = weekStart.minusDays(7)
        rebuild()
    }

    fun nextWeek() {
        if (!canGoNext) return
        weekStart = weekStart.plusDays(7)
        rebuild()
    }

    /** One tap a day: how today felt. Tapping again changes it. */
    fun setMood(mood: Int) {
        if (mood !in Moods.RANGE) return
        val moods = input.moods + (today() to mood)
        input = input.copy(moods = moods)
        rebuild()
        scope.launch { source.saveMoods(moods) }
    }

    private fun rebuild() {
        val now = today()
        recap = WeekRecaps.build(input, weekStart, now, zone)
        todayMood = input.moods[now]
    }
}
