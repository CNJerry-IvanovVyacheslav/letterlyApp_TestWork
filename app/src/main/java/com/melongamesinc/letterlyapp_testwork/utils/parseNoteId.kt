package com.melongamesinc.letterlyapp_testwork.utils

import org.json.JSONObject

fun parseNoteId(json: String?): Int {
    if (json.isNullOrBlank()) {
        throw IllegalArgumentException("Empty response body")
    }

    val jsonObject = JSONObject(json)
    return jsonObject.getInt("noteId")
}