package com.example.mobicad.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.mobicad.geometry.ObjExporter
import com.example.mobicad.geometry.ProjectSerializer
import com.example.mobicad.geometry.StlExporter
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.CadProject
import com.example.ui.theme.*
import java.io.File

@Composable
fun ProjectManagerDialog(
    currentProject: CadProject,
    lang: CadLanguage,
    onProjectLoaded: (CadProject) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localFiles by remember { mutableStateOf(ProjectSerializer.listLocalProjects(context)) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(4.dp)
                .testTag("project_manager_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = CadCyan)
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
                    text = "100% Offline Local Storage & Mechanical Export (STL, OBJ, JSON)",
                    fontSize = 11.sp,
                    color = CadTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider(color = CadBorder)

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Project Actions (Save / New)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val savedFile = ProjectSerializer.saveProjectLocally(context, currentProject)
                            localFiles = ProjectSerializer.listLocalProjects(context)
                            Toast.makeText(context, "Saved: ${savedFile.name}", Toast.LENGTH_SHORT).show()
                            exportStatusMessage = "Project '${currentProject.name}' saved locally."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("save_project_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(CadStrings.get("save_project", lang), fontSize = 12.sp, color = CadBackgroundDark, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val newProj = CadProject(name = "New Part")
                            onProjectLoaded(newProj)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("new_project_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(CadStrings.get("new_project", lang), fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Options
                Text(
                    text = CadStrings.get("export", lang) + ":",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CadOrange
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val mesh = currentProject.generatedMesh
                            if (mesh != null && mesh.triangles.isNotEmpty()) {
                                val stlContent = StlExporter.exportToAsciiStl(mesh, currentProject.name)
                                val outDir = File(context.filesDir, "exports")
                                if (!outDir.exists()) outDir.mkdirs()
                                val outFile = File(outDir, "${currentProject.name.replace(" ", "_")}.stl")
                                outFile.writeText(stlContent)
                                exportStatusMessage = "Exported STL (${mesh.triangles.size} facets) to ${outFile.name}"
                                Toast.makeText(context, "Exported STL: ${outFile.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                exportStatusMessage = "Error: No solid geometry to export."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("export_stl_btn")
                    ) {
                        Text("STL (3D Print)", fontSize = 11.sp, color = CadCyan)
                    }

                    Button(
                        onClick = {
                            val mesh = currentProject.generatedMesh
                            if (mesh != null && mesh.triangles.isNotEmpty()) {
                                val objContent = ObjExporter.exportToObj(mesh, currentProject.name)
                                val outDir = File(context.filesDir, "exports")
                                if (!outDir.exists()) outDir.mkdirs()
                                val outFile = File(outDir, "${currentProject.name.replace(" ", "_")}.obj")
                                outFile.writeText(objContent)
                                exportStatusMessage = "Exported OBJ (${mesh.triangles.size} facets) to ${outFile.name}"
                                Toast.makeText(context, "Exported OBJ: ${outFile.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                exportStatusMessage = "Error: No solid geometry to export."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("export_obj_btn")
                    ) {
                        Text("OBJ Mesh", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val outDir = File(context.filesDir, "exports")
                            if (!outDir.exists()) outDir.mkdirs()
                            val outFile = File(outDir, "${currentProject.name.replace(" ", "_")}.json")
                            outFile.writeText(ProjectSerializer.toJson(currentProject))
                            exportStatusMessage = "Exported Parametric JSON to ${outFile.name}"
                            Toast.makeText(context, "Exported JSON: ${outFile.name}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                        modifier = Modifier.weight(1f).height(38.dp).testTag("export_json_btn")
                    ) {
                        Text("JSON", fontSize = 11.sp, color = CadOrange)
                    }
                }

                if (exportStatusMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exportStatusMessage!!,
                        fontSize = 11.sp,
                        color = CadGreen,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pre-built Samples
                Text(
                    text = "Load Robotics Sample Project:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CadCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val samples = listOf("ESP32 Enclosure", "NEMA 17 Mount", "Differential Chassis")
                    for (s in samples) {
                        OutlinedButton(
                            onClick = {
                                val proj = ProjectSerializer.createSampleProject(s)
                                onProjectLoaded(proj)
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(34.dp)
                        ) {
                            Text(s, fontSize = 10.sp, color = Color.White, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Local Files List
                Text(
                    text = "Saved Local Projects (${localFiles.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CadTextSecondary
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(localFiles) { (fileId, projName) ->
                        Card(
                            onClick = {
                                val loaded = ProjectSerializer.loadProjectLocally(context, fileId)
                                if (loaded != null) {
                                    onProjectLoaded(loaded)
                                    onDismiss()
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(projName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text(fileId, fontSize = 10.sp, color = CadTextSecondary)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = "Open", tint = CadCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
