package com.stealthcrypt.keystore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "stealthcrypt_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("shared_password", null)
    }

    fun setPassword(password: String) {
        sharedPreferences.edit().putString("shared_password", password).apply()
    }

    fun hasPassword(): Boolean {
        return getPassword() != null
    }

    fun clearPassword() {
        sharedPreferences.edit().remove("shared_password").apply()
    }
}
