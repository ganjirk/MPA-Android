package com.grk.mpa.app.ui.recap

import com.grk.mpa.app.core.RecapInput
import com.grk.mpa.app.core.Task
import com.grk.mpa.app.data.RecapSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

private class FakeRecapSource(var input: RecapInput = RecapInput()) : RecapSource {
    var savedMoods: Map<LocalDate, Int>? = null
    override suspend fun load() = input
    override suspend fun saveMoods(moods: Map<LocalDate, Int>) {
        savedMoods = moods
        input = input.copy(moods = moods)
    }
}

class RecapViewModelTest {
    private var today = LocalDate.of(2026, 10, 28)
    private val monday = LocalDate.of(2026, 10, 26)

    private fun viewModel(source: FakeRecapSource = FakeRecapSource()) =
        RecapViewModel(source, CoroutineScope(Dispatchers.Unconfined), { today }, ZoneOffset.UTC)

    @Test
    fun opensOnTheCurrentWeek() {
        val vm = viewModel()
        assertEquals(monday, vm.weekStart)
        assertNotNull(vm.recap)
        assertTrue(vm.recap!!.isCurrent)
        assertFalse(vm.canGoNext)
    }

    @Test
    fun movesBetweenWeeksButNeverIntoTheFuture() {
        val vm = viewModel()
        vm.nextWeek()
        assertEquals(monday, vm.weekStart)
        vm.previousWeek()
        assertEquals(monday.minusDays(7), vm.weekStart)
        assertFalse(vm.recap!!.isCurrent)
        assertTrue(vm.canGoNext)
        vm.nextWeek()
        assertEquals(monday, vm.weekStart)
    }

    @Test
    fun moodIsSavedAndCanBeChanged() {
        val source = FakeRecapSource()
        val vm = viewModel(source)
        vm.setMood(4)
        assertEquals(4, vm.todayMood)
        assertEquals(mapOf(today to 4), source.savedMoods)
        assertEquals(4, vm.recap!!.days[2].mood)
        vm.setMood(2)
        assertEquals(mapOf(today to 2), source.savedMoods)
        vm.setMood(9)
        assertEquals(2, vm.todayMood)
    }

    @Test
    fun refreshPicksUpNewData() {
        val source = FakeRecapSource()
        val vm = viewModel(source)
        assertTrue(vm.recap!!.isEmpty)
        source.input = RecapInput(tasks = listOf(Task(1, "a", false, today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 1000)))
        vm.refresh()
        assertEquals(1, vm.recap!!.tasksAdded)
    }

    @Test
    fun aScreenLeftOpenOvernightUsesTheNewDate() {
        val vm = viewModel()
        today = today.plusDays(1)
        vm.setMood(3)
        assertEquals(3, vm.todayMood)
        assertTrue(vm.recap!!.days[3].isToday)
    }
}
