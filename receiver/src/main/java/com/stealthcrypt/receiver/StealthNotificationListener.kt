package com.stealthcrypt.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager

class StealthNotificationListener : NotificationListenerService() {

    private lateinit var keyManager: KeyManager

    // Limits text to decrypt to prevent memory/performance issues on very large messages
    private val TRUNCATION_LIMIT = 5000

    override fun onCreate() {
        super.onCreate()
        keyManager = KeyManager(this)
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        // Filter primarily for WhatsApp, but could expand.
        if (sbn.packageName != "com.whatsapp") {
            return
        }

        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        val password = keyManager.getPassword() ?: return

        try {
            // Check if looks like a message (starting with the magic bytes might be masked by base64,
            // but we rely on decrypt throwing on fail)
            val decryptedText = StealthCrypto.decrypt(text.take(TRUNCATION_LIMIT), password)
            showDecryptedNotification(decryptedText, sbn.id)
        } catch (e: Exception) {
            // Not a StealthCrypt message or wrong key. Ignore silently.
        }
    }

    private fun showDecryptedNotification(decryptedText: String, notificationId: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = Notification.Builder(this, "stealthcrypt_channel")
            .setContentTitle("🔓 Decrypted Message")
            .setContentText(decryptedText)
            .setSmallIcon(android.R.drawable.ic_secure) // Generic icon
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
