package com.stealthcrypt.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager
import java.util.Base64

class StealthNotificationListener : NotificationListenerService() {

    private var keyManager: KeyManager? = null

    // Limits text to decrypt to prevent memory/performance issues on very large messages
    private val TRUNCATION_LIMIT = 5000

    override fun onCreate() {
        super.onCreate()
        keyManager = try {
            KeyManager(this)
        } catch (e: Exception) {
            null
        }
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        // Never process our own notifications (would loop on the decrypted output).
        if (sbn.packageName == packageName) return

        val password = try {
            keyManager?.getPassword()
        } catch (e: Exception) {
            null
        } ?: return

        val extras = sbn.notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        // Collect every text this notification carries: plain text, expanded text
        // and all MessagingStyle messages (WhatsApp, Signal, Telegram, SMS, ...).
        val candidates = collectTexts(extras)

        var counter = 0
        for (text in candidates) {
            val trimmed = text.trim().take(TRUNCATION_LIMIT)
            if (!looksLikeEnvelope(trimmed)) continue
            try {
                val decryptedText = StealthCrypto.decrypt(trimmed, password)
                showDecryptedNotification(decryptedText, sender, sbn.id + counter)
                counter++
            } catch (e: Exception) {
                // Not a StealthCrypt message or wrong key. Ignore silently.
            }
        }
    }

    private fun collectTexts(extras: Bundle): List<String> {
        val texts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let { texts.add(it) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let { texts.add(it) }

        @Suppress("DEPRECATION")
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null) {
            for (message in messages) {
                val bundle = message as? Bundle ?: continue
                bundle.getCharSequence("text")?.toString()?.let { texts.add(it) }
            }
        }
        return texts.distinct()
    }

    /**
     * Cheap pre-check so we never run the expensive Argon2 key derivation on
     * ordinary chat messages: the envelope is Base64URL and starts with the
     * magic bytes "SC" followed by the version byte.
     */
    private fun looksLikeEnvelope(text: String): Boolean {
        if (text.length < 80 || text.contains(' ') || text.contains('\n')) return false
        return try {
            val decoded = Base64.getUrlDecoder().decode(text)
            decoded.size > 3 &&
                decoded[0] == 'S'.code.toByte() &&
                decoded[1] == 'C'.code.toByte() &&
                decoded[2] == StealthCrypto.VERSION
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun showDecryptedNotification(decryptedText: String, sender: String?, notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = Notification.Builder(this, "stealthcrypt_channel")
            .setContentTitle(if (sender != null) "🔓 $sender" else "🔓 Decrypted Message")
            .setContentText(decryptedText)
            .setStyle(Notification.BigTextStyle().bigText(decryptedText))
            .setSmallIcon(android.R.drawable.ic_secure)
            .setAutoCancel(true)
            .build()

        // Use a different ID to avoid replacing the original chat notification
        notificationManager.notify(notificationId + 1000, notification)
    }

    private fun createNotificationChannel() {
        val name = "Decrypted Messages"
        val descriptionText = "Shows messages decrypted by StealthCrypt"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("stealthcrypt_channel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
