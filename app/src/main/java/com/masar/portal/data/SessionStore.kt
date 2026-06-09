package com.masar.portal.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "masar_prefs")

class SessionStore(private val context: Context) {
    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_TOKEN    = stringPreferencesKey("token")
        private val KEY_NID      = stringPreferencesKey("national_id")
        private val KEY_NAME     = stringPreferencesKey("driver_name")
        private val KEY_CID      = stringPreferencesKey("courier_id")
    }

    val baseUrlFlow: Flow<String?> = context.dataStore.data.map { it[KEY_BASE_URL] }
    val tokenFlow:   Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val nidFlow:     Flow<String?> = context.dataStore.data.map { it[KEY_NID] }
    val nameFlow:    Flow<String?> = context.dataStore.data.map { it[KEY_NAME] }
    val cidFlow:     Flow<String?> = context.dataStore.data.map { it[KEY_CID] }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = url }
    }

    suspend fun saveLogin(token: String, nid: String, name: String, cid: String) {
        context.dataStore.edit { p ->
            p[KEY_TOKEN] = token
            p[KEY_NID]   = nid
            p[KEY_NAME]  = name
            p[KEY_CID]   = cid
        }
    }

    suspend fun clearLogin() {
        context.dataStore.edit { p ->
            p.remove(KEY_TOKEN); p.remove(KEY_NID); p.remove(KEY_NAME); p.remove(KEY_CID)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    /** Snapshot للوصول السريع المتزامن */
    suspend fun snapshot(): Session {
        val baseUrl = baseUrlFlow.first()
        val token   = tokenFlow.first()
        val nid     = nidFlow.first()
        val name    = nameFlow.first()
        val cid     = cidFlow.first()
        return Session(baseUrl, token, nid, name, cid)
    }
}

data class Session(
    val baseUrl: String?,
    val token: String?,
    val nid: String?,
    val name: String?,
    val cid: String?,
) {
    val isLoggedIn get() = !baseUrl.isNullOrBlank() && !token.isNullOrBlank() && !nid.isNullOrBlank()
}
