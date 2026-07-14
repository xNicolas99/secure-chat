package com.stealthcrypt.ime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager

class StealthImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var composeView: ComposeView? = null
    private var isEncryptionEnabled by mutableStateOf(true)
    private var currentText by mutableStateOf("")
    private var errorMessage by mutableStateOf<String?>(null)
    private lateinit var keyManager: KeyManager

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        try { StealthCrypto.init(LazySodiumAndroid(SodiumAndroid())) } catch(e: Exception) {}
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        keyManager = KeyManager(this)
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@StealthImeService)
            setViewTreeViewModelStoreOwner(this@StealthImeService)
            setViewTreeSavedStateRegistryOwner(this@StealthImeService)
            setContent {
                MaterialTheme {
                    ImeUi(
                        text = currentText,
                        errorMessage = errorMessage,
                        isEncryptionEnabled = isEncryptionEnabled,
                        onToggleEncryption = { isEncryptionEnabled = it; errorMessage = null },
                        onCommit = { commitCurrentText() },
                        onKeyPress = { currentText += it; errorMessage = null },
                        onBackspace = { handleBackspace(); errorMessage = null }
                    )
                }
            }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        composeView = view
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentText = ""
        errorMessage = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    private fun handleBackspace() {
        if (currentText.isNotEmpty()) {
            currentText = currentText.dropLast(1)
        } else {
            val ic = currentInputConnection
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun commitCurrentText() {
        val ic: InputConnection = currentInputConnection ?: return
        if (currentText.isEmpty()) return

        if (isEncryptionEnabled) {
            val password = keyManager.getPassword()
            if (password != null) {
                try {
                    val ciphertext = StealthCrypto.encrypt(currentText, password)
                    ic.commitText(ciphertext, 1)
                    currentText = ""
                } catch (e: Exception) {
                    errorMessage = "Encryption failed."
                }
            } else {
                errorMessage = "No key set."
            }
        } else {
            ic.commitText(currentText, 1)
            currentText = ""
        }
    }
}

@Composable
fun ImeUi(
    text: String,
    errorMessage: String?,
    isEncryptionEnabled: Boolean,
    onToggleEncryption: (Boolean) -> Unit,
    onCommit: () -> Unit,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEncryptionEnabled) "🔒" else "🔓",
                modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
                checked = isEncryptionEnabled,
                onCheckedChange = onToggleEncryption
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, shape = MaterialTheme.shapes.small)
                    .padding(8.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage, color = Color.Red)
                } else if (text.isEmpty()) {
                    Text("Type message here...", color = Color.Gray)
                } else {
                    Text(text)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        KeyboardLayout(
            onKeyPress = onKeyPress,
            onBackspace = onBackspace,
            onCommit = onCommit
        )
    }
}
