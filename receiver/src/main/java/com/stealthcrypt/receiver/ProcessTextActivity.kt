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
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager
import java.util.Base64

abstract class ProcessTextActivity : ComponentActivity() {

    protected abstract fun transform(input: String, password: String): String
    protected abstract val dialogTitle: String
    protected open val requiresEnvelopeCheck: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val input = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
        val readonly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if (input.isNullOrEmpty()) {
            finish()
            return
        }

        if (requiresEnvelopeCheck) {
            val isValidEnvelope = try {
                val decoded = Base64.getUrlDecoder().decode(input)
                decoded.size >= 2 && decoded[0] == 'S'.code.toByte() && decoded[1] == 'C'.code.toByte()
            } catch (e: Exception) {
                false
            }
            if (!isValidEnvelope) {
                Toast.makeText(this, "Not a valid StealthCrypt envelope.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }

        val password = try {
            KeyManager(this).getPassword()
        } catch (e: Exception) {
            null
        }
        if (password == null) {
            Toast.makeText(this, "No encryption key set. Open StealthCrypt first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val output = try {
            transform(input, password)
        } catch (e: Exception) {
            Toast.makeText(this, "Operation failed: wrong key or invalid message.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!readonly) {
            val result = Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, output)
            setResult(RESULT_OK, result)
            finish()
        } else {
            showResultDialog(output)
        }
    }

    private fun showResultDialog(output: String) {
        setContent {
            var displayedText by remember { mutableStateOf(output) }

            LaunchedEffect(Unit) {
                delay(5000)
                displayedText = ""
                finish()
            }

            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Transparent), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Surface(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = dialogTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = displayedText)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                copyToClipboard(displayedText)
                                finish()
                            }) {
                                Text("Copy")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { finish() }) {
                                Text("Close")
                            }
                        }
                    }
                }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("StealthCrypt", text))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

class EncryptTextActivity : ProcessTextActivity() {
    override val dialogTitle = "🔒 Encrypted Message"
    override fun transform(input: String, password: String): String =
        StealthCrypto.encrypt(input, password)
}

class DecryptTextActivity : ProcessTextActivity() {
    override val dialogTitle = "🔓 Decrypted Message"
    override val requiresEnvelopeCheck = true
    override fun transform(input: String, password: String): String =
        StealthCrypto.decrypt(input.trim(), password)
}
