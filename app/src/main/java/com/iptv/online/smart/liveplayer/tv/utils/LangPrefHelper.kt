package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context
import android.content.SharedPreferences

class LangPrefHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)

    companion object {
        private val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    }

    var language: String
        get() = prefs.getString(SELECTED_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(SELECTED_LANGUAGE, value).apply()
}