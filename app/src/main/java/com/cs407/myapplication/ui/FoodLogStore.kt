package com.cs407.myapplication.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("food_logs_ds")
private val LOGS_JSON = stringPreferencesKey("logs_json")

@Serializable
data class FoodItem(val name: String, val calories: Int)

typealias DayKey = String // yyyy-MM-dd

class FoodLogStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun readAll(): MutableMap<DayKey, MutableList<FoodItem>> {
        val prefs = context.dataStore.data.first()
        val current = prefs[LOGS_JSON] ?: "{}"
        val map: Map<DayKey, List<FoodItem>> =
            runCatching {
                json.decodeFromString<Map<DayKey, List<FoodItem>>>(current)
            }.getOrElse { emptyMap() }
        return map.mapValues { it.value.toMutableList() }.toMutableMap()
    }

    private suspend fun writeAll(all: Map<DayKey, List<FoodItem>>) {
        val txt = json.encodeToString<Map<DayKey, List<FoodItem>>>(all)
        context.dataStore.edit { it[LOGS_JSON] = txt }
    }

    fun observeDay(day: DayKey): Flow<List<FoodItem>> =
        context.dataStore.data.map { prefs ->
            val txt = prefs[LOGS_JSON] ?: "{}"
            val map: Map<DayKey, List<FoodItem>> =
                runCatching {
                    json.decodeFromString<Map<DayKey, List<FoodItem>>>(txt)
                }.getOrElse { emptyMap() }
            map[day] ?: emptyList()
        }

    suspend fun addItem(day: DayKey, item: FoodItem) {
        val all = readAll()
        val list = all.getOrPut(day) { mutableListOf() }
        list.add(0, item)
        writeAll(all)
    }

    suspend fun removeItem(day: DayKey, index: Int) {
        val all = readAll()
        all[day]?.let {
            if (index in it.indices) {
                it.removeAt(index)
                if (it.isEmpty()) all.remove(day)
                writeAll(all)
            }
        }
    }
}
