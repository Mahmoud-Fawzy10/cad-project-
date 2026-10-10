package com.example.mobicad.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobicad.geometry.SketchSolver
import com.example.mobicad.localization.CadLanguage
import com.example.mobicad.localization.CadStrings
import com.example.mobicad.model.*
import com.example.ui.theme.*
import kotlin.math.*

enum class SketchTool {
    SELECT,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARC,
    DIMENSION,
    CONSTRAINT_HORIZ,
    CONSTRAINT_VERT,
    PAN
}

@Composable
fun SketchEditorView(
    sketch: Sketch2D,
    lang: CadLanguage,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTool by remember { mutableStateOf(SketchTool.RECTANGLE) }
    var panOffset by remember { mutableStateOf(Offset(400f, 400f)) }
    var zoomScale by remember { mutableFloatStateOf(4.0f) } // Pixels per mm

    var dragStartPoint by remember { mutableStateOf<Point2D?>(null) }
    var currentDragPoint by remember { mutableStateOf<Point2D?>(null) }
    var selectedEntityId by remember { mutableStateOf<String?>(null) }

    // Numeric keypad state
    var showKeypadForEntity by remember { mutableStateOf<Pair<String, Double>?>(null) }

    fun screenToWorld(screenOffset: Offset): Point2D {
        val wx = ((screenOffset.x - panOffset.x) / zoomScale).toDouble()
        val wy = (-(screenOffset.y - panOffset.y) / zoomScale).toDouble() // Inverted Y for Cartesian CAD
        // Snap to 1.0 mm grid
        val snapX = (wx * 2.0).roundToInt() / 2.0
        val snapY = (wy * 2.0).roundToInt() / 2.0
        return Point2D(snapX, snapY)
    }

    fun worldToScreen(pt: Point2D): Offset {
        val sx = (pt.x * zoomScale).toFloat() + panOffset.x
        val sy = -(pt.y * zoomScale).toFloat() + panOffset.y
        return Offset(sx, sy)
    }

    // Constraint status calculation
    val isConstrained = sketch.isFullyConstrained

    Box(modifier = modifier.fillMaxSize().background(CadBackgroundDark).testTag("sketch_editor_root")) {
        // 1. Interactive 2D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool) {
                    detectTapGestures { tapOffset ->
                        val worldPt = screenToWorld(tapOffset)
                        when (activeTool) {
                            SketchTool.CIRCLE -> {
                                sketch.addCircle(worldPt.x, worldPt.y, 10.0)
                                SketchSolver.solve(sketch)
                            }
                            SketchTool.SELECT, SketchTool.DIMENSION -> {
                                // Find closest entity
                                var foundId: String? = null
                                for (e in sketch.entities) {
                                    if (e is SketchEntity.RectangleEntity) {
                                        val p1 = sketch.points[e.pMinId]
                                        val p2 = sketch.points[e.pMaxId]
                                        if (p1 != null && p2 != null) {
                                            val minX = minOf(p1.x, p2.x); val maxX = maxOf(p1.x, p2.x)
                                            val minY = minOf(p1.y, p2.y); val maxY = maxOf(p1.y, p2.y)
                                            if (worldPt.x in minX..maxX && worldPt.y in minY..maxY) {
                                                foundId = e.id
                                                break
                                            }
                                        }
                                    } else if (e is SketchEntity.CircleEntity) {
                                        val c = sketch.points[e.centerId]
                                        if (c != null && hypot(worldPt.x - c.x, worldPt.y - c.y) <= e.radius + 2.0) {
                                            foundId = e.id
                                            break
                                        }
                                    }
                                }
                                selectedEntityId = foundId
                                if (foundId != null && activeTool == SketchTool.DIMENSION) {
                                    val ent = sketch.entities.find { it.id == foundId }
                                    if (ent is SketchEntity.CircleEntity) {
                                        showKeypadForEntity = Pair(ent.id, ent.radius * 2.0)
                                    } else if (ent is SketchEntity.RectangleEntity) {
                                        val p1 = sketch.points[ent.pMinId]
                                        val p2 = sketch.points[ent.pMaxId]
                                        if (p1 != null && p2 != null) {
                                            showKeypadForEntity = Pair(ent.id, abs(p2.x - p1.x))
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                .pointerInput(activeTool) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (activeTool == SketchTool.PAN) {
                                // Pan handled in drag
                            } else {
                                dragStartPoint = screenToWorld(offset)
                                currentDragPoint = dragStartPoint
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeTool == SketchTool.PAN) {
                                panOffset += dragAmount
                            } else {
                                currentDragPoint = screenToWorld(change.position)
                            }
                        },
                        onDragEnd = {
                            val start = dragStartPoint
                            val end = currentDragPoint
                            if (start != null && end != null && start.distanceTo(end) > 0.5) {
                                when (activeTool) {
                                    SketchTool.LINE -> {
                                        val line = sketch.addLine(start.x, start.y, end.x, end.y)
                                        sketch.constraints.add(SketchConstraint(type = ConstraintType.LENGTH_DIMENSION, entity1Id = line.id, targetValue = start.distanceTo(end)))
                                        SketchSolver.solve(sketch)
                                    }
                                    SketchTool.RECTANGLE -> {
                                        val w = abs(end.x - start.x)
                                        val h = abs(end.y - start.y)
                                        val minX = minOf(start.x, end.x)
                                        val minY = minOf(start.y, end.y)
                                        val rect = sketch.addRectangle(minX, minY, w, h)
                                        sketch.constraints.add(SketchConstraint(type = ConstraintType.LENGTH_DIMENSION, entity1Id = rect.id, targetValue = w))
                                        SketchSolver.solve(sketch)
                                    }
                                    SketchTool.CIRCLE -> {
                                        val r = start.distanceTo(end)
                                        val c = sketch.addCircle(start.x, start.y, r)
                                        sketch.constraints.add(SketchConstraint(type = ConstraintType.RADIUS_DIMENSION, entity1Id = c.id, targetValue = r))
                                        SketchSolver.solve(sketch)
                                    }
                                    else -> {}
                                }
                            }
                            dragStartPoint = null
                            currentDragPoint = null
                        }
                    )
                }
        ) {
            // Draw Cartesian Grid
            val gridSize = 10.0 // 10 mm
            val numLines = 40
            for (i in -numLines..numLines) {
                val isMajor = (i % 5 == 0)
                val color = if (isMajor) Color(0xFF334155) else Color(0xFF1E293B)
                val strokeW = if (isMajor) 1.5f else 0.8f

                // Vertical line
                val pTop = worldToScreen(Point2D(i * gridSize, -numLines * gridSize))
                val pBottom = worldToScreen(Point2D(i * gridSize, numLines * gridSize))
                drawLine(color, pTop, pBottom, strokeWidth = strokeW)

                // Horizontal line
                val pLeft = worldToScreen(Point2D(-numLines * gridSize, i * gridSize))
                val pRight = worldToScreen(Point2D(numLines * gridSize, i * gridSize))
                drawLine(color, pLeft, pRight, strokeWidth = strokeW)
            }

            // Draw Cartesian Axes (X-Red, Y-Green)
            val origin = worldToScreen(Point2D(0.0, 0.0))
            drawLine(CadAxisX, Offset(0f, origin.y), Offset(size.width, origin.y), strokeWidth = 2f)
            drawLine(CadAxisY, Offset(origin.x, 0f), Offset(origin.x, size.height), strokeWidth = 2f)
            drawCircle(CadCyan, radius = 5f, center = origin)

            // Draw Sketch Entities
            for (e in sketch.entities) {
                val isSelected = (e.id == selectedEntityId)
                val entityColor = if (isSelected) CadOrange else if (e.isConstruction) Color(0xFF64748B) else CadCyan

                when (e) {
                    is SketchEntity.LineEntity -> {
                        val p1 = sketch.points[e.p1Id]
                        val p2 = sketch.points[e.p2Id]
                        if (p1 != null && p2 != null) {
                            val sp1 = worldToScreen(Point2D(p1.x, p1.y))
                            val sp2 = worldToScreen(Point2D(p2.x, p2.y))
                            drawLine(entityColor, sp1, sp2, strokeWidth = if (isSelected) 4f else 2.5f)
                            drawCircle(Color.White, radius = 4f, center = sp1)
                            drawCircle(Color.White, radius = 4f, center = sp2)
                        }
                    }
                    is SketchEntity.RectangleEntity -> {
                        val p1 = sketch.points[e.pMinId]
                        val p2 = sketch.points[e.pMaxId]
                        if (p1 != null && p2 != null) {
                            val minX = minOf(p1.x, p2.x); val maxX = maxOf(p1.x, p2.x)
                            val minY = minOf(p1.y, p2.y); val maxY = maxOf(p1.y, p2.y)
                            val tl = worldToScreen(Point2D(minX, maxY))
                            val tr = worldToScreen(Point2D(maxX, maxY))
                            val br = worldToScreen(Point2D(maxX, minY))
                            val bl = worldToScreen(Point2D(minX, minY))
                            drawLine(entityColor, tl, tr, strokeWidth = if (isSelected) 4f else 2.5f)
                            drawLine(entityColor, tr, br, strokeWidth = if (isSelected) 4f else 2.5f)
                            drawLine(entityColor, br, bl, strokeWidth = if (isSelected) 4f else 2.5f)
                            drawLine(entityColor, bl, tl, strokeWidth = if (isSelected) 4f else 2.5f)
                        }
                    }
                    is SketchEntity.CircleEntity -> {
                        val c = sketch.points[e.centerId]
                        if (c != null) {
                            val sc = worldToScreen(Point2D(c.x, c.y))
                            val rPx = (e.radius * zoomScale).toFloat()
                            drawCircle(entityColor, radius = rPx, center = sc, style = Stroke(width = if (isSelected) 4f else 2.5f))
                            drawCircle(Color.White, radius = 3f, center = sc)
                        }
                    }
                    else -> {}
                }
            }

            // Draw Live Drag Preview
            val start = dragStartPoint
            val current = currentDragPoint
            if (start != null && current != null) {
                val s1 = worldToScreen(start)
                val s2 = worldToScreen(current)
                when (activeTool) {
                    SketchTool.LINE -> {
                        drawLine(CadAmber, s1, s2, strokeWidth = 2f)
                    }
                    SketchTool.RECTANGLE -> {
                        val minX = minOf(start.x, current.x); val maxX = maxOf(start.x, current.x)
                        val minY = minOf(start.y, current.y); val maxY = maxOf(start.y, current.y)
                        val tl = worldToScreen(Point2D(minX, maxY))
                        val tr = worldToScreen(Point2D(maxX, maxY))
                        val br = worldToScreen(Point2D(maxX, minY))
                        val bl = worldToScreen(Point2D(minX, minY))
                        drawLine(CadAmber, tl, tr, strokeWidth = 2f)
                        drawLine(CadAmber, tr, br, strokeWidth = 2f)
                        drawLine(CadAmber, br, bl, strokeWidth = 2f)
                        drawLine(CadAmber, bl, tl, strokeWidth = 2f)
                    }
                    SketchTool.CIRCLE -> {
                        val rPx = (start.distanceTo(current) * zoomScale).toFloat()
                        drawCircle(CadAmber, radius = rPx, center = s1, style = Stroke(width = 2f))
                    }
                    else -> {}
                }
            }
        }

        // 2. Top Header Overlay with Status and Finish Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Constraint Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isConstrained) Color(0x3300E676) else Color(0x33FFB300),
                border = BorderStroke(1.dp, if (isConstrained) CadGreen else CadAmber)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isConstrained) CadGreen else CadAmber, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConstrained) "Fully Constrained" else "Under-Constrained",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Finish and Cancel Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sketch_cancel_btn")
                ) {
                    Text(CadStrings.get("cancel_sketch", lang), color = Color.White, fontSize = 12.sp)
                }
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = CadGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sketch_finish_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = CadBackgroundDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(CadStrings.get("finish_sketch", lang), color = CadBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 3. Floating Bottom Tool Palette
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CadSurfaceDark,
            border = BorderStroke(1.dp, CadBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .testTag("sketch_tools_palette")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tools = listOf(
                    Pair(SketchTool.SELECT, Icons.Default.NearMe),
                    Pair(SketchTool.RECTANGLE, Icons.Default.CropSquare),
                    Pair(SketchTool.CIRCLE, Icons.Default.RadioButtonUnchecked),
                    Pair(SketchTool.LINE, Icons.Default.HorizontalRule),
                    Pair(SketchTool.DIMENSION, Icons.Default.Straighten),
                    Pair(SketchTool.PAN, Icons.Default.PanTool)
                )

                for ((tool, icon) in tools) {
                    val isSelected = (activeTool == tool)
                    IconButton(
                        onClick = { activeTool = tool },
                        modifier = Modifier
                            .size(44.dp)
                            .background(if (isSelected) CadCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .testTag("tool_${tool.name}")
                    ) {
                        Icon(
                            icon,
                            contentDescription = tool.name,
                            tint = if (isSelected) CadBackgroundDark else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Divider(modifier = Modifier.height(24.dp).width(1.dp), color = CadBorder)

                // Quick Zoom Controls
                IconButton(onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(30f) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }
                IconButton(onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.5f) }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }
            }
        }

        // Numeric Keypad Dialog for Editing Exact Dimension
        if (showKeypadForEntity != null) {
            val (entId, curVal) = showKeypadForEntity!!
            NumericKeypadDialog(
                title = CadStrings.get("dimension", lang),
                initialValue = curVal,
                unit = "mm",
                lang = lang,
                onConfirm = { newVal ->
                    val ent = sketch.entities.find { it.id == entId }
                    if (ent is SketchEntity.CircleEntity) {
                        ent.radius = newVal / 2.0
                        sketch.constraints.removeAll { it.entity1Id == entId && it.type == ConstraintType.RADIUS_DIMENSION }
                        sketch.constraints.add(SketchConstraint(type = ConstraintType.RADIUS_DIMENSION, entity1Id = entId, targetValue = ent.radius))
                    } else if (ent is SketchEntity.RectangleEntity) {
                        val p1 = sketch.points[ent.pMinId]
                        val p2 = sketch.points[ent.pMaxId]
                        if (p1 != null && p2 != null) {
                            val h = abs(p2.y - p1.y)
                            p2.x = p1.x + newVal
                            sketch.constraints.removeAll { it.entity1Id == entId && it.type == ConstraintType.LENGTH_DIMENSION }
                            sketch.constraints.add(SketchConstraint(type = ConstraintType.LENGTH_DIMENSION, entity1Id = entId, targetValue = newVal))
                        }
                    }
                    SketchSolver.solve(sketch)
                    showKeypadForEntity = null
                },
                onDismiss = { showKeypadForEntity = null }
            )
        }
    }
}
