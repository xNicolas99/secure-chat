package com.stealthcrypt.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
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

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onPasswordSet(result.contents)
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Scan StealthCrypt QR Code")
                setBeepEnabled(false)
            }
            scanLauncher.launch(options)
        }
    }

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
                val randomBytes = ByteArray(32)
                SecureRandom().nextBytes(randomBytes)
                val randomHex = randomBytes.joinToString("") { "%02x".format(it) }
                onPasswordSet(randomHex)
            }
        ) {
            Text("Generate Random Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        ) {
            Text("Scan QR Code")
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

        Text("Enable Notification Listener:")
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
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    } catch (e: Exception) {
        return null
    }
}
