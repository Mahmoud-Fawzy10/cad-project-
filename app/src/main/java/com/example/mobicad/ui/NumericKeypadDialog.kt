package com.example.mobicad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.ui.theme.CadBackgroundDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariant

@Composable
fun NumericKeypadDialog(
    title: String,
    initialValue: Double,
    unit: String = "mm",
    lang: CadLanguage,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(if (initialValue == initialValue.toLong().toDouble()) initialValue.toLong().toString() else initialValue.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("numeric_keypad_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Value Display Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(CadBackgroundDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (textValue.isEmpty()) "0" else textValue,
                            color = CadCyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unit,
                            color = Color(0xFF94A3B8),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad Grid
                val keys = listOf(
                    listOf("7", "8", "9", "⌫"),
                    listOf("4", "5", "6", "C"),
                    listOf("1", "2", "3", "±"),
                    listOf("0", ".", "+5", "-5")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (key in row) {
                            Button(
                                onClick = {
                                    when (key) {
                                        "⌫" -> {
                                            if (textValue.isNotEmpty()) textValue = textValue.dropLast(1)
                                        }
                                        "C" -> textValue = ""
                                        "±" -> {
                                            textValue = if (textValue.startsWith("-")) textValue.drop(1) else "-$textValue"
                                        }
                                        "+5" -> {
                                            val cur = textValue.toDoubleOrNull() ?: 0.0
                                            textValue = (cur + 5.0).toString()
                                        }
                                        "-5" -> {
                                            val cur = textValue.toDoubleOrNull() ?: 0.0
                                            textValue = (cur - 5.0).coerceAtLeast(0.0).toString()
                                        }
                                        "." -> {
                                            if (!textValue.contains(".")) textValue += if (textValue.isEmpty()) "0." else "."
                                        }
                                        else -> {
                                            textValue += key
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (key) {
                                        "⌫", "C" -> Color(0xFF334155)
                                        "+5", "-5" -> Color(0xFF1E293B)
                                        else -> CadSurfaceVariant
                                    }
                                ),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("keypad_btn_$key")
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("keypad_cancel_btn")
                    ) {
                        Text(CadStrings.get("cancel_sketch", lang), color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = {
                            val v = textValue.toDoubleOrNull() ?: 0.0
                            onConfirm(v)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("keypad_apply_btn")
                    ) {
                        Text(CadStrings.get("apply", lang), color = CadBackgroundDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
