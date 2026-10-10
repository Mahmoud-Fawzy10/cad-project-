package com.example.mobicad.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobicad.geometry.ObjExporter
import com.example.mobicad.geometry.ParametricKernel
import com.example.mobicad.geometry.ProjectSerializer
import com.example.mobicad.geometry.StlExporter
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.CadProject
import com.example.mobicad.model.Feature
import com.example.mobicad.model.FeatureType
import com.example.mobicad.model.Material
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProjectManagerTab {
    BROWSE,
    CREATE_NEW,
    SAVE_EXPORT,
    SAMPLES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagerDialog(
    currentProject: CadProject,
    lang: CadLanguage,
    onProjectLoaded: (CadProject) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ProjectManagerTab.BROWSE) }
    var localProjects by remember { mutableStateOf(ProjectSerializer.listProjectMetadata(context)) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    val hasAutosave = remember { ProjectSerializer.hasAutosavedProject(context) }

    // New Project Form State
    var newProjectName by remember { mutableStateOf("Robotics Part ${localProjects.size + 1}") }
    var selectedUnit by remember { mutableStateOf("mm") }
    var selectedMaterial by remember { mutableStateOf(Material.PLA) }
    var selectedTemplate by remember { mutableStateOf("Blank") }

    // Save As Form State
    var saveAsName by remember { mutableStateOf(currentProject.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(4.dp)
                .testTag("project_manager_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CadCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = CadStrings.get("projects", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "100% Offline Local Storage & Native Parametric JSON",
                    fontSize = 11.sp,
                    color = CadTextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Autosave Recovery Banner (if available)
                if (hasAutosave) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x33FFB300),
                        border = BorderStroke(1.dp, CadAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Autosaved session detected", fontSize = 11.sp, color = CadAmber, fontWeight = FontWeight.SemiBold)
                            Button(
                                onClick = {
                                    val restored = ProjectSerializer.loadAutosavedProject(context)
                                    if (restored != null) {
                                        onProjectLoaded(restored)
                                        Toast.makeText(context, "Autosaved project restored", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CadAmber),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Restore", fontSize = 10.sp, color = CadBackgroundDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Tab Selector
                SecondaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = CadSurfaceVariant,
                    contentColor = CadCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == ProjectManagerTab.BROWSE,
                        onClick = { selectedTab = ProjectManagerTab.BROWSE },
                        text = { Text("Open (${localProjects.size})", fontSize = 11.sp, maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == ProjectManagerTab.CREATE_NEW,
                        onClick = { selectedTab = ProjectManagerTab.CREATE_NEW },
                        text = { Text("New", fontSize = 11.sp, maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == ProjectManagerTab.SAVE_EXPORT,
                        onClick = { selectedTab = ProjectManagerTab.SAVE_EXPORT },
                        text = { Text("Export", fontSize = 11.sp, maxLines = 1) }
                    )
                    Tab(
                        selected = selectedTab == ProjectManagerTab.SAMPLES,
                        onClick = { selectedTab = ProjectManagerTab.SAMPLES },
                        text = { Text("Samples", fontSize = 11.sp, maxLines = 1) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content
                when (selectedTab) {
                    ProjectManagerTab.BROWSE -> {
                        // List Saved Projects
                        if (localProjects.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CadTextSecondary, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No saved projects found on device.", color = CadTextSecondary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            ProjectSerializer.saveProjectLocally(context, currentProject)
                                            localProjects = ProjectSerializer.listProjectMetadata(context)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CadCyan)
                                    ) {
                                        Text("Save Current Project Now", color = CadBackgroundDark, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(localProjects) { meta ->
                                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(meta.lastModified))
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                                        modifier = Modifier.fillMaxWidth().testTag("local_project_card_${meta.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(meta.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("${meta.featureCount} operations • ${meta.materialName} • $dateStr", fontSize = 11.sp, color = CadTextSecondary)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Duplicate
                                                IconButton(
                                                    onClick = {
                                                        val copy = ProjectSerializer.duplicateProjectLocally(context, meta.id)
                                                        if (copy != null) {
                                                            localProjects = ProjectSerializer.listProjectMetadata(context)
                                                            Toast.makeText(context, "Duplicated: ${copy.name}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }

                                                // Delete
                                                IconButton(
                                                    onClick = {
                                                        ProjectSerializer.deleteProjectLocally(context, meta.id)
                                                        localProjects = ProjectSerializer.listProjectMetadata(context)
                                                        Toast.makeText(context, "Deleted project", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CadRed, modifier = Modifier.size(16.dp))
                                                }

                                                // Open
                                                IconButton(
                                                    onClick = {
                                                        val loaded = ProjectSerializer.loadProjectLocally(context, meta.id)
                                                        if (loaded != null) {
                                                            onProjectLoaded(loaded)
                                                            Toast.makeText(context, "Loaded: ${loaded.name}", Toast.LENGTH_SHORT).show()
                                                            onDismiss()
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp).background(CadCyan, RoundedCornerShape(6.dp))
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open", tint = CadBackgroundDark, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ProjectManagerTab.CREATE_NEW -> {
                        // Create New Project Form
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Project Name:", fontSize = 12.sp, color = CadCyan, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = newProjectName,
                                onValueChange = { newProjectName = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CadCyan,
                                    unfocusedBorderColor = CadBorder
                                )
                            )

                            Text("Units & Canonical Scale:", fontSize = 12.sp, color = CadCyan, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val units = listOf("mm", "cm", "inch")
                                for (u in units) {
                                    val isSel = (u == selectedUnit)
                                    Button(
                                        onClick = { selectedUnit = u },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) CadCyan else CadSurfaceVariant),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(u, color = if (isSel) CadBackgroundDark else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("Initial Engineering Material:", fontSize = 12.sp, color = CadCyan, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val mats = listOf(Material.PLA, Material.ALUMINUM_6061, Material.STEEL_1018)
                                for (m in mats) {
                                    val isSel = (m.name == selectedMaterial.name)
                                    Button(
                                        onClick = { selectedMaterial = m },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) CadOrange else CadSurfaceVariant),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(m.name.substringBefore(" "), color = if (isSel) CadBackgroundDark else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Text("Initial Starting Geometry:", fontSize = 12.sp, color = CadCyan, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val tmpls = listOf("Blank", "Cube", "Cylinder")
                                for (t in tmpls) {
                                    val isSel = (t == selectedTemplate)
                                    OutlinedButton(
                                        onClick = { selectedTemplate = t },
                                        border = BorderStroke(1.dp, if (isSel) CadCyan else CadBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(t, color = if (isSel) CadCyan else Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    val newProj = CadProject(
                                        name = newProjectName.ifBlank { "Untitled Project" },
                                        units = selectedUnit,
                                        material = selectedMaterial
                                    )
                                    when (selectedTemplate) {
                                        "Cube" -> {
                                            val f = Feature(name = "Initial Cube", type = FeatureType.PRIMITIVE_BOX)
                                            f.numericParams["width"] = 25.0
                                            f.numericParams["length"] = 25.0
                                            f.numericParams["height"] = 20.0
                                            newProj.features.add(f)
                                            ParametricKernel.recompute(newProj)
                                        }
                                        "Cylinder" -> {
                                            val f = Feature(name = "Initial Cylinder", type = FeatureType.PRIMITIVE_CYLINDER)
                                            f.numericParams["radius"] = 12.0
                                            f.numericParams["height"] = 25.0
                                            newProj.features.add(f)
                                            ParametricKernel.recompute(newProj)
                                        }
                                    }
                                    ProjectSerializer.saveProjectLocally(context, newProj)
                                    onProjectLoaded(newProj)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_create_new_project_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CadBackgroundDark)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create & Open Project", color = CadBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    ProjectManagerTab.SAVE_EXPORT -> {
                        // Save & Export Tab
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Save Current Model State:", fontSize = 12.sp, color = CadCyan, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val f = ProjectSerializer.saveProjectLocally(context, currentProject)
                                        ProjectSerializer.autosaveProject(context, currentProject)
                                        localProjects = ProjectSerializer.listProjectMetadata(context)
                                        Toast.makeText(context, "Saved successfully: ${f.name}", Toast.LENGTH_SHORT).show()
                                        exportStatusMessage = "Project '${currentProject.name}' saved to local storage."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                                    modifier = Modifier.weight(1f).height(42.dp).testTag("save_current_btn")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Project", color = CadBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = CadBorder)

                            Text("Export for 3D Printing & CAD Interchange:", fontSize = 12.sp, color = CadOrange, fontWeight = FontWeight.SemiBold)

                            val mesh = currentProject.generatedMesh
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (mesh != null && mesh.triangles.isNotEmpty()) {
                                            val stl = StlExporter.exportToAsciiStl(mesh, currentProject.name)
                                            val dir = File(context.filesDir, "exports")
                                            if (!dir.exists()) dir.mkdirs()
                                            val out = File(dir, "${currentProject.name.replace(" ", "_")}.stl")
                                            out.writeText(stl)
                                            exportStatusMessage = "Exported STL (${mesh.triangles.size} facets, ${out.length() / 1024} KB) to ${out.name}"
                                            Toast.makeText(context, "Exported STL: ${out.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            exportStatusMessage = "No 3D geometry available to export."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("STL (Slicer)", color = CadCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (mesh != null && mesh.triangles.isNotEmpty()) {
                                            val obj = ObjExporter.exportToObj(mesh, currentProject.name)
                                            val dir = File(context.filesDir, "exports")
                                            if (!dir.exists()) dir.mkdirs()
                                            val out = File(dir, "${currentProject.name.replace(" ", "_")}.obj")
                                            out.writeText(obj)
                                            exportStatusMessage = "Exported OBJ (${mesh.triangles.size} facets) to ${out.name}"
                                            Toast.makeText(context, "Exported OBJ: ${out.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            exportStatusMessage = "No 3D geometry available to export."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Wavefront OBJ", color = Color.White, fontSize = 11.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val json = ProjectSerializer.toJson(currentProject)
                                        val dir = File(context.filesDir, "exports")
                                        if (!dir.exists()) dir.mkdirs()
                                        val out = File(dir, "${currentProject.name.replace(" ", "_")}.json")
                                        out.writeText(json)
                                        exportStatusMessage = "Exported Parametric JSON (${out.length() / 1024} KB) to ${out.name}"
                                        Toast.makeText(context, "Exported JSON: ${out.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Parametric JSON", color = CadOrange, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val vol = mesh?.calculateVolume() ?: 0.0
                                        val mass = currentProject.material.calculateMassGrams(vol)
                                        val csv = "Part Name,Material,Density (g/cm3),Volume (mm3),Mass (g),Triangles\n\"${currentProject.name}\",\"${currentProject.material.name}\",${currentProject.material.densityGramsPerCm3},${vol},${mass},${mesh?.triangles?.size ?: 0}\n"
                                        val dir = File(context.filesDir, "exports")
                                        if (!dir.exists()) dir.mkdirs()
                                        val out = File(dir, "${currentProject.name.replace(" ", "_")}_BOM.csv")
                                        out.writeText(csv)
                                        exportStatusMessage = "Exported BOM CSV report to ${out.name}"
                                        Toast.makeText(context, "Exported BOM CSV", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("BOM Report CSV", color = CadGreen, fontSize = 11.sp)
                                }
                            }

                            if (exportStatusMessage != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exportStatusMessage!!,
                                    fontSize = 11.sp,
                                    color = CadGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    ProjectManagerTab.SAMPLES -> {
                        // Robotics Preloaded Samples
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val samples = listOf(
                                Triple("ESP32 Enclosure", "Autonomous Sensor Enclosure", "ESP32 DevKit 30-pin housing with mounting bosses, USB cutout, and snap cover"),
                                Triple("NEMA 17 Mount", "Geared Stepper Actuator Mount", "Heavy duty 42.3mm stepper motor bracket with 31mm M3 bolt pattern"),
                                Triple("Differential Chassis", "Mobile Robot Chassis Plate", "120x90mm differential drive platform with TT motor pass-throughs and caster mount")
                            )

                            items(samples) { (sampleKey, title, desc) ->
                                Card(
                                    onClick = {
                                        val proj = ProjectSerializer.createSampleProject(sampleKey)
                                        ProjectSerializer.saveProjectLocally(context, proj)
                                        onProjectLoaded(proj)
                                        Toast.makeText(context, "Loaded $title", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(title, fontWeight = FontWeight.Bold, color = CadCyan, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(desc, fontSize = 11.sp, color = CadTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
