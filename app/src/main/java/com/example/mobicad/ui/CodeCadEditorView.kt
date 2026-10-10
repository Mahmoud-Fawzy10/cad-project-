package com.example.mobicad.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobicad.geometry.CodeCadEngine
import com.example.mobicad.geometry.CodeExecResult
import com.example.mobicad.geometry.ParametricKernel
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.CadProject
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeCadEditorView(
    project: CadProject,
    lang: CadLanguage,
    onBackTo3D: () -> Unit,
    onOpenAiPrompt: () -> Unit,
    onPushUndo: () -> Unit,
    initialScript: String? = null
) {
    BackHandler {
        onBackTo3D()
    }

    var scriptText by remember {
        mutableStateOf(initialScript ?: CodeCadEngine.projectToScript(project))
    }

    var executionResult by remember {
        mutableStateOf<CodeExecResult?>(
            CodeExecResult(
                isSuccess = true,
                message = "${project.features.size} features loaded from active CAD model.",
                featuresCount = project.features.size
            )
        )
    }

    var showTemplatesMenu by remember { mutableStateOf(false) }

    fun runCode() {
        onPushUndo()
        val res = CodeCadEngine.parseAndExecute(scriptText, project)
        executionResult = res
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == CadLanguage.AR) "محرر التصميم البرمجي (Code CAD)" else "Code CAD Studio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CadCyan
                        )
                        Text(
                            text = if (lang == CadLanguage.AR) "نمذجة بارامترية برمجية في الوقت الفعلي" else "Live Parametric Scripting Engine",
                            fontSize = 10.sp,
                            color = CadTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackTo3D, modifier = Modifier.testTag("code_cad_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to 3D", tint = Color.White)
                    }
                },
                actions = {
                    // Templates dropdown
                    Box {
                        IconButton(
                            onClick = { showTemplatesMenu = true },
                            modifier = Modifier.testTag("code_templates_btn")
                        ) {
                            Icon(Icons.Default.LibraryBooks, contentDescription = "Templates", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showTemplatesMenu,
                            onDismissRequest = { showTemplatesMenu = false },
                            modifier = Modifier.background(CadSurfaceDark)
                        ) {
                            CodeCadEngine.TEMPLATES.forEach { (tmplName, tmplCode) ->
                                DropdownMenuItem(
                                    text = { Text(tmplName, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        scriptText = tmplCode
                                        showTemplatesMenu = false
                                        runCode()
                                    }
                                )
                            }
                        }
                    }

                    // Ask Gemini / AI
                    IconButton(onClick = onOpenAiPrompt, modifier = Modifier.testTag("code_ai_btn")) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI", tint = CadCyan)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // RUN Button
                    Button(
                        onClick = { runCode() },
                        colors = ButtonDefaults.buttonColors(containerColor = CadGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("code_run_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == CadLanguage.AR) "تشغيل" else "RUN",
                            color = CadBackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CadSurfaceDark)
            )
        },
        bottomBar = {
            Surface(
                color = CadSurfaceDark,
                border = BorderStroke(1.dp, CadBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Quick Snippets Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val snippets = listOf(
                            Pair("+ box", "box(width=40.0, length=30.0, height=15.0)\n"),
                            Pair("+ cylinder", "cylinder(radius=15.0, height=30.0)\n"),
                            Pair("+ hole", "hole(diameter=3.4, depth=15.0, x=0.0, y=0.0)\n"),
                            Pair("+ fillet", "fillet(radius=2.0)\n"),
                            Pair("+ chamfer", "chamfer(distance=1.5)\n"),
                            Pair("+ shell", "shell(thickness=2.0)\n"),
                            Pair("+ NEMA17", "robotics(component=\"NEMA17\")\n"),
                            Pair("+ ESP32", "robotics(component=\"ESP32\")\n"),
                            Pair("+ Bearing", "robotics(component=\"BEARING_608\")\n"),
                            Pair("+ Material", "material(\"ALUMINUM_6061\")\n")
                        )

                        for ((label, code) in snippets) {
                            SuggestionChip(
                                onClick = {
                                    scriptText = if (scriptText.endsWith("\n") || scriptText.isEmpty()) scriptText + code else scriptText + "\n" + code
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = CadSurfaceVariant,
                                    labelColor = Color.White
                                ),
                                border = BorderStroke(1.dp, CadBorder)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Execution Status Console
                    val res = executionResult
                    if (res != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (res.isSuccess) Color(0xFF0F261B) else Color(0xFF2C1010)
                            ),
                            border = BorderStroke(1.dp, if (res.isSuccess) CadGreen else CadRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("code_status_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.isSuccess) CadGreen else CadRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = res.message,
                                        fontSize = 11.sp,
                                        color = if (res.isSuccess) Color(0xFF81C784) else Color(0xFFE57373),
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 2
                                    )
                                }

                                if (res.isSuccess) {
                                    Button(
                                        onClick = onBackTo3D,
                                        colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("code_view_3d_btn")
                                    ) {
                                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (lang == CadLanguage.AR) "معاينة 3D" else "View 3D",
                                            fontSize = 11.sp,
                                            color = CadBackgroundDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(CadBackgroundDark)
        ) {
            val lines = scriptText.lines()
            Row(modifier = Modifier.fillMaxSize()) {
                // Line Numbers Gutter
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F131C))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lines.size) {
                        Text(
                            text = "$i",
                            color = if (executionResult?.errorLine == i) CadRed else Color(0xFF556075),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Code Editor Input Area
                TextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E6ED),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_cad_editor_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CadBackgroundDark,
                        unfocusedContainerColor = CadBackgroundDark,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = CadCyan
                    )
                )
            }
        }
    }
}
