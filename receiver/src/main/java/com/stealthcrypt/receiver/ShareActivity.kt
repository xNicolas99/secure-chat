package com.stealthcrypt.receiver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyManager = KeyManager(this)
        val password = keyManager.getPassword()

        var messageToDisplay = "No message provided."
        var isDecrypted = false

        if (password == null) {
            messageToDisplay = "No encryption key set. Please set it in the main app."
        } else if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                try {
                    messageToDisplay = StealthCrypto.decrypt(sharedText, password)
                    isDecrypted = true
                } catch (e: Exception) {
                    messageToDisplay = "Failed to decrypt. Not a valid StealthCrypt message or wrong key."
                }
            }
        }

        // Show as a dialog theme usually, but we'll use a Compose overlay
        setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isDecrypted) "🔓 Decrypted Message" else "❌ Error",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = messageToDisplay)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isDecrypted) {
                                Button(onClick = {
                                    copyToClipboard(messageToDisplay)
                                    finish()
                                }) {
                                    Text("Copy")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(onClick = { finish() }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Decrypted Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
