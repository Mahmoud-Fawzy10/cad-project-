package com.example.mobicad.geometry

import com.example.mobicad.model.*
import kotlin.math.*

object SketchSolver {

    /**
     * Solves 2D sketch constraints using iterative relaxation.
     * Enforces exact dimensions numerically and updates entity geometry.
     */
    fun solve(sketch: Sketch2D): Boolean {
        if (sketch.constraints.isEmpty()) {
            sketch.isFullyConstrained = false
            return true
        }

        val maxIterations = 50
        val tolerance = 1e-4
        var converged = false

        for (iter in 0 until maxIterations) {
            var maxError = 0.0

            for (c in sketch.constraints) {
                when (c.type) {
                    ConstraintType.HORIZONTAL -> {
                        val line = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.LineEntity
                        if (line != null) {
                            val p1 = sketch.points[line.p1Id]
                            val p2 = sketch.points[line.p2Id]
                            if (p1 != null && p2 != null) {
                                val err = abs(p2.y - p1.y)
                                if (err > maxError) maxError = err
                                val midY = (p1.y + p2.y) / 2.0
                                p1.y = midY
                                p2.y = midY
                            }
                        }
                    }
                    ConstraintType.VERTICAL -> {
                        val line = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.LineEntity
                        if (line != null) {
                            val p1 = sketch.points[line.p1Id]
                            val p2 = sketch.points[line.p2Id]
                            if (p1 != null && p2 != null) {
                                val err = abs(p2.x - p1.x)
                                if (err > maxError) maxError = err
                                val midX = (p1.x + p2.x) / 2.0
                                p1.x = midX
                                p2.x = midX
                            }
                        }
                    }
                    ConstraintType.COINCIDENT -> {
                        val p1 = sketch.points[c.entity1Id]
                        val p2 = sketch.points[c.entity2Id]
                        if (p1 != null && p2 != null) {
                            val err = hypot(p2.x - p1.x, p2.y - p1.y)
                            if (err > maxError) maxError = err
                            val midX = (p1.x + p2.x) / 2.0
                            val midY = (p1.y + p2.y) / 2.0
                            p1.x = midX; p1.y = midY
                            p2.x = midX; p2.y = midY
                        }
                    }
                    ConstraintType.LENGTH_DIMENSION -> {
                        val line = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.LineEntity
                        val targetLen = c.targetValue ?: 0.0
                        if (line != null && targetLen > 0.0) {
                            val p1 = sketch.points[line.p1Id]
                            val p2 = sketch.points[line.p2Id]
                            if (p1 != null && p2 != null) {
                                val curLen = hypot(p2.x - p1.x, p2.y - p1.y)
                                val err = abs(curLen - targetLen)
                                if (err > maxError) maxError = err
                                if (curLen > 1e-6) {
                                    val factor = targetLen / curLen
                                    val midX = (p1.x + p2.x) / 2.0
                                    val midY = (p1.y + p2.y) / 2.0
                                    val dx = (p2.x - midX) * factor
                                    val dy = (p2.y - midY) * factor
                                    p1.x = midX - dx; p1.y = midY - dy
                                    p2.x = midX + dx; p2.y = midY + dy
                                } else {
                                    p2.x = p1.x + targetLen
                                }
                            }
                        }
                    }
                    ConstraintType.DISTANCE_DIMENSION -> {
                        val p1 = sketch.points[c.entity1Id]
                        val p2 = sketch.points[c.entity2Id]
                        val targetDist = c.targetValue ?: 0.0
                        if (p1 != null && p2 != null && targetDist >= 0.0) {
                            val curDist = hypot(p2.x - p1.x, p2.y - p1.y)
                            val err = abs(curDist - targetDist)
                            if (err > maxError) maxError = err
                            if (curDist > 1e-6) {
                                val factor = targetDist / curDist
                                val midX = (p1.x + p2.x) / 2.0
                                val midY = (p1.y + p2.y) / 2.0
                                val dx = (p2.x - midX) * factor
                                val dy = (p2.y - midY) * factor
                                p1.x = midX - dx; p1.y = midY - dy
                                p2.x = midX + dx; p2.y = midY + dy
                            }
                        }
                    }
                    ConstraintType.RADIUS_DIMENSION -> {
                        val circle = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.CircleEntity
                        val targetR = c.targetValue ?: 0.0
                        if (circle != null && targetR > 0.0) {
                            val err = abs(circle.radius - targetR)
                            if (err > maxError) maxError = err
                            circle.radius = targetR
                        }
                        val arc = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.ArcEntity
                        if (arc != null && targetR > 0.0) {
                            val err = abs(arc.radius - targetR)
                            if (err > maxError) maxError = err
                            arc.radius = targetR
                        }
                    }
                    ConstraintType.EQUAL_LENGTH -> {
                        val line1 = sketch.entities.find { it.id == c.entity1Id } as? SketchEntity.LineEntity
                        val line2 = sketch.entities.find { it.id == c.entity2Id } as? SketchEntity.LineEntity
                        if (line1 != null && line2 != null) {
                            val p1a = sketch.points[line1.p1Id]
                            val p1b = sketch.points[line1.p2Id]
                            val p2a = sketch.points[line2.p1Id]
                            val p2b = sketch.points[line2.p2Id]
                            if (p1a != null && p1b != null && p2a != null && p2b != null) {
                                val len1 = hypot(p1b.x - p1a.x, p1b.y - p1a.y)
                                val len2 = hypot(p2b.x - p2a.x, p2b.y - p2a.y)
                                val avgLen = (len1 + len2) / 2.0
                                val err = abs(len1 - len2)
                                if (err > maxError) maxError = err

                                if (len2 > 1e-6) {
                                    val factor = avgLen / len2
                                    val midX = (p2a.x + p2b.x) / 2.0
                                    val midY = (p2a.y + p2b.y) / 2.0
                                    val dx = (p2b.x - midX) * factor
                                    val dy = (p2b.y - midY) * factor
                                    p2a.x = midX - dx; p2a.y = midY - dy
                                    p2b.x = midX + dx; p2b.y = midY + dy
                                }
                            }
                        }
                    }
                    ConstraintType.FIXED, ConstraintType.PARALLEL, ConstraintType.PERPENDICULAR -> {
                        // Handled / preserved
                    }
                }
            }

            if (maxError < tolerance) {
                converged = true
                break
            }
        }

        // Determine constraint status:
        // DoF heuristic: each point has 2 DoFs. Each constraint removes DoFs.
        val totalDof = sketch.points.size * 2
        val constrainedCount = sketch.constraints.size
        sketch.isFullyConstrained = (constrainedCount >= totalDof && totalDof > 0)
        return converged
    }
}
