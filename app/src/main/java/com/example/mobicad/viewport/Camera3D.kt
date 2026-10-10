package com.example.mobicad.viewport

import com.example.mobicad.model.*
import kotlin.math.*

class Camera3D {
    var yawDeg: Double = 45.0
    var pitchDeg: Double = 30.0
    var distance: Double = 120.0
    var target: Vector3D = Vector3D.ZERO
    var isPerspective: Boolean = true

    fun orbit(deltaYaw: Double, deltaPitch: Double) {
        yawDeg += deltaYaw
        pitchDeg = (pitchDeg + deltaPitch).coerceIn(-89.9, 89.9)
    }

    fun pan(dx: Double, dy: Double, viewWidth: Float, viewHeight: Float) {
        val factor = distance / minOf(viewWidth, viewHeight) * 1.2
        val yawRad = Math.toRadians(yawDeg)
        val rightX = cos(yawRad)
        val rightY = -sin(yawRad)
        val upZ = 1.0

        target = Vector3D(
            target.x - rightX * dx * factor,
            target.y - rightY * dx * factor,
            target.z + upZ * dy * factor
        )
    }

    fun zoom(scaleFactor: Double) {
        distance = (distance / scaleFactor).coerceIn(10.0, 2000.0)
    }

    fun setPresetView(preset: String) {
        when (preset.uppercase()) {
            "ISO", "ISOMETRIC" -> { yawDeg = 45.0; pitchDeg = 35.264 }
            "TOP" -> { yawDeg = 0.0; pitchDeg = 89.9 }
            "FRONT" -> { yawDeg = 0.0; pitchDeg = 0.0 }
            "RIGHT" -> { yawDeg = 90.0; pitchDeg = 0.0 }
            "BACK" -> { yawDeg = 180.0; pitchDeg = 0.0 }
            "LEFT" -> { yawDeg = -90.0; pitchDeg = 0.0 }
            "BOTTOM" -> { yawDeg = 0.0; pitchDeg = -89.9 }
        }
    }

    fun fitToBoundingBox(bb: BoundingBox) {
        if (bb == BoundingBox.EMPTY) {
            distance = 100.0
            target = Vector3D.ZERO
            return
        }
        target = bb.center
        val maxDim = bb.maxDimension.coerceAtLeast(10.0)
        distance = maxDim * 2.2
    }

    fun computeEyePosition(): Vector3D {
        val yawRad = Math.toRadians(yawDeg)
        val pitchRad = Math.toRadians(pitchDeg)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        val x = target.x + distance * cosPitch * sin(yawRad)
        val y = target.y - distance * cosPitch * cos(yawRad)
        val z = target.z + distance * sinPitch
        return Vector3D(x, y, z)
    }

    fun getViewMatrix(): Matrix4x4 {
        val eye = computeEyePosition()
        val up = Vector3D(0.0, 0.0, 1.0)
        return Matrix4x4.lookAt(eye, target, up)
    }

    fun getProjectionMatrix(width: Double, height: Double): Matrix4x4 {
        val aspect = width / height.coerceAtLeast(1.0)
        return if (isPerspective) {
            Matrix4x4.perspective(Math.toRadians(45.0), aspect, 1.0, 5000.0)
        } else {
            val orthoHalf = distance * 0.4
            Matrix4x4.ortho(-orthoHalf * aspect, orthoHalf * aspect, -orthoHalf, orthoHalf, -2000.0, 2000.0)
        }
    }

    /**
     * Projects a 3D point in world space to 2D viewport coordinates in pixels.
     * Returns Triple(screenX, screenY, depthZ)
     */
    fun project(p: Vector3D, width: Double, height: Double): Triple<Float, Float, Double>? {
        val viewMat = getViewMatrix()
        val projMat = getProjectionMatrix(width, height)
        val viewP = viewMat.transformPoint(p)

        // Clip near plane in perspective
        if (isPerspective && viewP.z > -1.0) return null

        val clipP = projMat.transformPoint(viewP)
        val screenX = ((clipP.x + 1.0) * 0.5 * width).toFloat()
        val screenY = ((1.0 - clipP.y) * 0.5 * height).toFloat()
        return Triple(screenX, screenY, viewP.z)
    }
}
