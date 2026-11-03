package com.grk.mpa.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grk.mpa.app.core.DecisionCategory
import com.grk.mpa.app.core.DecisionHistoryEntry
import com.grk.mpa.app.core.DecisionMakerData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DataStoreDecisionMakerRepositoryTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private var testScope: CoroutineScope? = null
    private var testFile: File? = null

    private fun createRepository(): DataStoreDecisionMakerRepository {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        testScope = scope

        val file = File(
            context.cacheDir,
            "decision_maker_test_${UUID.randomUUID()}.preferences_pb"
        )
        testFile = file

        val store: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file }
            )

        return DataStoreDecisionMakerRepository(context, store)
    }

    @After
    fun tearDown() {
        testScope?.cancel()
        testFile?.delete()
    }

    @Test
    fun load_returnsDefaultData_whenNothingHasBeenSaved() = runBlocking {
        val repository = createRepository()

        assertEquals(DecisionMakerData(), repository.load())
    }

    @Test
    fun saveAndLoad_preservesCustomCategoriesAndOptions() = runBlocking {
        val repository = createRepository()

        val expected = DecisionMakerData(
            categories = listOf(
                DecisionCategory(
                    id = "custom-1",
                    name = "Weekend Plans",
                    options = listOf("Reading", "Cycling", "Movies")
                )
            )
        )

        repository.save(expected)

        assertEquals(expected, repository.load())
    }

    @Test
    fun saveAndLoad_preservesHistory() = runBlocking {
        val repository = createRepository()

        val expected = DecisionMakerData(
            history = listOf(
                DecisionHistoryEntry(
                    id = 1L,
                    categoryName = "Morning Routine",
                    winners = listOf("Exercise"),
                    pickedAtMillis = 123456789L
                )
            )
        )

        repository.save(expected)

        assertEquals(expected, repository.load())
    }

    @Test
    fun save_overwritesPreviouslyPersistedData() = runBlocking {
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

        assertEquals(second, repository.load())
    }
}
