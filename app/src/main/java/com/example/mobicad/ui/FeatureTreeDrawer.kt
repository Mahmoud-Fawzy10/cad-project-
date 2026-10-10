package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.ui.theme.*

@Composable
fun FeatureTreeDrawer(
    project: CadProject,
    lang: CadLanguage,
    onEditFeature: (Feature) -> Unit,
    onDeleteFeature: (Feature) -> Unit,
    onToggleSuppress: (Feature) -> Unit,
    onAddFeature: (FeatureType) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("feature_tree_drawer"),
        color = CadSurfaceDark,
        border = BorderStroke(1.dp, CadBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = CadCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = CadStrings.get("features", lang),
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
                text = "${project.name} (${project.features.size} features)",
                fontSize = 12.sp,
                color = CadTextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Divider(color = CadBorder)

            Spacer(modifier = Modifier.height(8.dp))

            // Add Feature Bar
            Text(
                text = "Add 3D Operation:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = CadCyan,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onAddFeature(FeatureType.EXTRUDE) },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("add_extrude_btn")
                ) {
                    Text("+ Extrude", fontSize = 11.sp, color = CadCyan)
                }
                Button(
                    onClick = { onAddFeature(FeatureType.CUT_EXTRUDE) },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("add_cut_btn")
                ) {
                    Text("+ Cut", fontSize = 11.sp, color = CadOrange)
                }
                Button(
                    onClick = { onAddFeature(FeatureType.HOLE) },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("add_hole_btn")
                ) {
                    Text("+ Hole", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feature List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(project.features) { feat ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (feat.isSuppressed) Color(0xFF1E293B) else CadSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("feat_card_${feat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon according to feature type
                            val icon = when (feat.type) {
                                FeatureType.EXTRUDE -> Icons.Default.VerticalAlignTop
                                FeatureType.CUT_EXTRUDE -> Icons.Default.VerticalAlignBottom
                                FeatureType.REVOLVE -> Icons.Default.RotateRight
                                FeatureType.HOLE -> Icons.Default.HighlightOff
                                FeatureType.FILLET -> Icons.Default.RoundedCorner
                                FeatureType.SHELL -> Icons.Default.CropFree
                                FeatureType.ROBOTICS_COMPONENT -> Icons.Default.PrecisionManufacturing
                                else -> Icons.Default.Layers
                            }

                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (feat.isSuppressed) Color.Gray else if (feat.isValid) CadCyan else CadRed,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feat.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (feat.isSuppressed) Color.Gray else Color.White
                                )
                                val paramSummary = feat.numericParams.entries.joinToString(", ") { "${it.key}: ${it.value}mm" }
                                if (paramSummary.isNotEmpty()) {
                                    Text(
                                        text = paramSummary,
                                        fontSize = 11.sp,
                                        color = CadTextSecondary
                                    )
                                }
                                if (!feat.isValid && feat.errorMessage != null) {
                                    Text(
                                        text = feat.errorMessage!!,
                                        fontSize = 10.sp,
                                        color = CadRed
                                    )
                                }
                            }

                            // Actions
                            IconButton(onClick = { onToggleSuppress(feat) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (feat.isSuppressed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Suppress",
                                    tint = if (feat.isSuppressed) Color.Gray else CadCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onEditFeature(feat) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onDeleteFeature(feat) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CadRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
