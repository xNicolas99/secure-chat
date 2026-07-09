package com.stealthcrypt.ime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StealthImeServiceTest {

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val keyManager = KeyManager(appContext)
        keyManager.setPassword("test_password_for_ime")
    }

    @Test
    fun testKeyManagerIntegration() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val keyManager = KeyManager(appContext)
        assertEquals("test_password_for_ime", keyManager.getPassword())
    }

    // Note: Testing an actual InputMethodService flow requires UIAutomator to
    // switch keyboards which is difficult in basic instrumented tests without
    // system permissions. We'll verify the integration components instead.

    @Test
    fun testEncryptionCommitsValidEnvelope() {
        val plaintext = "Hello World"
        val password = "test_password_for_ime"
        val ciphertext = StealthCrypto.encrypt(plaintext, password)
        assertTrue(ciphertext.isNotEmpty())
        val decrypted = StealthCrypto.decrypt(ciphertext, password)
        assertEquals(plaintext, decrypted)
    }
}
