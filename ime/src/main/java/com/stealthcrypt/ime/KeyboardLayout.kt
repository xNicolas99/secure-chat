package com.stealthcrypt.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Farbwelt angelehnt an die Samsung-Tastatur im Dark Mode. */
object KeyboardColors {
    val Background = Color(0xFF1B1D21)
    val Key = Color(0xFF3B3E45)
    val FunctionKey = Color(0xFF2A2C31)
    val Accent = Color(0xFF3D7EFF)
    val Text = Color.White
    val HintText = Color(0xFF9AA0A6)
}

private val LETTER_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p", "ü"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "ö", "ä"),
    listOf("⇧", "y", "x", "c", "v", "b", "n", "m", "⌫"),
    listOf("!#1", ",", "SPACE", ".", "↵")
)

private val SYMBOL_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("@", "#", "€", "_", "&", "-", "+", "(", ")", "/"),
    listOf("*", "\"", "'", ":", ";", "!", "?", "%", "⌫"),
    listOf("ABC", ",", "SPACE", ".", "↵")
)

@Composable
fun KeyboardLayout(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onCommit: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbols by remember { mutableStateOf(false) }

    val rows = if (isSymbols) SYMBOL_ROWS else LETTER_ROWS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KeyboardColors.Background)
            .padding(horizontal = 3.dp)
            .padding(top = 4.dp, bottom = 8.dp)
    ) {
        for (row in rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (key in row) {
                    val isFunction = key in listOf("⇧", "⌫", "!#1", "ABC", "↵", "SPACE")
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
                                    "SPACE" -> 4f
                                    "⇧", "⌫", "!#1", "ABC", "↵" -> 1.5f
                                    else -> 1f
                                }
                            )
                            .padding(horizontal = 2.5.dp, vertical = 3.dp),
                        background = when {
                            key == "↵" -> KeyboardColors.Accent
                            key == "⇧" && isShifted -> KeyboardColors.HintText
                            isFunction && key != "SPACE" -> KeyboardColors.FunctionKey
                            else -> KeyboardColors.Key
                        },
                        onClick = {
                            when (key) {
                                "⇧" -> isShifted = !isShifted
                                "!#1" -> isSymbols = true
                                "ABC" -> isSymbols = false
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
    }
}

@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = KeyboardColors.Key,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(background, shape = RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = KeyboardColors.Text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
