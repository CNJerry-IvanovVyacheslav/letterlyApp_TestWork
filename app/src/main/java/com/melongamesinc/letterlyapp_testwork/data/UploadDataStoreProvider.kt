package com.melongamesinc.letterlyapp_testwork.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.uploadDataStore by preferencesDataStore(
    name = "upload_prefs"
)

fun provideUploadDataStore(context: Context): DataStore<Preferences> =
    context.applicationContext.uploadDataStore