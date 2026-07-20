package com.stealthcrypt.keystore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyManagerTest {

    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        keyManager = KeyManager(appContext)
        keyManager.clearPassword()
    }

    @Test
    fun testSetAndGetPassword() {
        val password = "my_secure_password"
        keyManager.setPassword(password)
        assertEquals(password, keyManager.getPassword())
    }

    @Test
    fun testHasPassword() {
        assertFalse(keyManager.hasPassword())
        keyManager.setPassword("temp_pass")
        assertTrue(keyManager.hasPassword())
    }

    @Test
    fun testClearPassword() {
        keyManager.setPassword("to_be_cleared")
        assertTrue(keyManager.hasPassword())
        keyManager.clearPassword()
        assertFalse(keyManager.hasPassword())
        assertNull(keyManager.getPassword())
    }
}
