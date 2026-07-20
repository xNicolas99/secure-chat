package com.stealthcrypt.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.stealthcrypt.keystore.KeyManager
import java.security.SecureRandom

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keyManager = KeyManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(keyManager, this)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(keyManager: KeyManager, context: Context) {
    var hasPassword by remember { mutableStateOf(keyManager.hasPassword()) }
    var currentPassword by remember { mutableStateOf(keyManager.getPassword() ?: "") }

    if (hasPassword) {
        MainScreen(
            currentPassword = currentPassword,
            onClearKey = {
                keyManager.clearPassword()
                hasPassword = false
                currentPassword = ""
            },
            context = context
        )
    } else {
        SetupScreen(
            onPasswordSet = {
                keyManager.setPassword(it)
                hasPassword = true
                currentPassword = it
            }
        )
    }
}

@Composable
fun SetupScreen(onPasswordSet: (String) -> Unit) {
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Setup StealthCrypt", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Enter a strong shared password") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (passwordInput.isNotBlank()) {
                    onPasswordSet(passwordInput)
                }
            },
            enabled = passwordInput.isNotBlank()
        ) {
            Text("Set Password")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OR", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Generate a strong 256-bit random password represented as Hex
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                val randomHex = randomBytes.joinToString("") { "%02x".format(it) }
                onPasswordSet(randomHex)
            }
        ) {
            Text("Generate Random Key")
        }
    }
}

@Composable
fun MainScreen(currentPassword: String, onClearKey: () -> Unit, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("StealthCrypt is Ready", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Enable the Keyboard:")
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }) {
            Text("Open Settings")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Enable Notification Listener (Optional):")
        Button(onClick = {
            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }) {
            Text("Open Settings")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Share this Key via QR Code:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        val qrBitmap = remember(currentPassword) { generateQrCode(currentPassword) }
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR Code of the Key",
                modifier = Modifier.size(250.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClearKey,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear Key (Reset)")
        }
    }
}

fun generateQrCode(text: String): Bitmap? {
    if (text.isEmpty()) return null
    try {
        val size = 512
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    } catch (e: com.google.zxing.WriterException) {
        return null
    }
}
