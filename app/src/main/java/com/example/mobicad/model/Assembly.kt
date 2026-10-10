package com.example.mobicad.model

import java.util.UUID

enum class JointType {
    FIXED,
    REVOLUTE,
    SLIDER,
    CYLINDRICAL
}

data class AssemblyPartInstance(
    val id: String = UUID.randomUUID().toString(),
    val partDefId: String,
    val name: String,
    var translation: Vector3D = Vector3D.ZERO,
    var rotationDeg: Vector3D = Vector3D.ZERO, // X, Y, Z degrees
    var isGrounded: Boolean = false,
    var colorHex: Long = 0xFF4392F1
) {
    fun getTransformMatrix(): Matrix4x4 {
        val tMat = Matrix4x4.translation(translation.x, translation.y, translation.z)
        val rx = Matrix4x4.rotationX(Math.toRadians(rotationDeg.x))
        val ry = Matrix4x4.rotationY(Math.toRadians(rotationDeg.y))
        val rz = Matrix4x4.rotationZ(Math.toRadians(rotationDeg.z))
        return tMat * rz * ry * rx
    }
}

data class AssemblyJoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: JointType,
    val part1Id: String,
    val part2Id: String,
    val origin: Vector3D = Vector3D.ZERO,
    val axis: Vector3D = Vector3D.UNIT_Z,
    val minLimit: Double = -180.0,
    val maxLimit: Double = 180.0,
    var currentValue: Double = 0.0 // Deg or mm
)
