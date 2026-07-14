package com.stealthcrypt.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KeyboardLayout(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onCommit: () -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbols by remember { mutableStateOf(false) }

    val rows = if (isSymbols) {
        listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "£", "_", "&", "-", "+", "(", ")", "/"),
            listOf("*", "\"", "'", ":", ";", "!", "?", "⌫"),
            listOf("ABC", ",", "SPACE", ".", "SEND")
        )
    } else {
        listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf("?123", ",", "SPACE", ".", "SEND")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .padding(bottom = 8.dp)
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    val displayKey = if (!isSymbols && isShifted && key.length == 1) key.uppercase() else key
                    KeyButton(
                        text = displayKey,
                        modifier = Modifier
                            .weight(if (key == "SPACE") 3f else if (key == "SEND" || key == "⇧" || key == "⌫" || key == "?123" || key == "ABC") 1.5f else 1f)
                            .padding(2.dp),
                        onClick = {
                            when (key) {
                                "⇧" -> isShifted = !isShifted
                                "?123" -> isSymbols = true
                                "ABC" -> isSymbols = false
                                "⌫" -> onBackspace()
                                "SPACE" -> onKeyPress(" ")
                                "SEND" -> onCommit()
                                else -> onKeyPress(displayKey)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(Color.White, shape = MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}
