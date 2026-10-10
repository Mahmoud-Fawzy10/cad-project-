package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
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
import java.util.Locale

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
    var selectedObjectId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize().background(CadViewportBg).testTag("viewport_view_root")) {
        // 1. Main Interactive 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val isHit = ViewportRenderer.hitTest(
                            tapOffset.x,
                            tapOffset.y,
                            camera,
                            mesh,
                            size.width.toDouble(),
                            size.height.toDouble()
                        )
                        selectedObjectId = if (isHit) "main" else null
                        triggerRedraw++
                    }
                }
                .pointerInput(navMode) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        if (zoom != 1f) {
                            // Two-finger pinch for zoom
                            camera.zoom(zoom.toDouble())
                            triggerRedraw++
                        }
                        if (pan != Offset.Zero) {
                            if (navMode == ViewportNavMode.ORBIT) {
                                // Dedicated touch orbit
                                camera.orbit(
                                    deltaYaw = pan.x.toDouble() * 0.4,
                                    deltaPitch = pan.y.toDouble() * 0.4
                                )
                            } else {
                                // Two-finger drag or pan mode
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
            val redraw = triggerRedraw
            ViewportRenderer.render(
                drawScope = this,
                camera = camera,
                mesh = mesh,
                assemblyParts = assemblyParts,
                shadingMode = shadingMode,
                selectedPartId = selectedObjectId
            )
        }

        // 2. Active Mode Indicator Pill (Top Center)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CadSurfaceDark.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, if (selectedObjectId != null) CadOrange else CadBorder),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when {
                                selectedObjectId != null -> CadOrange
                                navMode == ViewportNavMode.ORBIT -> CadCyan
                                else -> CadGreen
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        selectedObjectId != null -> "Selected: Solid Body"
                        navMode == ViewportNavMode.ORBIT -> "Gesture: 1-Finger Orbit / 2-Finger Pinch Zoom"
                        else -> "Gesture: 2-Finger Pan / 2-Finger Pinch Zoom"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // 3. Top-Right Camera Controls Overlay
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CadSurfaceDark.copy(alpha = 0.92f),
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

                // Fit to Screen Button
                IconButton(
                    onClick = {
                        if (mesh != null) camera.fitToBoundingBox(mesh.boundingBox)
                        triggerRedraw++
                    },
                    modifier = Modifier.size(38.dp).testTag("fit_camera_btn")
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Fit to Screen", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Perspective / Orthographic Camera Toggle
                IconButton(
                    onClick = {
                        camera.isPerspective = !camera.isPerspective
                        triggerRedraw++
                    },
                    modifier = Modifier.size(38.dp).testTag("ortho_toggle_btn")
                ) {
                    Text(
                        text = if (camera.isPerspective) "PERSP" else "ORTHO",
                        color = CadCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Shading Mode Cycle (Shaded with Edges / Shaded / Wireframe)
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
                    Icon(Icons.Default.Brush, contentDescription = "Shading Mode", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // 4. Top-Left Quick Standard Views Selector
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CadSurfaceDark.copy(alpha = 0.92f),
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

        // 5. Dedicated On-Screen Orbit Virtual Joystick / Trackpad (Bottom Left)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CadSurfaceDark.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, CadBorder),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
                .size(108.dp)
                .testTag("onscreen_orbit_control")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            camera.orbit(
                                deltaYaw = dragAmount.x.toDouble() * 0.7,
                                deltaPitch = dragAmount.y.toDouble() * 0.7
                            )
                            triggerRedraw++
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background Trackball Dial
                Canvas(modifier = Modifier.size(88.dp)) {
                    drawCircle(Color(0x3000E5FF), radius = size.minDimension / 2f)
                    drawCircle(CadCyan, radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))
                }
                // Directional labels
                Text("▲", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                Text("▼", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                Text("◀", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp))
                Text("▶", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp))

                // Center Thumb
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(CadCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Orbit Trackball",
                        tint = CadBackgroundDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 6. Selected Object Inspector Floating HUD (Bottom Center)
        if (selectedObjectId != null && mesh != null) {
            val bb = mesh.boundingBox
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CadSurfaceDark.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CadOrange),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .widthIn(max = 280.dp)
                    .testTag("selected_object_hud")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Solid Body Selected", fontWeight = FontWeight.Bold, color = CadOrange, fontSize = 12.sp)
                        IconButton(onClick = { selectedObjectId = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = String.format(Locale.US, "Size: %.1f × %.1f × %.1f mm", bb.width, bb.height, bb.depth),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenProperties,
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Inspect Mass", fontSize = 10.sp, color = CadCyan)
                        }
                        Button(
                            onClick = {
                                camera.fitToBoundingBox(bb)
                                triggerRedraw++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Frame Target", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // 7. Bottom-Right Quick Inspect FAB
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
