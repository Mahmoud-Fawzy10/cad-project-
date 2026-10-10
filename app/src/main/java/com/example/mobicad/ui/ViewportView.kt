package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.mobicad.viewport.Camera3D
import com.example.mobicad.viewport.ShadingMode
import com.example.mobicad.viewport.ViewportRenderer
import com.example.ui.theme.*

enum class ViewportNavMode {
    ORBIT,
    PAN
}

@Composable
fun ViewportView(
    mesh: Mesh3D?,
    assemblyParts: List<AssemblyPartInstance>,
    camera: Camera3D,
    lang: CadLanguage,
    onOpenProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    var navMode by remember { mutableStateOf(ViewportNavMode.ORBIT) }
    var shadingMode by remember { mutableStateOf(ShadingMode.SHADED_EDGES) }
    var triggerRedraw by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize().background(CadViewportBg).testTag("viewport_view_root")) {
        // 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(navMode) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        if (zoom != 1f) {
                            camera.zoom(zoom.toDouble())
                            triggerRedraw++
                        }
                        if (pan != Offset.Zero) {
                            if (navMode == ViewportNavMode.ORBIT) {
                                camera.orbit(
                                    deltaYaw = pan.x.toDouble() * 0.4,
                                    deltaPitch = pan.y.toDouble() * 0.4
                                )
                            } else {
                                camera.pan(
                                    pan.x.toDouble(),
                                    pan.y.toDouble(),
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )
                            }
                            triggerRedraw++
                        }
                    }
                }
        ) {
            // Trigger recomposition on camera movement
            val redraw = triggerRedraw
            ViewportRenderer.render(
                drawScope = this,
                camera = camera,
                mesh = mesh,
                assemblyParts = assemblyParts,
                shadingMode = shadingMode
            )
        }

        // Top-Right Camera Controls Overlay
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CadSurfaceDark.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, CadBorder),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Orbit / Pan Mode Toggle
                IconButton(
                    onClick = {
                        navMode = if (navMode == ViewportNavMode.ORBIT) ViewportNavMode.PAN else ViewportNavMode.ORBIT
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (navMode == ViewportNavMode.ORBIT) CadCyan else CadOrange, RoundedCornerShape(8.dp))
                        .testTag("nav_mode_toggle_btn")
                ) {
                    Icon(
                        if (navMode == ViewportNavMode.ORBIT) Icons.Default.Rotate90DegreesCcw else Icons.Default.PanTool,
                        contentDescription = "Nav Mode",
                        tint = CadBackgroundDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Fit to Screen
                IconButton(
                    onClick = {
                        if (mesh != null) camera.fitToBoundingBox(mesh.boundingBox)
                        triggerRedraw++
                    },
                    modifier = Modifier.size(38.dp).testTag("fit_camera_btn")
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Perspective / Ortho Toggle
                IconButton(
                    onClick = {
                        camera.isPerspective = !camera.isPerspective
                        triggerRedraw++
                    },
                    modifier = Modifier.size(38.dp).testTag("ortho_toggle_btn")
                ) {
                    Text(
                        text = if (camera.isPerspective) "3D" else "2D",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Shading Mode Cycle
                IconButton(
                    onClick = {
                        shadingMode = when (shadingMode) {
                            ShadingMode.SHADED_EDGES -> ShadingMode.SHADED
                            ShadingMode.SHADED -> ShadingMode.WIREFRAME
                            ShadingMode.WIREFRAME -> ShadingMode.SHADED_EDGES
                        }
                        triggerRedraw++
                    },
                    modifier = Modifier.size(38.dp).testTag("shading_mode_btn")
                ) {
                    Icon(Icons.Default.Brush, contentDescription = "Shading", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Top-Left Quick Standard Views Selector
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CadSurfaceDark.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, CadBorder),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val views = listOf("ISO", "TOP", "FRONT", "RIGHT")
                for (v in views) {
                    Button(
                        onClick = {
                            camera.setPresetView(v)
                            triggerRedraw++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("view_preset_$v")
                    ) {
                        Text(v, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bottom-Right Quick Inspect FAB
        FloatingActionButton(
            onClick = onOpenProperties,
            containerColor = CadCyan,
            contentColor = CadBackgroundDark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("open_inspect_fab")
        ) {
            Icon(Icons.Default.SquareFoot, contentDescription = "Inspect Dimensions & Mass")
        }
    }
}
