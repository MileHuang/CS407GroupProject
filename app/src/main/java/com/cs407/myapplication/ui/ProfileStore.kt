package com.cs407.myapplication.ui


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// DataStore for persisting a single user's profile.
private val Context.profileDataStore by preferencesDataStore("profile_ds")
private val PROFILE_JSON = stringPreferencesKey("profile_json")

@Serializable
data class UserProfile(
    val name: String = "",
    val age: Int? = null,
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val gender: String = "",
    val activityLevel: String = "",
    val goal: String = "",
    val calorieTarget: Int? = null
)

class ProfileStore(private val context: Context) {

    // Observe the current profile as a Flow, defaulting to an empty profile.
    val profileFlow: Flow<UserProfile> = context.profileDataStore.data.map { prefs ->
        val json = prefs[PROFILE_JSON]
        if (json.isNullOrBlank()) {
            UserProfile()
        } else {
            runCatching { Json.decodeFromString<UserProfile>(json) }
                .getOrElse { UserProfile() }
        }
    }

    // Save/overwrite the profile.
    suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[PROFILE_JSON] = Json.encodeToString(profile)
        }
    }
}
