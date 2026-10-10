package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.CadProject
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val text: String, val timestamp: Long = System.currentTimeMillis())

@Composable
fun CadCopilotDialog(
    project: CadProject,
    lang: CadLanguage,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "model",
                if (lang == CadLanguage.AR)
                    "مرحبًا بك في المساعد الهندسي لـ MobiCAD Pro! يمكنني مساعدتك في حسابات عزوم المحركات، واختيار تفاوتات الطباعة ثلاثية الأبعاد (Tolerances)، وحساب أحجام مسامير التثبيت وقوى التحمل."
                else
                    "Welcome to MobiCAD Pro CAD Copilot! I can assist you with motor torque sizing, 3D printing tolerances, screw clearance drill charts, and robotics bracket design."
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
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
                        Text(
                            text = CadStrings.get("copilot", lang),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Mechanical Engineering & Tolerance Advisor",
                    fontSize = 11.sp,
                    color = CadTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider(color = CadBorder)

                // Quick Engineering Action Chips
                val quickActions = listOf(
                    "3D Print Fit Tolerances",
                    "NEMA 17 Motor Sizing",
                    "Metric Screw Drill Chart",
                    "Cantilever Beam Stress"
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (qa in quickActions) {
                        SuggestionChip(
                            onClick = { inputText = qa },
                            label = { Text(qa, fontSize = 10.sp, maxLines = 1) },
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = (msg.role == "user")
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) CadSurfaceVariant else Color(0xFF131924)
                                ),
                                border = if (isUser) BorderStroke(1.dp, CadCyan) else BorderStroke(1.dp, CadBorder),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    color = if (isUser) Color.White else CadTextPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    if (isThinking) {
                        item {
                            Text(
                                text = "AI Copilot is evaluating engineering equations...",
                                fontSize = 11.sp,
                                color = CadCyan,
                                modifier = Modifier.padding(4.dp)
                            )
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
                        placeholder = { Text("Ask engineering question...", fontSize = 12.sp, color = CadTextSecondary) },
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
                                messages.add(ChatMessage("user", userQ))
                                inputText = ""
                                isThinking = true

                                coroutineScope.launch {
                                    val answer = queryGeminiOrOffline(userQ, project)
                                    messages.add(ChatMessage("model", answer))
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

private suspend fun queryGeminiOrOffline(question: String, project: CadProject): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isNullOrEmpty() || apiKey.startsWith("MY_GEMINI")) {
        // High-precision Offline Mechanical Engineering Rules Engine
        return@withContext offlineMechanicalAdvisor(question, project)
    }

    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val prompt = "You are an expert mechanical CAD software architect and robotics engineer assisting a user in MobiCAD Pro. Current CAD model has ${project.features.size} features, material: ${project.material.name}. User query: $question. Provide concise, numerically accurate mechanical engineering advice, tolerances, or formulas in mm and metric units."

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val respBody = response.body?.string() ?: ""
            val respJson = JSONObject(respBody)
            val candidates = respJson.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
            text ?: offlineMechanicalAdvisor(question, project)
        } else {
            offlineMechanicalAdvisor(question, project)
        }
    } catch (e: Exception) {
        offlineMechanicalAdvisor(question, project)
    }
}

private fun offlineMechanicalAdvisor(q: String, project: CadProject): String {
    val lower = q.lowercase()
    return when {
        lower.contains("tolerance") || lower.contains("print") -> {
            "Recommended FDM 3D Printing Fit Tolerances:\n• Press/Tight fit (Bearings, Pins): +0.10 to +0.15 mm diametral clearance.\n• Snug Sliding fit (Dovetails, Brackets): +0.20 to +0.25 mm clearance.\n• Free Rotation/Moving Joint: +0.40 to +0.50 mm clearance.\nNote: For PLA, material shrinkage is ~0.3-0.5%."
        }
        lower.contains("screw") || lower.contains("drill") || lower.contains("m3") || lower.contains("m4") -> {
            "Standard ISO Metric Screw Drilling Guide:\n• M2: Tap drill 1.6 mm | Close clearance 2.2 mm | Free clearance 2.4 mm\n• M2.5: Tap drill 2.05 mm | Close clearance 2.7 mm | Free clearance 2.9 mm\n• M3: Tap drill 2.5 mm | Close clearance 3.2 mm | Free clearance 3.4 mm\n• M4: Tap drill 3.3 mm | Close clearance 4.3 mm | Free clearance 4.5 mm\n• M5: Tap drill 4.2 mm | Close clearance 5.3 mm | Free clearance 5.5 mm"
        }
        lower.contains("motor") || lower.contains("torque") || lower.contains("nema") -> {
            "Motor Torque Calculation Formula:\nτ = (m × a + F_friction) × r × SafetyFactor (1.5 - 2.0)\nFor example: A 2 kg mobile robot with 65 mm wheels (r = 32.5 mm) accelerating at 1.0 m/s²:\nτ_min = 2.0 kg × 1.0 m/s² × 0.0325 m = 0.065 N·m (6.5 N·cm per wheel).\nA standard NEMA 17 provides 30 - 45 N·cm, offering an ample safety factor of ~5x."
        }
        lower.contains("stress") || lower.contains("beam") || lower.contains("cantilever") -> {
            "Cantilever Beam Stress & Deflection:\n• Max Bending Stress: σ_max = (M × c) / I = (6 × F × L) / (b × h²)\n• Max Deflection: δ_max = (F × L³) / (3 × E × I)\nFor ${project.material.name} (E = ${project.material.youngsModulusGpa} GPa, Yield = ${project.material.yieldStrengthMpa} MPa), ensure σ_max < Yield / 2.0 for a safety factor of 2."
        }
        else -> {
            "Engineering Analysis (Offline Kernel):\nModel has ${project.features.size} active operations using ${project.material.name} (density ${project.material.densityGramsPerCm3} g/cm³).\nAll dimensions are canonical millimeters. Recommended minimum wall thickness for rigid robotics shells: 2.4 mm to 3.0 mm."
        }
    }
}
