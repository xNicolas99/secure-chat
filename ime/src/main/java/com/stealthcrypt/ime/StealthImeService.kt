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
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager

class StealthImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var composeView: ComposeView? = null
    private var isEncryptionEnabled by mutableStateOf(true)
    private var currentText by mutableStateOf("")
    private lateinit var keyManager: KeyManager

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
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
                        isEncryptionEnabled = isEncryptionEnabled,
                        onToggleEncryption = { isEncryptionEnabled = it },
                        onCommit = { commitCurrentText() },
                        onTextChange = { currentText = it },
                        onBackspace = { handleBackspace() }
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
                } catch (e: Exception) {
                    ic.commitText("[Encryption Failed]", 1)
                }
            } else {
                ic.commitText("[No Key Set]", 1)
            }
        } else {
            ic.commitText(currentText, 1)
        }
        currentText = ""
    }
}

@Composable
fun ImeUi(
    text: String,
    isEncryptionEnabled: Boolean,
    onToggleEncryption: (Boolean) -> Unit,
    onCommit: () -> Unit,
    onTextChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0E0E0))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEncryptionEnabled) "🔒" else "🔓",
                modifier = Modifier.padding(8.dp)
            )
            Switch(
                checked = isEncryptionEnabled,
                onCheckedChange = onToggleEncryption
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type message here...") },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onBackspace) {
                Text("⌫")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCommit) {
                Text("Send")
            }
        }
    }
}
