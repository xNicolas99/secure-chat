package com.stealthcrypt.crypto

import com.goterl.lazysodium.LazySodium
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.interfaces.PwHash
import com.sun.jna.NativeLong
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

object StealthCrypto {
    private var _lazySodium: LazySodium? = null
    private val lazySodium: LazySodium
        get() = _lazySodium ?: LazySodiumJava(SodiumJava()).also { _lazySodium = it }

    fun init(sodium: LazySodium) {
        _lazySodium = sodium
    }

    const val MAGIC = "SC"
    const val VERSION: Byte = 1
    const val SALT_BYTES = PwHash.ARGON2ID_SALTBYTES // 16
    const val NONCE_BYTES = 24 // XChaCha20 nonce size
    const val MAC_BYTES = 16 // Poly1305 MAC size
    const val KEY_BYTES = 32

    // Argon2id parameters
    private val OPS_LIMIT = PwHash.ARGON2ID_OPSLIMIT_INTERACTIVE
    private val MEM_LIMIT = PwHash.MEMLIMIT_INTERACTIVE

    class DecryptionException(message: String) : Exception(message)

    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        require(salt.size == SALT_BYTES) { "Salt must be $SALT_BYTES bytes" }
        val key = ByteArray(KEY_BYTES)
        val pwdBytes = password.toByteArray(StandardCharsets.UTF_8)
        val success = lazySodium.cryptoPwHash(
            key, KEY_BYTES,
            pwdBytes, pwdBytes.size,
            salt,
            OPS_LIMIT, MEM_LIMIT,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )
        if (!success) {
            throw RuntimeException("Key derivation failed")
        }
        return key
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun generateNonce(): ByteArray {
        val nonce = ByteArray(NONCE_BYTES)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    // Envelope: [magic:2B "SC"] [version:1B] [kdf_salt:16B] [nonce:24B] [ciphertext+tag: n B]
    // Using Base64URL for transport to avoid chat app parsing issues better than Z85.

    fun encrypt(plaintext: String, password: String): String {
        val plaintextBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        val salt = generateSalt()
        val key = deriveKey(password, salt)
        val nonce = generateNonce()

        val ciphertext = ByteArray(plaintextBytes.size + MAC_BYTES)

        val success = lazySodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            ciphertext, null,
            plaintextBytes, plaintextBytes.size.toLong(),
            null, 0,
            null, nonce, key
        )

        if (!success) {
            throw RuntimeException("Encryption failed")
        }

        val magicBytes = MAGIC.toByteArray(StandardCharsets.UTF_8)
        val envelope = ByteArray(magicBytes.size + 1 + salt.size + nonce.size + ciphertext.size)

        var offset = 0
        System.arraycopy(magicBytes, 0, envelope, offset, magicBytes.size)
        offset += magicBytes.size

        envelope[offset] = VERSION
        offset += 1

        System.arraycopy(salt, 0, envelope, offset, salt.size)
        offset += salt.size

        System.arraycopy(nonce, 0, envelope, offset, nonce.size)
        offset += nonce.size

        System.arraycopy(ciphertext, 0, envelope, offset, ciphertext.size)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(envelope)
    }

    fun decrypt(envelopeBase64: String, password: String): String {
        val envelope: ByteArray
        try {
            envelope = Base64.getUrlDecoder().decode(envelopeBase64)
        } catch (e: IllegalArgumentException) {
            throw DecryptionException("Invalid Base64")
        }

        val magicBytes = MAGIC.toByteArray(StandardCharsets.UTF_8)
        val minSize = magicBytes.size + 1 + SALT_BYTES + NONCE_BYTES + MAC_BYTES
        if (envelope.size < minSize) {
            throw DecryptionException("Envelope too short")
        }

        var offset = 0
        for (i in magicBytes.indices) {
            if (envelope[offset + i] != magicBytes[i]) {
                throw DecryptionException("Invalid magic bytes")
            }
        }
        offset += magicBytes.size

        val version = envelope[offset]
        if (version != VERSION) {
            throw DecryptionException("Unsupported version")
        }
        offset += 1

        val salt = ByteArray(SALT_BYTES)
        System.arraycopy(envelope, offset, salt, 0, SALT_BYTES)
        offset += SALT_BYTES

        val nonce = ByteArray(NONCE_BYTES)
        System.arraycopy(envelope, offset, nonce, 0, NONCE_BYTES)
        offset += NONCE_BYTES

        val ciphertextSize = envelope.size - offset
        val ciphertext = ByteArray(ciphertextSize)
        System.arraycopy(envelope, offset, ciphertext, 0, ciphertextSize)

        val key = deriveKey(password, salt)
        val decrypted = ByteArray(ciphertextSize - MAC_BYTES)

        val success = lazySodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            decrypted, null,
            null,
            ciphertext, ciphertext.size.toLong(),
            null, 0,
            nonce, key
        )

        if (!success) {
            throw DecryptionException("Decryption failed (invalid password or corrupted data)")
        }

        return String(decrypted, StandardCharsets.UTF_8)
    }
}
