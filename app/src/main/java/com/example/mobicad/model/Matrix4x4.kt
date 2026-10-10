package com.example.mobicad.model

import kotlin.math.*

class Matrix4x4(val m: DoubleArray = DoubleArray(16)) {
    init {
        require(m.size == 16)
    }

    operator fun get(row: Int, col: Int): Double = m[row * 4 + col]
    operator fun set(row: Int, col: Int, v: Double) { m[row * 4 + col] = v }

    operator fun times(o: Matrix4x4): Matrix4x4 {
        val res = DoubleArray(16)
        for (r in 0..3) {
            for (c in 0..3) {
                var sum = 0.0
                for (k in 0..3) {
                    sum += m[r * 4 + k] * o.m[k * 4 + c]
                }
                res[r * 4 + c] = sum
            }
        }
        return Matrix4x4(res)
    }

    fun transformPoint(p: Vector3D): Vector3D {
        val x = p.x * m[0] + p.y * m[1] + p.z * m[2] + m[3]
        val y = p.x * m[4] + p.y * m[5] + p.z * m[6] + m[7]
        val z = p.x * m[8] + p.y * m[9] + p.z * m[10] + m[11]
        val w = p.x * m[12] + p.y * m[13] + p.z * m[14] + m[15]
        val invW = if (abs(w) > 1e-9) 1.0 / w else 1.0
        return Vector3D(x * invW, y * invW, z * invW)
    }

    fun transformVector(v: Vector3D): Vector3D {
        return Vector3D(
            v.x * m[0] + v.y * m[1] + v.z * m[2],
            v.x * m[4] + v.y * m[5] + v.z * m[6],
            v.x * m[8] + v.y * m[9] + v.z * m[10]
        ).normalized()
    }

    companion object {
        fun identity(): Matrix4x4 {
            val r = DoubleArray(16)
            r[0] = 1.0; r[5] = 1.0; r[10] = 1.0; r[15] = 1.0
            return Matrix4x4(r)
        }

        fun translation(tx: Double, ty: Double, tz: Double): Matrix4x4 {
            val res = identity()
            res[0, 3] = tx
            res[1, 3] = ty
            res[2, 3] = tz
            return res
        }

        fun scale(sx: Double, sy: Double, sz: Double): Matrix4x4 {
            val res = identity()
            res[0, 0] = sx
            res[1, 1] = sy
            res[2, 2] = sz
            return res
        }

        fun rotationX(rad: Double): Matrix4x4 {
            val res = identity()
            val c = cos(rad)
            val s = sin(rad)
            res[1, 1] = c; res[1, 2] = -s
            res[2, 1] = s; res[2, 2] = c
            return res
        }

        fun rotationY(rad: Double): Matrix4x4 {
            val res = identity()
            val c = cos(rad)
            val s = sin(rad)
            res[0, 0] = c; res[0, 2] = s
            res[2, 0] = -s; res[2, 2] = c
            return res
        }

        fun rotationZ(rad: Double): Matrix4x4 {
            val res = identity()
            val c = cos(rad)
            val s = sin(rad)
            res[0, 0] = c; res[0, 1] = -s
            res[1, 0] = s; res[1, 1] = c
            return res
        }

        fun lookAt(eye: Vector3D, target: Vector3D, up: Vector3D): Matrix4x4 {
            val f = (target - eye).normalized()
            val s = f.cross(up).normalized()
            val u = s.cross(f)

            val m = DoubleArray(16)
            m[0] = s.x;  m[1] = s.y;  m[2] = s.z;  m[3] = -s.dot(eye)
            m[4] = u.x;  m[5] = u.y;  m[6] = u.z;  m[7] = -u.dot(eye)
            m[8] = -f.x; m[9] = -f.y; m[10] = -f.z; m[11] = f.dot(eye)
            m[12] = 0.0; m[13] = 0.0; m[14] = 0.0; m[15] = 1.0
            return Matrix4x4(m)
        }

        fun perspective(fovyRad: Double, aspect: Double, near: Double, far: Double): Matrix4x4 {
            val tanHalfFov = tan(fovyRad / 2.0)
            val m = DoubleArray(16)
            m[0] = 1.0 / (aspect * tanHalfFov)
            m[5] = 1.0 / tanHalfFov
            m[10] = -(far + near) / (far - near)
            m[11] = -(2.0 * far * near) / (far - near)
            m[14] = -1.0
            m[15] = 0.0
            return Matrix4x4(m)
        }

        fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Matrix4x4 {
            val m = DoubleArray(16)
            m[0] = 2.0 / (right - left)
            m[3] = -(right + left) / (right - left)
            m[5] = 2.0 / (top - bottom)
            m[7] = -(top + bottom) / (top - bottom)
            m[10] = -2.0 / (far - near)
            m[11] = -(far + near) / (far - near)
            m[15] = 1.0
            return Matrix4x4(m)
        }
    }
}
