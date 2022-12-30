package com.unipass.core

import android.content.Context

object SharedPreferenceUtil {
    private const val UNIPASS_PREF = "UNIPASS_PREF"

    public const val SESSION_KEY = "SESSION"

    fun saveItem(context: Context, key: String, value: String) {
        val sharedPreference = context.getSharedPreferences(UNIPASS_PREF, Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()
        editor.putString(key, value)
        editor.commit()
    }

    fun getItem(context: Context, key: String): String? {
        val sharedPreference = context.getSharedPreferences(UNIPASS_PREF, Context.MODE_PRIVATE)
        return sharedPreference.getString(key, "")
    }

    fun deleteItem(context: Context, key: String) {
        val sharedPreference = context.getSharedPreferences(UNIPASS_PREF, Context.MODE_PRIVATE)
        sharedPreference.edit().remove(key)?.apply()
    }
}