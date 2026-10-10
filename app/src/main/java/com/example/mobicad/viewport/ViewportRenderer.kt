package com.example.mobicad.viewport

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.mobicad.model.*
import kotlin.math.*

enum class ShadingMode {
    SHADED_EDGES,
    SHADED,
    WIREFRAME
}

object ViewportRenderer {

    private val LIGHT_DIR = Vector3D(0.577, -0.577, 0.577).normalized()

    private class ProjectedTriangle(
        val p0: Offset,
        val p1: Offset,
        val p2: Offset,
        val depth: Double,
        val color: Color
    )

    fun render(
        drawScope: DrawScope,
        camera: Camera3D,
        mesh: Mesh3D?,
        assemblyParts: List<AssemblyPartInstance>,
        shadingMode: ShadingMode,
        selectedPartId: String? = null
    ) {
        val w = drawScope.size.width.toDouble()
        val h = drawScope.size.height.toDouble()

        // 1. Draw 3D Ground Grid (XY Plane) and 3D Datum Axes
        drawGrid(drawScope, camera, w, h)
        drawWorldAxes(drawScope, camera, w, h)

        // 2. Draw Main Solids
        if (mesh != null && mesh.triangles.isNotEmpty()) {
            renderMesh(drawScope, camera, mesh, shadingMode, w, h, isSelected = (selectedPartId == "main"))
        }

        // 3. Draw Assembly Parts
        for (part in assemblyParts) {
            val partMesh = mesh?.transform(part.getTransformMatrix())
            if (partMesh != null) {
                renderMesh(drawScope, camera, partMesh, shadingMode, w, h, isSelected = (selectedPartId == part.id))
            }
        }

        // 4. Draw Triad / Coordinate Gizmo (Bottom-Left)
        drawCoordinateGizmo(drawScope, camera)
    }

    private fun renderMesh(
        drawScope: DrawScope,
        camera: Camera3D,
        mesh: Mesh3D,
        shadingMode: ShadingMode,
        w: Double,
        h: Double,
        isSelected: Boolean
    ) {
        val viewMat = camera.getViewMatrix()
        val baseColor = if (isSelected) Color(0xFF00E5FF) else Color(mesh.colorHex)

        if (shadingMode == ShadingMode.WIREFRAME) {
            // Draw only edges
            for (edge in mesh.edges) {
                val p1 = camera.project(edge.p1, w, h) ?: continue
                val p2 = camera.project(edge.p2, w, h) ?: continue
                drawScope.drawLine(
                    color = if (isSelected) Color(0xFFFF6D00) else Color(0xFF90CAF9),
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = if (isSelected) 3f else 1.5f
                )
            }
            return
        }

        // Shaded mode: Project & depth-sort triangles (Painter's algorithm)
        val projectedTris = mutableListOf<ProjectedTriangle>()

        for (t in mesh.triangles) {
            // Backface culling in view space
            val viewNormal = viewMat.transformVector(t.normal)
            val v0View = viewMat.transformPoint(t.v0)
            val toCam = (-v0View).normalized()

            if (viewNormal.dot(toCam) < -0.05) continue

            val p0Proj = camera.project(t.v0, w, h) ?: continue
            val p1Proj = camera.project(t.v1, w, h) ?: continue
            val p2Proj = camera.project(t.v2, w, h) ?: continue

            // Lighting (Lambertian diffuse + ambient)
            val diffuse = max(0.0, t.normal.dot(LIGHT_DIR))
            val intensity = (0.38 + 0.62 * diffuse).toFloat().coerceIn(0.2f, 1.0f)

            val shadedColor = if (isSelected) {
                Color(
                    red = (0.0f * intensity + 0.15f).coerceIn(0f, 1f),
                    green = (0.85f * intensity + 0.15f).coerceIn(0f, 1f),
                    blue = (0.95f * intensity + 0.05f).coerceIn(0f, 1f),
                    alpha = 0.95f
                )
            } else {
                Color(
                    red = (baseColor.red * intensity).coerceIn(0f, 1f),
                    green = (baseColor.green * intensity).coerceIn(0f, 1f),
                    blue = (baseColor.blue * intensity).coerceIn(0f, 1f),
                    alpha = 1.0f
                )
            }

            val avgDepth = (p0Proj.third + p1Proj.third + p2Proj.third) / 3.0

            projectedTris.add(
                ProjectedTriangle(
                    p0 = Offset(p0Proj.first, p0Proj.second),
                    p1 = Offset(p1Proj.first, p1Proj.second),
                    p2 = Offset(p2Proj.first, p2Proj.second),
                    depth = avgDepth,
                    color = shadedColor
                )
            )
        }

        // Sort by depth (farthest first)
        projectedTris.sortBy { it.depth }

        val path = Path()
        for (pt in projectedTris) {
            path.reset()
            path.moveTo(pt.p0.x, pt.p0.y)
            path.lineTo(pt.p1.x, pt.p1.y)
            path.lineTo(pt.p2.x, pt.p2.y)
            path.close()
            drawScope.drawPath(path, color = pt.color)
        }

        // If SHADED_EDGES or isSelected, overlay crisp CAD feature boundary edges
        if (shadingMode == ShadingMode.SHADED_EDGES || isSelected) {
            val edgeColor = if (isSelected) Color(0xFFFF6D00) else Color(0xFF1E293B)
            val strokeW = if (isSelected) 3.0f else 1.5f
            for (edge in mesh.edges) {
                val p1 = camera.project(edge.p1, w, h) ?: continue
                val p2 = camera.project(edge.p2, w, h) ?: continue
                drawScope.drawLine(
                    color = edgeColor,
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = strokeW
                )
            }
        }

        // If isSelected, draw 3D bounding box cage outline with corner brackets
        if (isSelected) {
            val bb = mesh.boundingBox
            drawBoundingBoxCage(drawScope, camera, bb, w, h)
        }
    }

    private fun drawBoundingBoxCage(drawScope: DrawScope, camera: Camera3D, bb: BoundingBox, w: Double, h: Double) {
        val cageColor = Color(0xFFFFB300)
        val corners = listOf(
            Vector3D(bb.minX, bb.minY, bb.minZ), Vector3D(bb.maxX, bb.minY, bb.minZ),
            Vector3D(bb.maxX, bb.maxY, bb.minZ), Vector3D(bb.minX, bb.maxY, bb.minZ),
            Vector3D(bb.minX, bb.minY, bb.maxZ), Vector3D(bb.maxX, bb.minY, bb.maxZ),
            Vector3D(bb.maxX, bb.maxY, bb.maxZ), Vector3D(bb.minX, bb.maxY, bb.maxZ)
        )
        val edges = listOf(
            Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
            Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4),
            Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7)
        )
        for ((i1, i2) in edges) {
            val p1 = camera.project(corners[i1], w, h) ?: continue
            val p2 = camera.project(corners[i2], w, h) ?: continue
            drawScope.drawLine(
                color = cageColor,
                start = Offset(p1.first, p1.second),
                end = Offset(p2.first, p2.second),
                strokeWidth = 1.2f
            )
        }
    }

    private fun drawGrid(drawScope: DrawScope, camera: Camera3D, w: Double, h: Double) {
        val gridSize = 100.0 // +/- 100mm
        val step = 10.0      // 10mm grid interval

        var current = -gridSize
        while (current <= gridSize + 0.1) {
            val isMajor = abs(current % 50.0) < 0.1
            val lineColor = if (isMajor) Color(0xFF334155) else Color(0xFF1E293B)
            val strokeW = if (isMajor) 1.5f else 0.8f

            val p1x = camera.project(Vector3D(-gridSize, current, 0.0), w, h)
            val p2x = camera.project(Vector3D(gridSize, current, 0.0), w, h)
            if (p1x != null && p2x != null) {
                drawScope.drawLine(lineColor, Offset(p1x.first, p1x.second), Offset(p2x.first, p2x.second), strokeWidth = strokeW)
            }

            val p1y = camera.project(Vector3D(current, -gridSize, 0.0), w, h)
            val p2y = camera.project(Vector3D(current, gridSize, 0.0), w, h)
            if (p1y != null && p2y != null) {
                drawScope.drawLine(lineColor, Offset(p1y.first, p1y.second), Offset(p2y.first, p2y.second), strokeWidth = strokeW)
            }

            current += step
        }
    }

    private fun drawWorldAxes(drawScope: DrawScope, camera: Camera3D, w: Double, h: Double) {
        val o = camera.project(Vector3D.ZERO, w, h) ?: return
        val oPt = Offset(o.first, o.second)
        val axisLen = 45.0

        // X Axis (Red)
        val px = camera.project(Vector3D(axisLen, 0.0, 0.0), w, h)
        if (px != null) {
            drawScope.drawLine(Color(0xFFEF4444), oPt, Offset(px.first, px.second), strokeWidth = 2.5f)
        }
        // Y Axis (Green)
        val py = camera.project(Vector3D(0.0, axisLen, 0.0), w, h)
        if (py != null) {
            drawScope.drawLine(Color(0xFF22C55E), oPt, Offset(py.first, py.second), strokeWidth = 2.5f)
        }
        // Z Axis (Blue)
        val pz = camera.project(Vector3D(0.0, 0.0, axisLen), w, h)
        if (pz != null) {
            drawScope.drawLine(Color(0xFF3B82F6), oPt, Offset(pz.first, pz.second), strokeWidth = 2.5f)
        }
    }

    private fun drawCoordinateGizmo(drawScope: DrawScope, camera: Camera3D) {
        val originX = 64f
        val originY = drawScope.size.height - 64f
        val gizmoLen = 36.0

        val rotMat = camera.getViewMatrix()
        val xDir = rotMat.transformVector(Vector3D.UNIT_X) * gizmoLen
        val yDir = rotMat.transformVector(Vector3D.UNIT_Y) * gizmoLen
        val zDir = rotMat.transformVector(Vector3D.UNIT_Z) * gizmoLen

        fun toOffset(v: Vector3D) = Offset((originX + v.x).toFloat(), (originY - v.y).toFloat())

        val o = Offset(originX, originY)
        // Background disk
        drawScope.drawCircle(Color(0xD0111827), radius = 46f, center = o)

        // Axis Lines & Nodes
        val xPt = toOffset(xDir)
        val yPt = toOffset(yDir)
        val zPt = toOffset(zDir)

        // X Axis (Red)
        drawScope.drawLine(Color(0xFFEF4444), o, xPt, strokeWidth = 3f)
        drawScope.drawCircle(Color(0xFFEF4444), radius = 5f, center = xPt)

        // Y Axis (Green)
        drawScope.drawLine(Color(0xFF22C55E), o, yPt, strokeWidth = 3f)
        drawScope.drawCircle(Color(0xFF22C55E), radius = 5f, center = yPt)

        // Z Axis (Blue)
        drawScope.drawLine(Color(0xFF3B82F6), o, zPt, strokeWidth = 3f)
        drawScope.drawCircle(Color(0xFF3B82F6), radius = 5f, center = zPt)
    }

    /**
     * Hit-tests screen coordinates against the mesh triangles to pick object.
     */
    fun hitTest(screenX: Float, screenY: Float, camera: Camera3D, mesh: Mesh3D?, w: Double, h: Double): Boolean {
        if (mesh == null || mesh.triangles.isEmpty()) return false
        val tap = Offset(screenX, screenY)

        for (t in mesh.triangles) {
            val p0 = camera.project(t.v0, w, h) ?: continue
            val p1 = camera.project(t.v1, w, h) ?: continue
            val p2 = camera.project(t.v2, w, h) ?: continue

            val a = Offset(p0.first, p0.second)
            val b = Offset(p1.first, p1.second)
            val c = Offset(p2.first, p2.second)

            if (isPointInTriangle(tap, a, b, c)) {
                return true
            }
        }
        return false
    }

    private fun isPointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
        val d1 = (p.x - b.x) * (a.y - b.y) - (a.x - b.x) * (p.y - b.y)
        val d2 = (p.x - c.x) * (b.y - c.y) - (b.x - c.x) * (p.y - c.y)
        val d3 = (p.x - a.x) * (c.y - a.y) - (c.x - a.x) * (p.y - a.y)
        val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
        val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
        return !(hasNeg && hasPos)
    }
}
