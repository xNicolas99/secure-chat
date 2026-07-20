package com.stealthcrypt.ime

import android.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.compose.foundation.isSystemInDarkTheme

object KeyboardColors {
    val BackgroundDark = Color(0xFF000000)
    val KeyDark = Color(0xFF2C2C2C)
    val FunctionKeyDark = Color(0xFF2A2C31)
    val TextDark = Color.White
    val HintTextDark = Color(0xFF9AA0A6)

    val BackgroundLight = Color(0xFFFFFFFF)
    val KeyLight = Color(0xFFEBEBEB)
    val FunctionKeyLight = Color(0xFFD6D6D6)
    val TextLight = Color.Black
    val HintTextLight = Color(0xFF6B6B6B)

    val Accent = Color(0xFF3D7EFF)
}

@Composable
fun getKeyboardColors(): Map<String, Color> {
    val isDark = isSystemInDarkTheme()
    return mapOf(
        "Background" to if (isDark) KeyboardColors.BackgroundDark else KeyboardColors.BackgroundLight,
        "Key" to if (isDark) KeyboardColors.KeyDark else KeyboardColors.KeyLight,
        "FunctionKey" to if (isDark) KeyboardColors.FunctionKeyDark else KeyboardColors.FunctionKeyLight,
        "Text" to if (isDark) KeyboardColors.TextDark else KeyboardColors.TextLight,
        "HintText" to if (isDark) KeyboardColors.HintTextDark else KeyboardColors.HintTextLight,
        "Accent" to KeyboardColors.Accent
    )
}

private val LETTER_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ö", "ä"),
    listOf("⇧", "y", "x", "c", "v", "b", "n", "m", "⌫"),
    listOf("!#1", "😊", ",", "SPACE", ".", "↵")
)

private val SYMBOL_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("@", "#", "€", "_", "&", "-", "+", "(", ")", "/"),
    listOf("*", "\"", "'", ":", ";", "!", "?", "%", "⌫"),
    listOf("ABC", "😊", ",", "SPACE", ".", "↵")
)

@Composable
fun KeyboardLayout(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onCommit: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbols by remember { mutableStateOf(false) }
    var isEmoji by remember { mutableStateOf(false) }
    val colors = getKeyboardColors()

    if (isEmoji) {
        EmojiPanel(
            onEmojiPicked = onKeyPress,
            onBackspace = onBackspace,
            onClose = { isEmoji = false }
        )
        return
    }

    val rows = if (isSymbols) SYMBOL_ROWS else LETTER_ROWS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors["Background"]!!)
            .padding(horizontal = 3.dp)
            .padding(top = 4.dp)
    ) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (key in row) {
                    val isFunction = key in listOf("⇧", "⌫", "!#1", "ABC", "↵", "SPACE", "😊")
                    val displayKey = when {
                        key == "SPACE" -> " "
                        !isSymbols && isShifted && key.length == 1 && key[0].isLetter() -> key.uppercase()
                        else -> key
                    }
                    KeyButton(
                        text = when (key) {
                            "SPACE" -> "Deutsch"
                            else -> displayKey
                        },
                        modifier = Modifier
                            .weight(
                                when (key) {
                                    "SPACE" -> 3f
                                    "⇧", "⌫", "!#1", "ABC", "↵" -> 1.5f
                                    else -> 1f
                                }
                            )
                            .padding(6.dp), // 6dp total horizontal between keys
                        background = when {
                            key == "↵" -> colors["Accent"]!!
                            key == "⇧" && isShifted -> colors["HintText"]!!
                            isFunction && key != "SPACE" -> colors["FunctionKey"]!!
                            else -> colors["Key"]!!
                        },
                        textColor = colors["Text"]!!,
                        onClick = {
                            when (key) {
                                "⇧" -> isShifted = !isShifted
                                "!#1" -> isSymbols = true
                                "ABC" -> isSymbols = false
                                "😊" -> isEmoji = true
                                "⌫" -> onBackspace()
                                "SPACE" -> onKeyPress(" ")
                                "↵" -> onCommit()
                                else -> {
                                    onKeyPress(displayKey)
                                    // Wie bei Samsung: Shift gilt nur für einen Buchstaben.
                                    if (isShifted && !isSymbols) isShifted = false
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/**
 * Vollwertiger Emoji-Picker (Kategorien, zuletzt verwendet, Hauttöne per
 * Langdruck). Die Emoji-Glyphen kommen vom System-Font — auf Samsung-Geräten
 * sehen sie damit exakt wie in der Samsung-Tastatur aus.
 */
@Composable
fun EmojiPanel(
    onEmojiPicked: (String) -> Unit,
    onBackspace: () -> Unit,
    onClose: () -> Unit
) {
    val currentOnEmojiPicked by rememberUpdatedState(onEmojiPicked)
    val colors = getKeyboardColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors["Background"]!!)

    ) {
        AndroidView(
            factory = { ctx ->
                EmojiPickerView(ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault)).apply {
                    emojiGridColumns = 9
                }
            },
            update = { view ->
                view.setOnEmojiPickedListener { item -> currentOnEmojiPicked(item.emoji) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(258.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
        ) {
            KeyButton(
                text = "ABC",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(6.dp),
                background = colors["FunctionKey"]!!,
                textColor = colors["Text"]!!,
                onClick = onClose
            )
            KeyButton(
                text = " ",
                modifier = Modifier
                    .weight(5f)
                    .padding(6.dp),
                background = colors["Key"]!!,
                textColor = colors["Text"]!!,
                onClick = { onEmojiPicked(" ") }
            )
            KeyButton(
                text = "⌫",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(6.dp),
                background = colors["FunctionKey"]!!,
                textColor = colors["Text"]!!,
                onClick = onBackspace
            )
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(background, shape = RoundedCornerShape(size = 4.dp)) // 4dp corner radius
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
