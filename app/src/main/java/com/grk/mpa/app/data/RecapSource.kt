package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grk.mpa.app.core.MoodCodec
import com.grk.mpa.app.core.RecapInput
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Where "Your week" reads its data from. The UI only depends on this interface. */
interface RecapSource {
    suspend fun load(): RecapInput
    suspend fun saveMoods(moods: Map<LocalDate, Int>)
}

private val Context.moodDataStore by preferencesDataStore(name = "moods")

/** Reads what the other screens already keep on the device, and stores the daily mood. Nothing leaves the phone. */
class DeviceRecapSource(context: Context) : RecapSource {
    private val app = context.applicationContext
    private val moodStore = app.moodDataStore
    private val moodKey = stringPreferencesKey("moods_v1")

    override suspend fun load(): RecapInput = RecapInput(
        tasks = DataStoreTaskRepository(app).load(),
        expenses = DataStoreExpenseRepository(app).load(),
        guessRecords = DataStoreGuessStatsRepository(app).load(),
        matchResults = DataStoreMatchCardsRepository(app).load(),
        ticTacToe = DataStoreTicTacToeRepository(app).load(),
        moods = MoodCodec.decode(moodStore.data.first()[moodKey].orEmpty())
    )

    override suspend fun saveMoods(moods: Map<LocalDate, Int>) {
        moodStore.edit { it[moodKey] = MoodCodec.encode(moods) }
    }
}
