package com.cs407.myapplication.viewModels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// DataStore
private val Context.profileDataStore by preferencesDataStore("profile_ds")
private val PROFILE_JSON = stringPreferencesKey("profile_json")

class ProfileStore(private val context: Context) {

    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        val json = prefs[PROFILE_JSON]
        if (json.isNullOrBlank()) {
            UserProfile()
        } else {
            Json.decodeFromString<UserProfile>(json)
        }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[PROFILE_JSON] = Json.encodeToString(profile)
        }
    }
}
