package com.example.mobicad.model

import kotlin.math.*

data class Point2D(val x: Double, val y: Double) {
    fun distanceTo(other: Point2D): Double = hypot(other.x - x, other.y - y)
    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(scale: Double) = Point2D(x * scale, y * scale)
}

data class Vector3D(val x: Double, val y: Double, val z: Double) {
    fun length(): Double = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 1e-12) Vector3D(x / len, y / len, z / len) else Vector3D(0.0, 0.0, 1.0)
    }

    operator fun plus(o: Vector3D) = Vector3D(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vector3D) = Vector3D(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vector3D(x * s, y * s, z * s)
    operator fun div(s: Double) = Vector3D(x / s, y / s, z / s)
    operator fun unaryMinus() = Vector3D(-x, -y, -z)

    fun dot(o: Vector3D): Double = x * o.x + y * o.y + z * o.z

    fun cross(o: Vector3D): Vector3D = Vector3D(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun distanceTo(o: Vector3D): Double = (this - o).length()

    companion object {
        val ZERO = Vector3D(0.0, 0.0, 0.0)
        val UNIT_X = Vector3D(1.0, 0.0, 0.0)
        val UNIT_Y = Vector3D(0.0, 1.0, 0.0)
        val UNIT_Z = Vector3D(0.0, 0.0, 1.0)
    }
}

data class Triangle3D(
    val v0: Vector3D,
    val v1: Vector3D,
    val v2: Vector3D,
    val normal: Vector3D = computeNormal(v0, v1, v2)
) {
    companion object {
        fun computeNormal(p0: Vector3D, p1: Vector3D, p2: Vector3D): Vector3D {
            val edge1 = p1 - p0
            val edge2 = p2 - p0
            return edge1.cross(edge2).normalized()
        }
    }

    fun area(): Double {
        val edge1 = v1 - v0
        val edge2 = v2 - v0
        return 0.5 * edge1.cross(edge2).length()
    }

    // Signed tetrahedron volume relative to origin for divergence theorem
    fun signedVolumeContribution(): Double {
        return (v0.x * (v1.y * v2.z - v2.y * v1.z) +
                v1.x * (v2.y * v0.z - v0.y * v2.z) +
                v2.x * (v0.y * v1.z - v1.y * v0.z)) / 6.0
    }

    // Centroid of tetrahedron with origin
    fun weightedCentroidContribution(): Vector3D {
        val vol = signedVolumeContribution()
        val c = (v0 + v1 + v2) * 0.25
        return c * vol
    }
}

data class BoundingBox(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double
) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val depth: Double get() = maxZ - minZ
    val center: Vector3D get() = Vector3D((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0)
    val maxDimension: Double get() = maxOf(width, maxOf(height, depth))

    companion object {
        val EMPTY = BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        fun fromPoints(points: List<Vector3D>): BoundingBox {
            if (points.isEmpty()) return EMPTY
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var minZ = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            var maxZ = -Double.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.z < minZ) minZ = p.z
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
                if (p.z > maxZ) maxZ = p.z
            }
            return BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)
        }
    }
}
