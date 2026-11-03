
package com.grk.mpa.app.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.grk.mpa.app.core.DecisionCategory
import com.grk.mpa.app.core.DecisionHistoryEntry
import com.grk.mpa.app.core.DecisionMakerData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DataStoreDecisionMakerRepositoryTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun createRepository(): DataStoreDecisionMakerRepository {
        return DataStoreDecisionMakerRepository(context)
    }

    @Test
    fun load_returnsDefaultData_whenNothingHasBeenSaved() = runBlocking {
        clearStore()

        val result = createRepository().load()

        assertEquals(DecisionMakerData(), result)
    }

    @Test
    fun saveAndLoad_preservesCustomCategoriesAndOptions() = runBlocking {
        clearStore()

        val expected = DecisionMakerData(
            categories = listOf(
                DecisionCategory(
                    id = "custom-1",
                    name = "Weekend Plans",
                    options = listOf("Reading", "Cycling", "Movies")
                )
            )
        )

        val repository = createRepository()
        repository.save(expected)

        assertEquals(expected, createRepository().load())
    }

    @Test
    fun saveAndLoad_preservesHistory() = runBlocking {
        clearStore()

        val expected = DecisionMakerData(
            history = listOf(
                DecisionHistoryEntry(
                    categoryId = "morning",
                    categoryName = "Morning Routine",
                    winners = listOf("Exercise")
                )
            )
        )

        val repository = createRepository()
        repository.save(expected)

        assertEquals(expected, createRepository().load())
    }

    @Test
    fun save_overwritesPreviouslyPersistedData() = runBlocking {
        clearStore()

        val repository = createRepository()

        val first = DecisionMakerData()
        val second = DecisionMakerData(
            categories = listOf(
                DecisionCategory(
                    id = "custom-2",
                    name = "Food",
                    options = listOf("Dosa", "Biryani")
                )
            )
        )

        repository.save(first)
        repository.save(second)

        assertEquals(second, createRepository().load())
    }

    private fun clearStore() {
        val file: File = context.preferencesDataStoreFile("decision_maker")
        if (file.exists()) {
            assertTrue("Could not delete test DataStore file", file.delete())
        }
    }
}

