package com.melongamesinc.letterlyapp_testwork.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val RESULT_KEY = stringPreferencesKey("last_upload_result")

class UploadResultStore(
    private val context: Context
) {
    private val dataStore: DataStore<Preferences> = provideUploadDataStore(context)

    fun getResult(): Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[RESULT_KEY]
        }
    suspend fun saveSuccess(noteId: Int) {
        dataStore.edit { prefs ->
            prefs[RESULT_KEY] =
                """{"status":"SUCCESS","noteId":$noteId}"""
        }
    }

    suspend fun saveError(message: String) {
        dataStore.edit { prefs ->
            prefs[RESULT_KEY] =
                """{"status":"ERROR","message":"$message"}"""
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
