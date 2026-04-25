package com.doctalk.app.utils

import android.content.Context
import com.doctalk.app.network.groq.GroqModels

object AppPreferences {

    fun getSelectedGroqModel(context: Context): String {
        val storedModel = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(Constants.PREF_SELECTED_GROQ_MODEL, Constants.DEFAULT_GROQ_MODEL)
            ?: Constants.DEFAULT_GROQ_MODEL

        return when (storedModel) {
            GroqModels.LLAMA31_8B_INSTANT,
            GroqModels.LLAMA33_70B_VERSATILE -> storedModel
            "llama3-8b-8192" -> Constants.DEFAULT_GROQ_MODEL
            else -> Constants.DEFAULT_GROQ_MODEL
        }
    }

    fun setSelectedGroqModel(context: Context, model: String) {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(Constants.PREF_SELECTED_GROQ_MODEL, model)
            .apply()
    }
}