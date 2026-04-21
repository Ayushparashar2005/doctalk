package com.doctalk.app.utils

import android.content.Context

object AppPreferences {

    fun getSelectedGroqModel(context: Context): String {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(Constants.PREF_SELECTED_GROQ_MODEL, Constants.DEFAULT_GROQ_MODEL)
            ?: Constants.DEFAULT_GROQ_MODEL
    }

    fun setSelectedGroqModel(context: Context, model: String) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(Constants.PREF_SELECTED_GROQ_MODEL, model)
            .apply()
    }
}