package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.mobicad.geometry.AiCadGenerator
import com.example.mobicad.geometry.AiCadResult
import com.example.mobicad.geometry.CodeCadEngine
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.CadProject
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class CopilotMessage(
    val role: String,
    val text: String,
    val aiCadResult: AiCadResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CadCopilotDialog(
    project: CadProject,
    lang: CadLanguage,
    onApplyScriptToModel: (String) -> Unit,
    onOpenInCodeEditor: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    val isArabic = (lang == CadLanguage.AR)

    val messages = remember {
        mutableStateListOf(
            CopilotMessage(
                "model",
                if (isArabic)
                    "مرحبًا بك في مساعد Gemini الهندسي لـ MobiCAD Pro!\nيمكنك أن تطلب مني توليد تصاميم ثلاثية الأبعاد كاملة (Text-to-CAD)، أو تعديل أبعاد ومجسمات التصميم الحالي، أو إجراء الحسابات الميكانيكية وتفاوتات الطباعة (Tolerances)."
                else
                    "Welcome to MobiCAD Pro AI Generative CAD Copilot powered by Gemini!\nYou can ask me to generate full 3D CAD designs, modify features in the current model, write parametric scripts, or solve mechanical engineering equations."
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(4.dp)
                .testTag("copilot_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CadCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isArabic) "مساعد التصميم الهندسي الذكي" else "AI Generative CAD Copilot",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Gemini 3.5 Flash & Offline Parametric Engine",
                                fontSize = 10.sp,
                                color = CadCyan
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("copilot_close_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Divider(color = CadBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Quick AI Action Prompts
                val quickPrompts = if (isArabic) listOf(
                    "صمم علبة إلكترونيات ESP32 مع ثقوب M3",
                    "صمم حامل محرك NEMA 17",
                    "صمم صرة عمود مع رولمان بلي 608",
                    "أضف ثقب بقطر 4 مم في المنتصف",
                    "تفاوتات الطباعة ثلاثية الأبعاد"
                ) else listOf(
                    "Create ESP32 enclosure with M3 holes",
                    "Generate NEMA 17 motor bracket",
                    "Design shaft coupler with 608 bearing",
                    "Add central 4mm hole",
                    "3D Print Fit Tolerances"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (qp in quickPrompts) {
                        SuggestionChip(
                            onClick = { inputText = qp },
                            label = { Text(qp, fontSize = 10.sp, maxLines = 1) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = CadSurfaceVariant,
                                labelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, CadBorder)
                        )
                    }
                }

                // Chat Messages Thread
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = (msg.role == "user")
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 310.dp),
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) CadSurfaceVariant else Color(0xFF131924)
                                    ),
                                    border = if (isUser) BorderStroke(1.dp, CadCyan) else BorderStroke(1.dp, CadBorder)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        color = if (isUser) Color.White else CadTextPrimary,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                // If AI generated a 3D CAD design / script, display the Action Card
                                val cadRes = msg.aiCadResult
                                if (cadRes != null && cadRes.generatedScript.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1726)),
                                        border = BorderStroke(1.dp, CadCyan),
                                        modifier = Modifier.fillMaxWidth().testTag("ai_cad_action_card")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = CadCyan, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (cadRes.actionType == "MODIFY")
                                                            (if (isArabic) "تعديل النموذج 3D" else "3D Model Modification")
                                                        else
                                                            (if (isArabic) "تصميم ثلاثي الأبعاد جاهز" else "Generated 3D CAD Model"),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0x3300E5FF)
                                                ) {
                                                    Text(
                                                        text = "${cadRes.appliedFeaturesCount} Features",
                                                        fontSize = 9.sp,
                                                        color = CadCyan,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Code Preview Box
                                            Surface(
                                                color = Color(0xFF090D14),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFF222B3B)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = cadRes.generatedScript.lines().take(4).joinToString("\n") + if (cadRes.generatedScript.lines().size > 4) "\n..." else "",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFB0BEC5),
                                                    modifier = Modifier.padding(6.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action Buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Apply to 3D Canvas
                                                Button(
                                                    onClick = {
                                                        onApplyScriptToModel(cadRes.generatedScript)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CadGreen),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f).testTag("apply_ai_design_btn")
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isArabic) "تطبيق على 3D" else "Apply to 3D",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CadBackgroundDark
                                                    )
                                                }

                                                // Open in Code CAD Editor
                                                OutlinedButton(
                                                    onClick = {
                                                        onOpenInCodeEditor(cadRes.generatedScript)
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(1.dp, CadCyan),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    modifier = Modifier.weight(1f).testTag("open_in_code_cad_btn")
                                                ) {
                                                    Icon(Icons.Default.Code, contentDescription = null, tint = CadCyan, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isArabic) "تعديل بالكود" else "Edit Code",
                                                        fontSize = 11.sp,
                                                        color = CadCyan
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isThinking) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = CadCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "جارٍ المعالجة الهندسية وتوليد الأبعاد عبر الذكاء الاصطناعي..." else "AI is calculating mechanical geometry & parametric script...",
                                    fontSize = 11.sp,
                                    color = CadCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Field and Send Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "اطلب تصميم أو تعديل مجسم 3D..." else "Prompt 3D CAD design or edit...",
                                fontSize = 12.sp,
                                color = CadTextSecondary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("copilot_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CadCyan,
                            unfocusedBorderColor = CadBorder
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isThinking) {
                                val userQ = inputText.trim()
                                messages.add(CopilotMessage("user", userQ))
                                inputText = ""
                                isThinking = true

                                coroutineScope.launch {
                                    val aiResult = AiCadGenerator.generateOrModifyCad(userQ, project, isArabic)
                                    messages.add(
                                        CopilotMessage(
                                            role = "model",
                                            text = aiResult.explanation,
                                            aiCadResult = aiResult
                                        )
                                    )
                                    isThinking = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(CadCyan, RoundedCornerShape(8.dp))
                            .testTag("copilot_send_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = CadBackgroundDark)
                    }
                }
            }
        }
    }
}
