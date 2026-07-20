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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.stealthcrypt.crypto.StealthCrypto
import com.stealthcrypt.keystore.KeyManager

class StealthImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var composeView: ComposeView? = null
    private var isEncryptionEnabled by mutableStateOf(true)
    private var currentText by mutableStateOf("")
    private var keyManager: KeyManager? = null

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
        // Keystore access can fail on some devices; the keyboard must never crash because of it.
        keyManager = try {
            KeyManager(this)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreateInputView(): View {
        // Compose resolves the ViewTree owners from the *root* view of the IME window
        // (the DecorView), not from the ComposeView itself. Without this the keyboard
        // crashes with "ViewTreeLifecycleOwner not found" the moment it is shown.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

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
                        onKeyPress = { handleKeyPress(it) },
                        onCommit = { commitCurrentText() },
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

    private fun handleKeyPress(key: String) {
        if (isEncryptionEnabled) {
            currentText += key
        } else {
            // Without encryption the keyboard behaves like a normal one.
            currentInputConnection?.commitText(key, 1)
        }
    }

    private fun handleBackspace() {
        if (isEncryptionEnabled && currentText.isNotEmpty()) {
            currentText = currentText.dropLast(1)
        } else {
            val ic = currentInputConnection
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    private fun commitCurrentText() {
        val ic: InputConnection = currentInputConnection ?: return

        if (!isEncryptionEnabled || currentText.isEmpty()) {
            // Nothing buffered: behave like the enter/send key of a normal keyboard.
            if (!sendDefaultEditorAction(true)) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            return
        }

        val password = try {
            keyManager?.getPassword()
        } catch (e: Exception) {
            null
        }
        if (password != null) {
            val textToEncrypt = currentText
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val ciphertext = StealthCrypto.encrypt(textToEncrypt, password)
                    withContext(Dispatchers.Main) {
                        ic.commitText(ciphertext, 1)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        ic.commitText("[Encryption Failed]", 1)
                    }
                }
            }
        } else {
            ic.commitText("[No Key Set]", 1)
        }
        currentText = ""
    }
}

@Composable
fun ImeUi(
    text: String,
    isEncryptionEnabled: Boolean,
    onToggleEncryption: (Boolean) -> Unit,
    onKeyPress: (String) -> Unit,
    onCommit: () -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyboardColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEncryptionEnabled) "🔒" else "🔓",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isEncryptionEnabled,
                onCheckedChange = onToggleEncryption,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = KeyboardColors.Accent
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when {
                    !isEncryptionEnabled -> "Unverschlüsselt – tippt direkt ins Feld"
                    text.isEmpty() -> "Nachricht eingeben…"
                    else -> text
                },
                color = if (isEncryptionEnabled && text.isNotEmpty()) Color.White else KeyboardColors.HintText,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        KeyboardLayout(
            onKeyPress = onKeyPress,
            onBackspace = onBackspace,
            onCommit = onCommit
        )
    }
}
