package com.example.mobicad.model

import kotlin.math.abs

data class MeshEdge(val p1: Vector3D, val p2: Vector3D)

data class Mesh3D(
    val id: String,
    val name: String,
    val triangles: List<Triangle3D>,
    val edges: List<MeshEdge> = emptyList(),
    val colorHex: Long = 0xFF4392F1
) {
    val boundingBox: BoundingBox by lazy {
        val pts = ArrayList<Vector3D>(triangles.size * 3)
        for (t in triangles) {
            pts.add(t.v0)
            pts.add(t.v1)
            pts.add(t.v2)
        }
        BoundingBox.fromPoints(pts)
    }

    /**
     * Compute solid volume using Gauss divergence theorem on closed manifold triangle surface.
     * Returns volume in cubic millimeters (mm³).
     */
    fun calculateVolume(): Double {
        if (triangles.isEmpty()) return 0.0
        var totalSignedVolume = 0.0
        for (t in triangles) {
            totalSignedVolume += t.signedVolumeContribution()
        }
        return abs(totalSignedVolume)
    }

    /**
     * Compute total surface area in square millimeters (mm²).
     */
    fun calculateSurfaceArea(): Double {
        var totalArea = 0.0
        for (t in triangles) {
            totalArea += t.area()
        }
        return totalArea
    }

    /**
     * Compute geometric center of mass (centroid of enclosed volume).
     */
    fun calculateCenterOfMass(): Vector3D {
        if (triangles.isEmpty()) return Vector3D.ZERO
        var totalVol = 0.0
        var weightedCentroidSum = Vector3D.ZERO
        for (t in triangles) {
            val volContr = t.signedVolumeContribution()
            val cContr = t.weightedCentroidContribution()
            totalVol += volContr
            weightedCentroidSum = weightedCentroidSum + cContr
        }
        return if (abs(totalVol) > 1e-9) {
            weightedCentroidSum / totalVol
        } else {
            boundingBox.center
        }
    }

    fun transform(mat: Matrix4x4): Mesh3D {
        val newTris = triangles.map { t ->
            val v0 = mat.transformPoint(t.v0)
            val v1 = mat.transformPoint(t.v1)
            val v2 = mat.transformPoint(t.v2)
            val n = mat.transformVector(t.normal)
            Triangle3D(v0, v1, v2, n)
        }
        val newEdges = edges.map { e ->
            MeshEdge(mat.transformPoint(e.p1), mat.transformPoint(e.p2))
        }
        return copy(triangles = newTris, edges = newEdges)
    }

    companion object {
        val EMPTY = Mesh3D("empty", "Empty", emptyList())
    }
}
