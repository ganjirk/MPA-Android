package com.grk.mpa.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Keeps the tokens in a DataStore file, encrypted with [cipher]. The file is excluded from
 * backups (see the backup rules), because the key cannot be restored on another device.
 * Tokens that cannot be decrypted are discarded, which simply signs the user out.
 */
class EncryptedAuthStorage(
    context: Context,
    private val cipher: TokenCipher = KeystoreTokenCipher(),
    private val json: Json = Json
) : AuthStorage {
    private val store = context.applicationContext.authDataStore
    private val key = stringPreferencesKey("tokens_v1")

    override suspend fun load(): StoredTokens? {
        val blob = store.data.first()[key] ?: return null
        return try {
            json.decodeFromString<StoredTokens>(cipher.decrypt(blob))
        } catch (e: Exception) {
            clear()
            null
        }
    }

    override suspend fun save(tokens: StoredTokens) {
        val blob = cipher.encrypt(json.encodeToString(tokens))
        store.edit { it[key] = blob }
    }

    override suspend fun clear() {
        store.edit { it.remove(key) }
    }
}
