package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
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
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesPanel(
    project: CadProject,
    lang: CadLanguage,
    onMaterialChanged: (Material) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mesh = project.generatedMesh
    val volumeMm3 = mesh?.calculateVolume() ?: 0.0
    val volumeCm3 = volumeMm3 / 1000.0
    val surfaceAreaMm2 = mesh?.calculateSurfaceArea() ?: 0.0
    val massGrams = project.material.calculateMassGrams(volumeMm3)
    val com = mesh?.calculateCenterOfMass() ?: Vector3D.ZERO
    val bb = mesh?.boundingBox ?: BoundingBox.EMPTY

    var selectedMaterial by remember { mutableStateOf(project.material) }
    var materialDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .testTag("properties_panel_root"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = CadSurfaceDark,
        border = BorderStroke(1.dp, CadBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = CadCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CadStrings.get("inspect", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Text(
                text = "Analytical mass & volumetric properties computed directly from solid boundary",
                fontSize = 12.sp,
                color = CadTextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Material Selection
            Text(
                text = CadStrings.get("material", lang),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CadCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = materialDropdownExpanded,
                onExpandedChange = { materialDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${selectedMaterial.name} (${selectedMaterial.densityGramsPerCm3} g/cm³)",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = materialDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorder
                    )
                )

                ExposedDropdownMenu(
                    expanded = materialDropdownExpanded,
                    onDismissRequest = { materialDropdownExpanded = false },
                    modifier = Modifier.background(CadSurfaceVariant)
                ) {
                    for (mat in Material.PRESETS) {
                        DropdownMenuItem(
                            text = { Text("${mat.name} — ${mat.densityGramsPerCm3} g/cm³", color = Color.White) },
                            onClick = {
                                selectedMaterial = mat
                                onMaterialChanged(mat)
                                materialDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Properties Grid
            val props = listOf(
                Pair(CadStrings.get("mass", lang), String.format(Locale.US, "%.2f g  (%.4f kg)", massGrams, massGrams / 1000.0)),
                Pair(CadStrings.get("volume", lang), String.format(Locale.US, "%.1f mm³  (%.3f cm³)", volumeMm3, volumeCm3)),
                Pair(CadStrings.get("surface_area", lang), String.format(Locale.US, "%.1f mm²  (%.2f cm²)", surfaceAreaMm2, surfaceAreaMm2 / 100.0)),
                Pair(CadStrings.get("bounding_box", lang), String.format(Locale.US, "%.1f × %.1f × %.1f mm", bb.width, bb.height, bb.depth)),
                Pair(CadStrings.get("center_of_mass", lang), String.format(Locale.US, "X: %.2f, Y: %.2f, Z: %.2f mm", com.x, com.y, com.z)),
                Pair("Mesh Facets & Edges", "${mesh?.triangles?.size ?: 0} triangles, ${mesh?.edges?.size ?: 0} edges")
            )

            for ((label, value) in props) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontSize = 12.sp, color = CadTextSecondary)
                        Text(
                            text = value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CadCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
