package com.stealthcrypt.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.stealthcrypt.crypto.StealthCrypto
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StealthCryptoAndroidTest {

    @Before
    fun setup() {
        StealthCrypto.init(LazySodiumAndroid(SodiumAndroid()))
    }

    @Test
    fun roundTripWorksOnAndroid() {
        val plaintext = "Hello from Android Instrumentation Test!"
        val password = "verySecurePassword123"
        val ciphertext = StealthCrypto.encrypt(plaintext, password)
        val decrypted = StealthCrypto.decrypt(ciphertext, password)
        assertEquals(plaintext, decrypted)
    }
}
