package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobicad.geometry.ParametricKernel
import com.example.mobicad.geometry.ProjectSerializer
import com.example.mobicad.geometry.RoboticsLibrary
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.mobicad.viewport.Camera3D
import com.example.ui.theme.*

enum class ActiveScreenMode {
    VIEWPORT_3D,
    SKETCH_2D
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobiCadApp() {
    val context = LocalContext.current

    // App Language State (English / Arabic RTL)
    var currentLang by remember { mutableStateOf(CadLanguage.EN) }

    // Project State
    var currentProject by remember {
        mutableStateOf(ProjectSerializer.createSampleProject("ESP32 Enclosure"))
    }

    // Camera State
    val camera = remember {
        Camera3D().apply {
            if (currentProject.generatedMesh != null) {
                fitToBoundingBox(currentProject.generatedMesh!!.boundingBox)
            }
        }
    }

    // Screen Mode (3D Viewport vs 2D Sketch Editor)
    var activeMode by remember { mutableStateOf(ActiveScreenMode.VIEWPORT_3D) }
    var activeEditingSketch by remember { mutableStateOf<Sketch2D?>(null) }

    // Undo / Redo History Stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    fun pushUndoState() {
        undoStack.add(ProjectSerializer.toJson(currentProject))
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
    }

    // Dialog & Panel States
    var showFeatureTree by remember { mutableStateOf(false) }
    var showPropertiesPanel by remember { mutableStateOf(false) }
    var showRoboticsLibrary by remember { mutableStateOf(false) }
    var showAssemblyPanel by remember { mutableStateOf(false) }
    var showProjectManager by remember { mutableStateOf(false) }
    var showCopilotDialog by remember { mutableStateOf(false) }
    var showPrimitivesDialog by remember { mutableStateOf(false) }

    // Feature creation dialog (Extrude, Cut, Hole, etc.)
    var pendingFeatureType by remember { mutableStateOf<FeatureType?>(null) }
    var editingFeature by remember { mutableStateOf<Feature?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CadStrings.get("app_title", currentLang),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CadCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Offline Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x3300E676),
                            border = BorderStroke(1.dp, CadGreen)
                        ) {
                            Text(
                                text = "OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CadGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Undo & Redo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(ProjectSerializer.toJson(currentProject))
                                val prevJson = undoStack.removeAt(undoStack.lastIndex)
                                currentProject = ProjectSerializer.fromJson(prevJson)
                            }
                        },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.testTag("app_undo_btn")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray)
                    }

                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(ProjectSerializer.toJson(currentProject))
                                val nextJson = redoStack.removeAt(redoStack.lastIndex)
                                currentProject = ProjectSerializer.fromJson(nextJson)
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.testTag("app_redo_btn")
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray)
                    }

                    // Language Toggle (EN / AR)
                    TextButton(
                        onClick = {
                            currentLang = if (currentLang == CadLanguage.EN) CadLanguage.AR else CadLanguage.EN
                        },
                        modifier = Modifier.testTag("lang_toggle_btn")
                    ) {
                        Text(
                            text = CadStrings.get("lang_toggle", currentLang),
                            color = CadOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Feature Tree Toggle
                    IconButton(onClick = { showFeatureTree = !showFeatureTree }, modifier = Modifier.testTag("toggle_feature_tree_btn")) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Features", tint = Color.White)
                    }

                    // AI Copilot
                    IconButton(onClick = { showCopilotDialog = true }, modifier = Modifier.testTag("open_copilot_btn")) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "CAD Copilot", tint = CadCyan)
                    }

                    // Projects / Export
                    IconButton(onClick = { showProjectManager = true }, modifier = Modifier.testTag("open_projects_btn")) {
                        Icon(Icons.Default.Folder, contentDescription = "Projects", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CadSurfaceDark)
            )
        },
        bottomBar = {
            if (activeMode == ActiveScreenMode.VIEWPORT_3D) {
                Surface(
                    color = CadSurfaceDark,
                    border = BorderStroke(1.dp, CadBorder),
                    modifier = Modifier.fillMaxWidth().testTag("cad_bottom_toolbar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Sketch Button
                        Button(
                            onClick = {
                                pushUndoState()
                                val newSketch = Sketch2D(name = "Sketch ${currentProject.sketches.size + 1}")
                                currentProject.sketches.add(newSketch)
                                activeEditingSketch = newSketch
                                activeMode = ActiveScreenMode.SKETCH_2D
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("start_sketch_btn")
                        ) {
                            Icon(Icons.Default.Create, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(CadStrings.get("sketch", currentLang), color = CadBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // 2. Extrude Button
                        Button(
                            onClick = {
                                if (currentProject.sketches.isNotEmpty()) {
                                    pendingFeatureType = FeatureType.EXTRUDE
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toolbar_extrude_btn")
                        ) {
                            Text(CadStrings.get("extrude", currentLang), color = Color.White, fontSize = 12.sp)
                        }

                        // 3. Cut-Extrude Button
                        Button(
                            onClick = {
                                if (currentProject.sketches.isNotEmpty()) {
                                    pendingFeatureType = FeatureType.CUT_EXTRUDE
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toolbar_cut_btn")
                        ) {
                            Text(CadStrings.get("cut_extrude", currentLang), color = CadOrange, fontSize = 12.sp)
                        }

                        // 4. Revolve Button
                        Button(
                            onClick = {
                                if (currentProject.sketches.isNotEmpty()) {
                                    pendingFeatureType = FeatureType.REVOLVE
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(CadStrings.get("revolve", currentLang), color = Color.White, fontSize = 12.sp)
                        }

                        // 5. Hole Button
                        Button(
                            onClick = { pendingFeatureType = FeatureType.HOLE },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(CadStrings.get("hole", currentLang), color = Color.White, fontSize = 12.sp)
                        }

                        // 6. 3D Primitives Button
                        Button(
                            onClick = { showPrimitivesDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toolbar_primitives_btn")
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = CadCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("3D Primitives", color = Color.White, fontSize = 12.sp)
                        }

                        // 7. Fillet Button
                        Button(
                            onClick = {
                                pushUndoState()
                                val f = Feature(name = "Fillet", type = FeatureType.FILLET)
                                f.numericParams["radius"] = 2.0
                                currentProject.features.add(f)
                                ParametricKernel.recompute(currentProject)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(CadStrings.get("fillet", currentLang), color = Color.White, fontSize = 12.sp)
                        }

                        // 7. Shell Button
                        Button(
                            onClick = {
                                pushUndoState()
                                val f = Feature(name = "Shell", type = FeatureType.SHELL)
                                f.numericParams["thickness"] = 2.0
                                currentProject.features.add(f)
                                ParametricKernel.recompute(currentProject)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(CadStrings.get("shell", currentLang), color = Color.White, fontSize = 12.sp)
                        }

                        // 8. Robotics Library
                        Button(
                            onClick = { showRoboticsLibrary = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CadOrange),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toolbar_robotics_btn")
                        ) {
                            Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(CadStrings.get("robotics", currentLang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // 9. Assembly
                        Button(
                            onClick = { showAssemblyPanel = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("toolbar_assembly_btn")
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = CadCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(CadStrings.get("assembly", currentLang), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeMode) {
                ActiveScreenMode.VIEWPORT_3D -> {
                    ViewportView(
                        mesh = currentProject.generatedMesh,
                        assemblyParts = currentProject.assemblyParts,
                        camera = camera,
                        lang = currentLang,
                        onOpenProperties = { showPropertiesPanel = true }
                    )
                }
                ActiveScreenMode.SKETCH_2D -> {
                    val activeSketch = activeEditingSketch ?: currentProject.sketches.lastOrNull()
                    if (activeSketch != null) {
                        SketchEditorView(
                            sketch = activeSketch,
                            lang = currentLang,
                            onFinish = {
                                pushUndoState()
                                // Auto-extrude prompt or return to 3D
                                val feat = Feature(
                                    name = "Extrude ${currentProject.features.size + 1}",
                                    type = FeatureType.EXTRUDE,
                                    sketchId = activeSketch.id
                                )
                                feat.numericParams["depth"] = 15.0
                                currentProject.features.add(feat)
                                ParametricKernel.recompute(currentProject)
                                camera.fitToBoundingBox(currentProject.generatedMesh?.boundingBox ?: BoundingBox.EMPTY)
                                activeMode = ActiveScreenMode.VIEWPORT_3D
                            },
                            onCancel = {
                                activeMode = ActiveScreenMode.VIEWPORT_3D
                            }
                        )
                    }
                }
            }

            // Feature Tree Drawer Overlay
            if (showFeatureTree) {
                FeatureTreeDrawer(
                    project = currentProject,
                    lang = currentLang,
                    onEditFeature = { feat ->
                        editingFeature = feat
                    },
                    onDeleteFeature = { feat ->
                        pushUndoState()
                        currentProject.features.remove(feat)
                        ParametricKernel.recompute(currentProject)
                    },
                    onToggleSuppress = { feat ->
                        pushUndoState()
                        feat.isSuppressed = !feat.isSuppressed
                        ParametricKernel.recompute(currentProject)
                    },
                    onAddFeature = { fType ->
                        pendingFeatureType = fType
                    },
                    onClose = { showFeatureTree = false }
                )
            }

            // Properties Panel Bottom Sheet
            if (showPropertiesPanel) {
                PropertiesPanel(
                    project = currentProject,
                    lang = currentLang,
                    onMaterialChanged = { newMat ->
                        currentProject.material = newMat
                    },
                    onClose = { showPropertiesPanel = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Assembly Panel Bottom Sheet
            if (showAssemblyPanel) {
                AssemblyJointsPanel(
                    project = currentProject,
                    lang = currentLang,
                    onJointValueChanged = { joint, newVal ->
                        // Recompute assembly part transformations
                        for (part in currentProject.assemblyParts) {
                            if (part.id == joint.part2Id) {
                                when (joint.type) {
                                    JointType.REVOLUTE -> part.rotationDeg = Vector3D(0.0, 0.0, newVal)
                                    JointType.SLIDER -> part.translation = Vector3D(newVal, 0.0, 0.0)
                                    else -> {}
                                }
                            }
                        }
                    },
                    onAddPartInstance = {
                        pushUndoState()
                        val newPart = AssemblyPartInstance(
                            partDefId = currentProject.id,
                            name = "Part ${currentProject.assemblyParts.size + 1}",
                            translation = Vector3D(20.0 * (currentProject.assemblyParts.size + 1), 0.0, 0.0)
                        )
                        currentProject.assemblyParts.add(newPart)
                    },
                    onAddJoint = { jType ->
                        pushUndoState()
                        val j = AssemblyJoint(
                            name = "${jType.name} Joint",
                            type = jType,
                            part1Id = "main",
                            part2Id = currentProject.assemblyParts.lastOrNull()?.id ?: "main"
                        )
                        currentProject.assemblyJoints.add(j)
                    },
                    onClose = { showAssemblyPanel = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Robotics Library Dialog
            if (showRoboticsLibrary) {
                RoboticsLibraryDialog(
                    lang = currentLang,
                    onSelectComponent = { compId, params ->
                        pushUndoState()
                        val feat = Feature(
                            name = "Robotics: $compId",
                            type = FeatureType.ROBOTICS_COMPONENT
                        )
                        feat.stringParams["component"] = compId
                        feat.numericParams.putAll(params)
                        currentProject.features.add(feat)
                        ParametricKernel.recompute(currentProject)
                        camera.fitToBoundingBox(currentProject.generatedMesh?.boundingBox ?: BoundingBox.EMPTY)
                        showRoboticsLibrary = false
                    },
                    onDismiss = { showRoboticsLibrary = false }
                )
            }

            // Project Manager Dialog
            if (showProjectManager) {
                ProjectManagerDialog(
                    currentProject = currentProject,
                    lang = currentLang,
                    onProjectLoaded = { loadedProj ->
                        currentProject = loadedProj
                        undoStack.clear()
                        redoStack.clear()
                        camera.fitToBoundingBox(loadedProj.generatedMesh?.boundingBox ?: BoundingBox.EMPTY)
                    },
                    onDismiss = { showProjectManager = false }
                )
            }

            // AI CAD Copilot Dialog
            if (showCopilotDialog) {
                CadCopilotDialog(
                    project = currentProject,
                    lang = currentLang,
                    onDismiss = { showCopilotDialog = false }
                )
            }

            // 3D Primitives Selection Dialog
            if (showPrimitivesDialog) {
                Dialog(onDismissRequest = { showPrimitivesDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
                        modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("primitives_dialog")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Insert 3D Primitive", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                IconButton(onClick = { showPrimitivesDialog = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val prims = listOf(
                                Pair(FeatureType.PRIMITIVE_BOX, "Solid Box / Cube (30×30×20 mm)"),
                                Pair(FeatureType.PRIMITIVE_CYLINDER, "Solid Cylinder (r=15, h=30 mm)"),
                                Pair(FeatureType.PRIMITIVE_SPHERE, "Solid Sphere (r=15 mm)"),
                                Pair(FeatureType.PRIMITIVE_CONE, "Solid Cone (r=15, h=30 mm)"),
                                Pair(FeatureType.PRIMITIVE_TORUS, "Solid Torus / Ring (R=20, r=6 mm)")
                            )
                            for ((fType, label) in prims) {
                                Card(
                                    onClick = {
                                        pushUndoState()
                                        val f = Feature(name = label.substringBefore(" ("), type = fType)
                                        when (fType) {
                                            FeatureType.PRIMITIVE_BOX -> { f.numericParams["width"] = 30.0; f.numericParams["length"] = 30.0; f.numericParams["height"] = 20.0 }
                                            FeatureType.PRIMITIVE_CYLINDER -> { f.numericParams["radius"] = 15.0; f.numericParams["height"] = 30.0 }
                                            FeatureType.PRIMITIVE_SPHERE -> { f.numericParams["radius"] = 15.0 }
                                            FeatureType.PRIMITIVE_CONE -> { f.numericParams["radius"] = 15.0; f.numericParams["height"] = 30.0 }
                                            FeatureType.PRIMITIVE_TORUS -> { f.numericParams["majorRadius"] = 20.0; f.numericParams["minorRadius"] = 6.0 }
                                            else -> {}
                                        }
                                        currentProject.features.add(f)
                                        ParametricKernel.recompute(currentProject)
                                        ProjectSerializer.autosaveProject(context, currentProject)
                                        camera.fitToBoundingBox(currentProject.generatedMesh?.boundingBox ?: BoundingBox.EMPTY)
                                        showPrimitivesDialog = false
                                    },
                                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("add_prim_${fType.name}")
                                ) {
                                    Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Numeric Keypad for Pending Extrude / Cut Depth
            if (pendingFeatureType != null) {
                val fType = pendingFeatureType!!
                NumericKeypadDialog(
                    title = when (fType) {
                        FeatureType.EXTRUDE -> CadStrings.get("depth", currentLang)
                        FeatureType.CUT_EXTRUDE -> CadStrings.get("cut_extrude", currentLang) + " " + CadStrings.get("depth", currentLang)
                        FeatureType.HOLE -> CadStrings.get("diameter", currentLang)
                        else -> "Dimension (mm)"
                    },
                    initialValue = if (fType == FeatureType.HOLE) 3.4 else 12.0,
                    unit = "mm",
                    lang = currentLang,
                    onConfirm = { numVal ->
                        pushUndoState()
                        when (fType) {
                            FeatureType.EXTRUDE -> {
                                val sId = currentProject.sketches.lastOrNull()?.id
                                val feat = Feature(name = "Extrude", type = FeatureType.EXTRUDE, sketchId = sId)
                                feat.numericParams["depth"] = numVal
                                currentProject.features.add(feat)
                            }
                            FeatureType.CUT_EXTRUDE -> {
                                val sId = currentProject.sketches.lastOrNull()?.id
                                val feat = Feature(name = "Cut", type = FeatureType.CUT_EXTRUDE, sketchId = sId)
                                feat.numericParams["depth"] = numVal
                                currentProject.features.add(feat)
                            }
                            FeatureType.HOLE -> {
                                val feat = Feature(name = "M${numVal.toInt()} Hole", type = FeatureType.HOLE)
                                feat.numericParams["diameter"] = numVal
                                feat.numericParams["depth"] = 20.0
                                currentProject.features.add(feat)
                            }
                            else -> {}
                        }
                        ParametricKernel.recompute(currentProject)
                        camera.fitToBoundingBox(currentProject.generatedMesh?.boundingBox ?: BoundingBox.EMPTY)
                        pendingFeatureType = null
                    },
                    onDismiss = { pendingFeatureType = null }
                )
            }

            // Numeric Keypad for Editing Feature Parameters
            if (editingFeature != null) {
                val feat = editingFeature!!
                val paramKey = feat.numericParams.keys.firstOrNull() ?: "depth"
                val curVal = feat.numericParams[paramKey] ?: 10.0
                NumericKeypadDialog(
                    title = "Edit ${feat.name} ($paramKey)",
                    initialValue = curVal,
                    unit = "mm",
                    lang = currentLang,
                    onConfirm = { newVal ->
                        pushUndoState()
                        feat.numericParams[paramKey] = newVal
                        ParametricKernel.recompute(currentProject)
                        editingFeature = null
                    },
                    onDismiss = { editingFeature = null }
                )
            }
        }
    }
}
