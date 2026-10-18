package ai.movenova.fortuneteller.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Local history store. Mirrors iOS PalmStore: newest first, persisted as a single JSON file so
 * the list survives process death without a database migration surface.
 */
class PalmStore(context: Context) {

    private val file = File(context.filesDir, "saved_readings.json")
    private val _readings = MutableStateFlow<List<SavedReading>>(emptyList())
    val readings: StateFlow<List<SavedReading>> = _readings.asStateFlow()

    init {
        _readings.value = load()
    }

    fun save(reading: SavedReading) {
        _readings.value = listOf(reading) + _readings.value
        persist()
    }

    fun delete(id: String) {
        _readings.value = _readings.value.filterNot { it.id == id }
        persist()
    }

    fun clearAll() {
        _readings.value = emptyList()
        persist()
    }

    private fun load(): List<SavedReading> = runCatching {
        if (!file.exists()) return emptyList()
        PalmJson.json.decodeFromString<List<SavedReading>>(file.readText())
    }.getOrDefault(emptyList())

    private fun persist() {
        // Atomic-ish write: a partial write must not wipe the whole history file.
        runCatching {
            val temp = File(file.absolutePath + ".tmp")
            temp.writeText(PalmJson.json.encodeToString(_readings.value))
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }
}
