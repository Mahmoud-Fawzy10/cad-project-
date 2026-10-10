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

        // 1. Draw 3D Ground Grid (XY Plane)
        drawGrid(drawScope, camera, w, h)

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
        val projMat = camera.getProjectionMatrix(w, h)
        val eyePos = camera.computeEyePosition()

        val baseColor = if (isSelected) Color(0xFF00E5FF) else Color(mesh.colorHex)

        if (shadingMode == ShadingMode.WIREFRAME) {
            // Draw only edges
            for (edge in mesh.edges) {
                val p1 = camera.project(edge.p1, w, h) ?: continue
                val p2 = camera.project(edge.p2, w, h) ?: continue
                drawScope.drawLine(
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90CAF9),
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = 2f
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

            // If normal points away from camera, cull
            if (viewNormal.dot(toCam) < -0.05) continue

            val p0Proj = camera.project(t.v0, w, h) ?: continue
            val p1Proj = camera.project(t.v1, w, h) ?: continue
            val p2Proj = camera.project(t.v2, w, h) ?: continue

            // Lighting (Lambertian diffuse + ambient)
            val diffuse = max(0.0, t.normal.dot(LIGHT_DIR))
            val intensity = (0.35 + 0.65 * diffuse).toFloat().coerceIn(0.2f, 1.0f)

            val shadedColor = Color(
                red = (baseColor.red * intensity).coerceIn(0f, 1f),
                green = (baseColor.green * intensity).coerceIn(0f, 1f),
                blue = (baseColor.blue * intensity).coerceIn(0f, 1f),
                alpha = 1.0f
            )

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

        // If SHADED_EDGES, overlay crisp CAD feature boundary edges
        if (shadingMode == ShadingMode.SHADED_EDGES) {
            val edgeColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
            for (edge in mesh.edges) {
                val p1 = camera.project(edge.p1, w, h) ?: continue
                val p2 = camera.project(edge.p2, w, h) ?: continue
                drawScope.drawLine(
                    color = edgeColor,
                    start = Offset(p1.first, p1.second),
                    end = Offset(p2.first, p2.second),
                    strokeWidth = if (isSelected) 2.5f else 1.5f
                )
            }
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

            // Line parallel to X
            val p1x = camera.project(Vector3D(-gridSize, current, 0.0), w, h)
            val p2x = camera.project(Vector3D(gridSize, current, 0.0), w, h)
            if (p1x != null && p2x != null) {
                drawScope.drawLine(lineColor, Offset(p1x.first, p1x.second), Offset(p2x.first, p2x.second), strokeWidth = strokeW)
            }

            // Line parallel to Y
            val p1y = camera.project(Vector3D(current, -gridSize, 0.0), w, h)
            val p2y = camera.project(Vector3D(current, gridSize, 0.0), w, h)
            if (p1y != null && p2y != null) {
                drawScope.drawLine(lineColor, Offset(p1y.first, p1y.second), Offset(p2y.first, p2y.second), strokeWidth = strokeW)
            }

            current += step
        }
    }

    private fun drawCoordinateGizmo(drawScope: DrawScope, camera: Camera3D) {
        val originX = 70f
        val originY = drawScope.size.height - 70f
        val gizmoLen = 40.0

        val rotMat = camera.getViewMatrix()
        // Extract 3x3 rotation
        val xDir = rotMat.transformVector(Vector3D.UNIT_X) * gizmoLen
        val yDir = rotMat.transformVector(Vector3D.UNIT_Y) * gizmoLen
        val zDir = rotMat.transformVector(Vector3D.UNIT_Z) * gizmoLen

        // Screen Y is down
        fun toOffset(v: Vector3D) = Offset((originX + v.x).toFloat(), (originY - v.y).toFloat())

        val o = Offset(originX, originY)
        // Draw background badge
        drawScope.drawCircle(Color(0x800F172A), radius = 48f, center = o)

        // X Axis (Red)
        drawScope.drawLine(Color(0xFFEF4444), o, toOffset(xDir), strokeWidth = 3f)
        // Y Axis (Green)
        drawScope.drawLine(Color(0xFF22C55E), o, toOffset(yDir), strokeWidth = 3f)
        // Z Axis (Blue)
        drawScope.drawLine(Color(0xFF3B82F6), o, toOffset(zDir), strokeWidth = 3f)
    }
}
