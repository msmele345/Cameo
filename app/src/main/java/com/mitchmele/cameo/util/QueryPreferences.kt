@file:Suppress("DEPRECATION")

package com.mitchmele.cameo.util

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.mitchmele.cameo.util.CameoConstants.PREF_LAST_RESULT_ID
import com.mitchmele.cameo.util.CameoConstants.PREF_SEARCH_QUERY
import com.mitchmele.cameo.util.CameoConstants.PRE_IS_POLLING

object QueryPreferences {

    fun getStoredQuery(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_SEARCH_QUERY, "")!!
    }

    fun getLastResultId(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_LAST_RESULT_ID, "")!!
    }

    fun setLastResultId(context: Context, lastResultId: String) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                putString(PREF_LAST_RESULT_ID, lastResultId)
            }
    }

    fun setStoredQuery(context: Context, query: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREF_SEARCH_QUERY, query)
            .apply()
    }

    fun isPolling(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(PRE_IS_POLLING, false)
    }


    fun setPolling(context: Context, isOn: Boolean) {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                putBoolean(PRE_IS_POLLING, isOn)
            }
    }
}