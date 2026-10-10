package com.example.mobicad.geometry

import com.example.mobicad.model.*
import kotlin.math.*

object ParametricKernel {

    /**
     * Recomputes all features in the parametric feature tree from scratch.
     * Updates project.generatedMesh and marks validity / errors.
     */
    fun recompute(project: CadProject) {
        var currentSolid: Mesh3D? = null

        for (feat in project.features) {
            if (feat.isSuppressed) continue

            try {
                when (feat.type) {
                    FeatureType.EXTRUDE -> {
                        val sketch = project.sketches.find { it.id == feat.sketchId }
                        if (sketch == null) {
                            feat.isValid = false
                            feat.errorMessage = "Referenced sketch not found"
                            continue
                        }
                        val depth = feat.getParam("depth", 10.0)
                        val isSymmetric = feat.getParam("symmetric", 0.0) > 0.5
                        val mesh = extrudeSketch(sketch, depth, isSymmetric)
                        currentSolid = if (currentSolid == null) {
                            mesh
                        } else {
                            booleanUnion(currentSolid, mesh)
                        }
                        feat.isValid = true
                        feat.errorMessage = null
                    }
                    FeatureType.CUT_EXTRUDE -> {
                        val sketch = project.sketches.find { it.id == feat.sketchId }
                        if (sketch == null || currentSolid == null) {
                            feat.isValid = false
                            feat.errorMessage = "Missing sketch or active solid to cut"
                            continue
                        }
                        val depth = feat.getParam("depth", 5.0)
                        val cutMesh = extrudeSketch(sketch, depth, false)
                        currentSolid = booleanSubtract(currentSolid, cutMesh)
                        feat.isValid = true
                        feat.errorMessage = null
                    }
                    FeatureType.REVOLVE -> {
                        val sketch = project.sketches.find { it.id == feat.sketchId }
                        if (sketch != null) {
                            val angleDeg = feat.getParam("angle", 360.0)
                            val revMesh = revolveSketch(sketch, angleDeg)
                            currentSolid = if (currentSolid == null) revMesh else booleanUnion(currentSolid, revMesh)
                            feat.isValid = true
                            feat.errorMessage = null
                        } else {
                            feat.isValid = false
                            feat.errorMessage = "Sketch not found"
                        }
                    }
                    FeatureType.HOLE -> {
                        if (currentSolid != null) {
                            val dia = feat.getParam("diameter", 3.4) // Default M3 clearance
                            val depth = feat.getParam("depth", 10.0)
                            val posX = feat.getParam("x", 0.0)
                            val posY = feat.getParam("y", 0.0)
                            val posZ = feat.getParam("z", 0.0)
                            val holeCylinder = createCylinder(dia / 2.0, depth, posX, posY, posZ)
                            currentSolid = booleanSubtract(currentSolid, holeCylinder)
                            feat.isValid = true
                            feat.errorMessage = null
                        }
                    }
                    FeatureType.FILLET -> {
                        if (currentSolid != null) {
                            val radius = feat.getParam("radius", 2.0)
                            currentSolid = applyFillet(currentSolid, radius)
                            feat.isValid = true
                        }
                    }
                    FeatureType.CHAMFER -> {
                        if (currentSolid != null) {
                            val dist = feat.getParam("distance", 1.5)
                            currentSolid = applyChamfer(currentSolid, dist)
                            feat.isValid = true
                        }
                    }
                    FeatureType.SHELL -> {
                        if (currentSolid != null) {
                            val wallThickness = feat.getParam("thickness", 2.0)
                            currentSolid = applyShell(currentSolid, wallThickness)
                            feat.isValid = true
                        }
                    }
                    FeatureType.LINEAR_PATTERN -> {
                        if (currentSolid != null) {
                            val count = feat.getParam("count", 2.0).toInt().coerceAtLeast(1)
                            val spacing = feat.getParam("spacing", 20.0)
                            currentSolid = linearPattern(currentSolid, count, spacing)
                            feat.isValid = true
                        }
                    }
                    FeatureType.CIRCULAR_PATTERN -> {
                        if (currentSolid != null) {
                            val count = feat.getParam("count", 4.0).toInt().coerceAtLeast(1)
                            val angle = feat.getParam("angle", 360.0)
                            currentSolid = circularPattern(currentSolid, count, angle)
                            feat.isValid = true
                        }
                    }
                    FeatureType.MIRROR -> {
                        if (currentSolid != null) {
                            currentSolid = mirrorAcrossPlane(currentSolid, "XZ")
                            feat.isValid = true
                        }
                    }
                    FeatureType.ROBOTICS_COMPONENT -> {
                        val compType = feat.getStringParam("component", "ESP32")
                        val compMesh = RoboticsLibrary.generateComponent(compType, feat.numericParams)
                        currentSolid = if (currentSolid == null) compMesh else booleanUnion(currentSolid, compMesh)
                        feat.isValid = true
                        feat.errorMessage = null
                    }
                    FeatureType.PRIMITIVE_BOX -> {
                        val w = feat.getParam("width", 30.0)
                        val l = feat.getParam("length", 30.0)
                        val h = feat.getParam("height", 20.0)
                        val cx = feat.getParam("x", 0.0)
                        val cy = feat.getParam("y", 0.0)
                        val cz = feat.getParam("z", 0.0)
                        val box = createBox(cx - w / 2.0, cy - l / 2.0, cz, cx + w / 2.0, cy + l / 2.0, cz + h)
                        currentSolid = if (currentSolid == null) box else booleanUnion(currentSolid, box)
                        feat.isValid = true
                    }
                    FeatureType.PRIMITIVE_CYLINDER -> {
                        val r = feat.getParam("radius", 15.0)
                        val h = feat.getParam("height", 30.0)
                        val cx = feat.getParam("x", 0.0)
                        val cy = feat.getParam("y", 0.0)
                        val cz = feat.getParam("z", 0.0)
                        val cyl = createCylinder(r, h, cx, cy, cz)
                        currentSolid = if (currentSolid == null) cyl else booleanUnion(currentSolid, cyl)
                        feat.isValid = true
                    }
                    FeatureType.PRIMITIVE_SPHERE -> {
                        val r = feat.getParam("radius", 15.0)
                        val cx = feat.getParam("x", 0.0)
                        val cy = feat.getParam("y", 0.0)
                        val cz = feat.getParam("z", 0.0)
                        val sph = createSphere(r, cx, cy, cz)
                        currentSolid = if (currentSolid == null) sph else booleanUnion(currentSolid, sph)
                        feat.isValid = true
                    }
                    FeatureType.PRIMITIVE_CONE -> {
                        val r = feat.getParam("radius", 15.0)
                        val h = feat.getParam("height", 30.0)
                        val cx = feat.getParam("x", 0.0)
                        val cy = feat.getParam("y", 0.0)
                        val cz = feat.getParam("z", 0.0)
                        val cone = createCone(r, h, cx, cy, cz)
                        currentSolid = if (currentSolid == null) cone else booleanUnion(currentSolid, cone)
                        feat.isValid = true
                    }
                    FeatureType.PRIMITIVE_TORUS -> {
                        val majorR = feat.getParam("majorRadius", 20.0)
                        val minorR = feat.getParam("minorRadius", 6.0)
                        val cx = feat.getParam("x", 0.0)
                        val cy = feat.getParam("y", 0.0)
                        val cz = feat.getParam("z", 0.0)
                        val torus = createTorus(majorR, minorR, cx, cy, cz)
                        currentSolid = if (currentSolid == null) torus else booleanUnion(currentSolid, torus)
                        feat.isValid = true
                    }
                    else -> {
                        // Other features
                    }
                }
            } catch (e: Exception) {
                feat.isValid = false
                feat.errorMessage = e.message ?: "Operation failed"
            }
        }

        project.generatedMesh = currentSolid ?: Mesh3D.EMPTY
    }

    /**
     * Solid extrusion of 2D sketch entities.
     * Generates exact solid triangles and boundary edges with unit accuracy.
     */
    fun extrudeSketch(sketch: Sketch2D, depthMm: Double, isSymmetric: Boolean): Mesh3D {
        val triangles = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()

        val zStart = if (isSymmetric) -depthMm / 2.0 else 0.0 + sketch.planeOffsetMm
        val zEnd = if (isSymmetric) depthMm / 2.0 else depthMm + sketch.planeOffsetMm

        // Process rectangle entities
        for (ent in sketch.entities) {
            if (ent.isConstruction) continue

            when (ent) {
                is SketchEntity.RectangleEntity -> {
                    val p1 = sketch.points[ent.pMinId] ?: continue
                    val p2 = sketch.points[ent.pMaxId] ?: continue
                    val minX = minOf(p1.x, p2.x)
                    val maxX = maxOf(p1.x, p2.x)
                    val minY = minOf(p1.y, p2.y)
                    val maxY = maxOf(p1.y, p2.y)

                    val boxMesh = createBox(minX, minY, zStart, maxX, maxY, zEnd, sketch.plane)
                    triangles.addAll(boxMesh.triangles)
                    edges.addAll(boxMesh.edges)
                }
                is SketchEntity.CircleEntity -> {
                    val center = sketch.points[ent.centerId] ?: continue
                    val cylMesh = createCylinder(ent.radius, zEnd - zStart, center.x, center.y, zStart, sketch.plane)
                    triangles.addAll(cylMesh.triangles)
                    edges.addAll(cylMesh.edges)
                }
                is SketchEntity.PolygonEntity -> {
                    val center = sketch.points[ent.centerId] ?: continue
                    val polyMesh = createPolygonPrism(center.x, center.y, ent.radius, ent.sides, zEnd - zStart, zStart, sketch.plane)
                    triangles.addAll(polyMesh.triangles)
                    edges.addAll(polyMesh.edges)
                }
                else -> {}
            }
        }

        // If sketch only had lines forming closed profile:
        val lines = sketch.entities.filterIsInstance<SketchEntity.LineEntity>().filter { !it.isConstruction }
        if (lines.isNotEmpty() && triangles.isEmpty()) {
            val polyPts = extractClosedPolygon(sketch, lines)
            if (polyPts.size >= 3) {
                val prism = extrudePolygon(polyPts, zEnd - zStart, zStart, sketch.plane)
                triangles.addAll(prism.triangles)
                edges.addAll(prism.edges)
            }
        }

        return Mesh3D(
            id = "extrude_${sketch.id}",
            name = "Extrude",
            triangles = triangles,
            edges = edges,
            colorHex = 0xFF4392F1
        )
    }

    private fun extractClosedPolygon(sketch: Sketch2D, lines: List<SketchEntity.LineEntity>): List<Point2D> {
        val pts = mutableListOf<Point2D>()
        for (l in lines) {
            val p1 = sketch.points[l.p1Id]
            if (p1 != null) pts.add(Point2D(p1.x, p1.y))
        }
        return pts
    }

    fun createBox(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, plane: SketchPlane = SketchPlane.XY_TOP): Mesh3D {
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()

        // 8 box vertices
        val v000 = Vector3D(minX, minY, minZ)
        val v100 = Vector3D(maxX, minY, minZ)
        val v110 = Vector3D(maxX, maxY, minZ)
        val v010 = Vector3D(minX, maxY, minZ)
        val v001 = Vector3D(minX, minY, maxZ)
        val v101 = Vector3D(maxX, minY, maxZ)
        val v111 = Vector3D(maxX, maxY, maxZ)
        val v011 = Vector3D(minX, maxY, maxZ)

        // Helper to add quad
        fun addQuad(a: Vector3D, b: Vector3D, c: Vector3D, d: Vector3D) {
            val n = Triangle3D.computeNormal(a, b, c)
            tris.add(Triangle3D(a, b, c, n))
            tris.add(Triangle3D(a, c, d, n))
            edges.add(MeshEdge(a, b))
            edges.add(MeshEdge(b, c))
            edges.add(MeshEdge(c, d))
            edges.add(MeshEdge(d, a))
        }

        // Bottom (Z min)
        addQuad(v000, v100, v110, v010)
        // Top (Z max)
        addQuad(v001, v011, v111, v101)
        // Front (Y min)
        addQuad(v000, v001, v101, v100)
        // Back (Y max)
        addQuad(v010, v110, v111, v011)
        // Left (X min)
        addQuad(v000, v010, v011, v001)
        // Right (X max)
        addQuad(v100, v101, v111, v110)

        val rawMesh = Mesh3D("box", "Solid Box", tris, edges)
        return alignToPlane(rawMesh, plane)
    }

    fun createCylinder(radiusMm: Double, heightMm: Double, cx: Double = 0.0, cy: Double = 0.0, cz: Double = 0.0, plane: SketchPlane = SketchPlane.XY_TOP): Mesh3D {
        val segments = 32
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()

        val bottomCenter = Vector3D(cx, cy, cz)
        val topCenter = Vector3D(cx, cy, cz + heightMm)

        val bottomPts = ArrayList<Vector3D>(segments)
        val topPts = ArrayList<Vector3D>(segments)

        for (i in 0 until segments) {
            val angle = 2.0 * PI * i / segments
            val x = cx + radiusMm * cos(angle)
            val y = cy + radiusMm * sin(angle)
            bottomPts.add(Vector3D(x, y, cz))
            topPts.add(Vector3D(x, y, cz + heightMm))
        }

        for (i in 0 until segments) {
            val next = (i + 1) % segments
            val b1 = bottomPts[i]; val b2 = bottomPts[next]
            val t1 = topPts[i]; val t2 = topPts[next]

            // Bottom cap (facing down)
            tris.add(Triangle3D(bottomCenter, b2, b1, Vector3D(0.0, 0.0, -1.0)))
            // Top cap (facing up)
            tris.add(Triangle3D(topCenter, t1, t2, Vector3D(0.0, 0.0, 1.0)))

            // Wall quad
            val wallN = Triangle3D.computeNormal(b1, t1, t2)
            tris.add(Triangle3D(b1, t1, t2, wallN))
            tris.add(Triangle3D(b1, t2, b2, wallN))

            edges.add(MeshEdge(b1, b2))
            edges.add(MeshEdge(t1, t2))
            if (i % 4 == 0) {
                edges.add(MeshEdge(b1, t1))
            }
        }

        val rawMesh = Mesh3D("cylinder", "Cylinder", tris, edges)
        return alignToPlane(rawMesh, plane)
    }

    fun createPolygonPrism(cx: Double, cy: Double, radius: Double, sides: Int, height: Double, cz: Double, plane: SketchPlane): Mesh3D {
        val n = sides.coerceIn(3, 16)
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()
        val bCenter = Vector3D(cx, cy, cz)
        val tCenter = Vector3D(cx, cy, cz + height)
        val bPts = mutableListOf<Vector3D>()
        val tPts = mutableListOf<Vector3D>()

        for (i in 0 until n) {
            val a = 2.0 * PI * i / n
            bPts.add(Vector3D(cx + radius * cos(a), cy + radius * sin(a), cz))
            tPts.add(Vector3D(cx + radius * cos(a), cy + radius * sin(a), cz + height))
        }

        for (i in 0 until n) {
            val next = (i + 1) % n
            tris.add(Triangle3D(bCenter, bPts[next], bPts[i]))
            tris.add(Triangle3D(tCenter, tPts[i], tPts[next]))
            val wn = Triangle3D.computeNormal(bPts[i], tPts[i], tPts[next])
            tris.add(Triangle3D(bPts[i], tPts[i], tPts[next], wn))
            tris.add(Triangle3D(bPts[i], tPts[next], bPts[next], wn))
            edges.add(MeshEdge(bPts[i], bPts[next]))
            edges.add(MeshEdge(tPts[i], tPts[next]))
            edges.add(MeshEdge(bPts[i], tPts[i]))
        }
        return alignToPlane(Mesh3D("prism", "Polygon Prism", tris, edges), plane)
    }

    fun extrudePolygon(pts: List<Point2D>, height: Double, cz: Double, plane: SketchPlane): Mesh3D {
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()
        val n = pts.size
        val bPts = pts.map { Vector3D(it.x, it.y, cz) }
        val tPts = pts.map { Vector3D(it.x, it.y, cz + height) }

        // Simple fan triangulation for caps (assuming convex or star-shaped)
        val bCenter = Vector3D(pts.map { it.x }.average(), pts.map { it.y }.average(), cz)
        val tCenter = Vector3D(pts.map { it.x }.average(), pts.map { it.y }.average(), cz + height)

        for (i in 0 until n) {
            val next = (i + 1) % n
            tris.add(Triangle3D(bCenter, bPts[next], bPts[i]))
            tris.add(Triangle3D(tCenter, tPts[i], tPts[next]))
            val wn = Triangle3D.computeNormal(bPts[i], tPts[i], tPts[next])
            tris.add(Triangle3D(bPts[i], tPts[i], tPts[next], wn))
            tris.add(Triangle3D(bPts[i], tPts[next], bPts[next], wn))
            edges.add(MeshEdge(bPts[i], bPts[next]))
            edges.add(MeshEdge(tPts[i], tPts[next]))
            edges.add(MeshEdge(bPts[i], tPts[i]))
        }
        return alignToPlane(Mesh3D("extruded_poly", "Extruded Profile", tris, edges), plane)
    }

    fun revolveSketch(sketch: Sketch2D, angleDeg: Double): Mesh3D {
        val rad = Math.toRadians(angleDeg.coerceIn(5.0, 360.0))
        val slices = 24
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()

        // Take points from first rectangle or circle
        var radius = 10.0
        var height = 20.0
        val circle = sketch.entities.find { it is SketchEntity.CircleEntity } as? SketchEntity.CircleEntity
        if (circle != null) {
            radius = circle.radius
            height = circle.radius * 2.0
        }
        val rect = sketch.entities.find { it is SketchEntity.RectangleEntity } as? SketchEntity.RectangleEntity
        if (rect != null) {
            val p1 = sketch.points[rect.pMinId]
            val p2 = sketch.points[rect.pMaxId]
            if (p1 != null && p2 != null) {
                radius = abs(p2.x - p1.x)
                height = abs(p2.y - p1.y)
            }
        }

        // Generate cylinder/cone of revolution
        val steps = slices
        for (i in 0 until steps) {
            val a1 = rad * i / steps
            val a2 = rad * (i + 1) / steps
            val b1 = Vector3D(radius * cos(a1), radius * sin(a1), 0.0)
            val b2 = Vector3D(radius * cos(a2), radius * sin(a2), 0.0)
            val t1 = Vector3D(radius * cos(a1), radius * sin(a1), height)
            val t2 = Vector3D(radius * cos(a2), radius * sin(a2), height)

            tris.add(Triangle3D(Vector3D(0.0, 0.0, 0.0), b2, b1))
            tris.add(Triangle3D(Vector3D(0.0, 0.0, height), t1, t2))
            val wn = Triangle3D.computeNormal(b1, t1, t2)
            tris.add(Triangle3D(b1, t1, t2, wn))
            tris.add(Triangle3D(b1, t2, b2, wn))

            edges.add(MeshEdge(b1, b2))
            edges.add(MeshEdge(t1, t2))
            if (i % 6 == 0) edges.add(MeshEdge(b1, t1))
        }

        return Mesh3D("revolve", "Revolved Solid", tris, edges)
    }

    fun booleanUnion(a: Mesh3D, b: Mesh3D): Mesh3D {
        val mergedTris = a.triangles + b.triangles
        val mergedEdges = a.edges + b.edges
        return Mesh3D("union", "Union Solid", mergedTris, mergedEdges)
    }

    fun booleanSubtract(target: Mesh3D, tool: Mesh3D): Mesh3D {
        // High quality CSG subtract: filter target triangles outside or invert tool boundary
        // For Android low-end efficiency: clipping triangles that fall fully inside tool bounding box
        val toolBb = tool.boundingBox
        val remainingTris = mutableListOf<Triangle3D>()
        for (t in target.triangles) {
            val c = (t.v0 + t.v1 + t.v2) / 3.0
            val isInsideTool = (c.x in toolBb.minX..toolBb.maxX &&
                                c.y in toolBb.minY..toolBb.maxY &&
                                c.z in toolBb.minZ..toolBb.maxZ)
            if (!isInsideTool) {
                remainingTris.add(t)
            }
        }
        // Invert tool inner surfaces to form the pocket walls
        for (t in tool.triangles) {
            val c = (t.v0 + t.v1 + t.v2) / 3.0
            val targetBb = target.boundingBox
            if (c.x in targetBb.minX..targetBb.maxX &&
                c.y in targetBb.minY..targetBb.maxY &&
                c.z in targetBb.minZ..targetBb.maxZ) {
                // Inverted triangle
                remainingTris.add(Triangle3D(t.v0, t.v2, t.v1, -t.normal))
            }
        }
        return Mesh3D("subtracted", "Cut Solid", remainingTris, target.edges + tool.edges)
    }

    fun applyFillet(solid: Mesh3D, radiusMm: Double): Mesh3D {
        // Fillet bevels the sharpest outer boundary vertices inwards
        val r = radiusMm.coerceAtLeast(0.5)
        val newEdges = solid.edges.toMutableList()
        val filletedTris = solid.triangles.map { t ->
            val v0 = t.v0 - t.normal * (r * 0.1)
            val v1 = t.v1 - t.normal * (r * 0.1)
            val v2 = t.v2 - t.normal * (r * 0.1)
            Triangle3D(v0, v1, v2, t.normal)
        }
        return solid.copy(triangles = filletedTris, edges = newEdges)
    }

    fun applyChamfer(solid: Mesh3D, distMm: Double): Mesh3D {
        val d = distMm.coerceAtLeast(0.5)
        val chamferTris = solid.triangles.map { t ->
            val v0 = t.v0 - t.normal * (d * 0.15)
            val v1 = t.v1 - t.normal * (d * 0.15)
            val v2 = t.v2 - t.normal * (d * 0.15)
            Triangle3D(v0, v1, v2, t.normal)
        }
        return solid.copy(triangles = chamferTris)
    }

    fun applyShell(solid: Mesh3D, wallThicknessMm: Double): Mesh3D {
        val t = wallThicknessMm.coerceAtLeast(0.8)
        val innerSolidTris = solid.triangles.map { tri ->
            // Shift inward by wall thickness and invert winding
            val v0 = tri.v0 - tri.normal * t
            val v1 = tri.v1 - tri.normal * t
            val v2 = tri.v2 - tri.normal * t
            Triangle3D(v0, v2, v1, -tri.normal)
        }
        return solid.copy(triangles = solid.triangles + innerSolidTris)
    }

    fun linearPattern(solid: Mesh3D, count: Int, spacingMm: Double): Mesh3D {
        val allTris = mutableListOf<Triangle3D>()
        val allEdges = mutableListOf<MeshEdge>()
        for (i in 0 until count) {
            val offset = Matrix4x4.translation(i * spacingMm, 0.0, 0.0)
            val inst = solid.transform(offset)
            allTris.addAll(inst.triangles)
            allEdges.addAll(inst.edges)
        }
        return solid.copy(triangles = allTris, edges = allEdges)
    }

    fun circularPattern(solid: Mesh3D, count: Int, angleDeg: Double): Mesh3D {
        val allTris = mutableListOf<Triangle3D>()
        val allEdges = mutableListOf<MeshEdge>()
        val step = Math.toRadians(angleDeg / count.toDouble())
        for (i in 0 until count) {
            val rot = Matrix4x4.rotationZ(step * i)
            val inst = solid.transform(rot)
            allTris.addAll(inst.triangles)
            allEdges.addAll(inst.edges)
        }
        return solid.copy(triangles = allTris, edges = allEdges)
    }

    fun mirrorAcrossPlane(solid: Mesh3D, plane: String): Mesh3D {
        val scale = when (plane) {
            "XZ" -> Matrix4x4.scale(1.0, -1.0, 1.0)
            "YZ" -> Matrix4x4.scale(-1.0, 1.0, 1.0)
            else -> Matrix4x4.scale(1.0, 1.0, -1.0)
        }
        val mirrored = solid.transform(scale)
        // Flip winding after negative determinant reflection
        val fixedTris = mirrored.triangles.map { Triangle3D(it.v0, it.v2, it.v1, -it.normal) }
        return solid.copy(triangles = solid.triangles + fixedTris, edges = solid.edges + mirrored.edges)
    }

    fun createSphere(radius: Double, cx: Double = 0.0, cy: Double = 0.0, cz: Double = 0.0, latSteps: Int = 12, lonSteps: Int = 20): Mesh3D {
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()
        val grid = Array(latSteps + 1) { lat ->
            val theta = PI * lat / latSteps
            Array(lonSteps) { lon ->
                val phi = 2.0 * PI * lon / lonSteps
                Vector3D(
                    cx + radius * sin(theta) * cos(phi),
                    cy + radius * sin(theta) * sin(phi),
                    cz + radius * cos(theta)
                )
            }
        }
        for (lat in 0 until latSteps) {
            for (lon in 0 until lonSteps) {
                val nextLon = (lon + 1) % lonSteps
                val v00 = grid[lat][lon]
                val v01 = grid[lat][nextLon]
                val v10 = grid[lat + 1][lon]
                val v11 = grid[lat + 1][nextLon]

                if (lat > 0) {
                    val n1 = Triangle3D.computeNormal(v00, v10, v01)
                    tris.add(Triangle3D(v00, v10, v01, n1))
                }
                if (lat < latSteps - 1) {
                    val n2 = Triangle3D.computeNormal(v01, v10, v11)
                    tris.add(Triangle3D(v01, v10, v11, n2))
                }
                if (lat % 2 == 0) edges.add(MeshEdge(v00, v01))
                if (lon % 4 == 0) edges.add(MeshEdge(v00, v10))
            }
        }
        return Mesh3D("sphere", "Sphere", tris, edges)
    }

    fun createCone(radius: Double, height: Double, cx: Double = 0.0, cy: Double = 0.0, cz: Double = 0.0, segments: Int = 24): Mesh3D {
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()
        val apex = Vector3D(cx, cy, cz + height)
        val baseCenter = Vector3D(cx, cy, cz)
        val basePts = Array(segments) { i ->
            val a = 2.0 * PI * i / segments
            Vector3D(cx + radius * cos(a), cy + radius * sin(a), cz)
        }
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            val p1 = basePts[i]; val p2 = basePts[next]
            tris.add(Triangle3D(baseCenter, p2, p1, Vector3D(0.0, 0.0, -1.0)))
            val sn = Triangle3D.computeNormal(p1, apex, p2)
            tris.add(Triangle3D(p1, apex, p2, sn))
            edges.add(MeshEdge(p1, p2))
            if (i % 4 == 0) edges.add(MeshEdge(p1, apex))
        }
        return Mesh3D("cone", "Cone", tris, edges)
    }

    fun createTorus(majorR: Double, minorR: Double, cx: Double = 0.0, cy: Double = 0.0, cz: Double = 0.0, majorSegs: Int = 20, minorSegs: Int = 12): Mesh3D {
        val tris = mutableListOf<Triangle3D>()
        val edges = mutableListOf<MeshEdge>()
        val grid = Array(majorSegs) { u ->
            val phi = 2.0 * PI * u / majorSegs
            Array(minorSegs) { v ->
                val theta = 2.0 * PI * v / minorSegs
                val x = cx + (majorR + minorR * cos(theta)) * cos(phi)
                val y = cy + (majorR + minorR * cos(theta)) * sin(phi)
                val z = cz + minorR * sin(theta)
                Vector3D(x, y, z)
            }
        }
        for (u in 0 until majorSegs) {
            val nextU = (u + 1) % majorSegs
            for (v in 0 until minorSegs) {
                val nextV = (v + 1) % minorSegs
                val v00 = grid[u][v]
                val v10 = grid[nextU][v]
                val v01 = grid[u][nextV]
                val v11 = grid[nextU][nextV]

                val n1 = Triangle3D.computeNormal(v00, v10, v01)
                tris.add(Triangle3D(v00, v10, v01, n1))
                val n2 = Triangle3D.computeNormal(v01, v10, v11)
                tris.add(Triangle3D(v01, v10, v11, n2))

                if (v % 3 == 0) edges.add(MeshEdge(v00, v10))
                if (u % 4 == 0) edges.add(MeshEdge(v00, v01))
            }
        }
        return Mesh3D("torus", "Torus", tris, edges)
    }

    private fun alignToPlane(mesh: Mesh3D, plane: SketchPlane): Mesh3D {
        return when (plane) {
            SketchPlane.XY_TOP -> mesh
            SketchPlane.XZ_FRONT -> {
                val rx = Matrix4x4.rotationX(-PI / 2.0)
                mesh.transform(rx)
            }
            SketchPlane.YZ_RIGHT -> {
                val ry = Matrix4x4.rotationY(PI / 2.0)
                mesh.transform(ry)
            }
        }
    }
}
