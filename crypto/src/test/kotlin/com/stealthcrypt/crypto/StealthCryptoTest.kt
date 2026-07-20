package com.stealthcrypt.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Base64
import kotlin.test.assertFailsWith

class StealthCryptoTest {

    @Test
    fun `round trip encryption and decryption works`() {
        val password = "mySecretPassword123"
        val plaintext = "Hello, this is a secret message! 🔒👍"

        val encrypted = StealthCrypto.encrypt(plaintext, password)
        val decrypted = StealthCrypto.decrypt(encrypted, password)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encryption produces different ciphertexts for same plaintext`() {
        val password = "mySecretPassword123"
        val plaintext = "Hello"

        val encrypted1 = StealthCrypto.encrypt(plaintext, password)
        val encrypted2 = StealthCrypto.encrypt(plaintext, password)

        assertNotEquals(encrypted1, encrypted2)
    }

    @Test
    fun `wrong password fails to decrypt`() {
        val password = "correctPassword"
        val wrongPassword = "wrongPassword"
        val plaintext = "Secret data"

        val encrypted = StealthCrypto.encrypt(plaintext, password)

        assertFailsWith<StealthCrypto.DecryptionException> {
            StealthCrypto.decrypt(encrypted, wrongPassword)
        }
    }

    @Test
    fun `tampering with ciphertext fails decryption`() {
        val password = "password"
        val plaintext = "Don't change me"

        val encrypted = StealthCrypto.encrypt(plaintext, password)
        val bytes = Base64.getUrlDecoder().decode(encrypted)

        // Tamper with the last byte (part of MAC or ciphertext)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()

        val tamperedEncrypted = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        assertFailsWith<StealthCrypto.DecryptionException> {
            StealthCrypto.decrypt(tamperedEncrypted, password)
        }
    }

    @Test
    fun `tampering with magic bytes fails decryption`() {
        val password = "password"
        val plaintext = "Don't change me"

        val encrypted = StealthCrypto.encrypt(plaintext, password)
        val bytes = Base64.getUrlDecoder().decode(encrypted)

        // Tamper with first byte (magic 'S')
        bytes[0] = 'X'.code.toByte()

        val tamperedEncrypted = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        val exception = assertFailsWith<StealthCrypto.DecryptionException> {
            StealthCrypto.decrypt(tamperedEncrypted, password)
        }
        assertEquals("Invalid magic bytes", exception.message)
    }

    @Test
    fun `empty string works`() {
        val password = "password"
        val plaintext = ""

        val encrypted = StealthCrypto.encrypt(plaintext, password)
        val decrypted = StealthCrypto.decrypt(encrypted, password)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `random text is rejected`() {
        val randomText = "This is just some random chat text that is not base64 encoded properly or at least not an envelope"
        assertFailsWith<StealthCrypto.DecryptionException> {
            StealthCrypto.decrypt(randomText, "password")
        }
    }

    @Test
    fun `too short envelope is rejected`() {
        val shortBytes = byteArrayOf('S'.code.toByte(), 'C'.code.toByte(), 1)
        val encrypted = Base64.getUrlEncoder().withoutPadding().encodeToString(shortBytes)

        assertFailsWith<StealthCrypto.DecryptionException> {
            StealthCrypto.decrypt(encrypted, "password")
        }
    }
}
