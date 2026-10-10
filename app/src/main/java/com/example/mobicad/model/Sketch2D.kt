package com.example.mobicad.model

import java.util.UUID

enum class SketchPlane {
    XY_TOP,    // Z = 0
    XZ_FRONT,  // Y = 0
    YZ_RIGHT   // X = 0
}

sealed class SketchEntity {
    abstract val id: String
    abstract val isConstruction: Boolean

    data class PointEntity(
        override val id: String = UUID.randomUUID().toString(),
        var x: Double,
        var y: Double,
        override val isConstruction: Boolean = false
    ) : SketchEntity()

    data class LineEntity(
        override val id: String = UUID.randomUUID().toString(),
        val p1Id: String,
        val p2Id: String,
        override val isConstruction: Boolean = false
    ) : SketchEntity()

    data class RectangleEntity(
        override val id: String = UUID.randomUUID().toString(),
        val pMinId: String,
        val pMaxId: String,
        override val isConstruction: Boolean = false
    ) : SketchEntity()

    data class CircleEntity(
        override val id: String = UUID.randomUUID().toString(),
        val centerId: String,
        var radius: Double,
        override val isConstruction: Boolean = false
    ) : SketchEntity()

    data class ArcEntity(
        override val id: String = UUID.randomUUID().toString(),
        val centerId: String,
        var radius: Double,
        var startAngleRad: Double,
        var endAngleRad: Double,
        override val isConstruction: Boolean = false
    ) : SketchEntity()

    data class PolygonEntity(
        override val id: String = UUID.randomUUID().toString(),
        val centerId: String,
        var radius: Double,
        val sides: Int = 6,
        override val isConstruction: Boolean = false
    ) : SketchEntity()
}

enum class ConstraintType {
    HORIZONTAL,
    VERTICAL,
    COINCIDENT,
    PARALLEL,
    PERPENDICULAR,
    EQUAL_LENGTH,
    LENGTH_DIMENSION,
    DISTANCE_DIMENSION,
    RADIUS_DIMENSION,
    FIXED
}

data class SketchConstraint(
    val id: String = UUID.randomUUID().toString(),
    val type: ConstraintType,
    val entity1Id: String,
    val entity2Id: String? = null,
    val targetValue: Double? = null // Exact dimension in millimeters
)

data class Sketch2D(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Sketch 1",
    val plane: SketchPlane = SketchPlane.XY_TOP,
    val planeOffsetMm: Double = 0.0,
    val points: MutableMap<String, SketchEntity.PointEntity> = mutableMapOf(),
    val entities: MutableList<SketchEntity> = mutableListOf(),
    val constraints: MutableList<SketchConstraint> = mutableListOf(),
    var isFullyConstrained: Boolean = false
) {
    fun addPoint(x: Double, y: Double): SketchEntity.PointEntity {
        val pt = SketchEntity.PointEntity(x = x, y = y)
        points[pt.id] = pt
        return pt
    }

    fun addLine(x1: Double, y1: Double, x2: Double, y2: Double, isConstruction: Boolean = false): SketchEntity.LineEntity {
        val p1 = addPoint(x1, y1)
        val p2 = addPoint(x2, y2)
        val line = SketchEntity.LineEntity(p1Id = p1.id, p2Id = p2.id, isConstruction = isConstruction)
        entities.add(line)
        return line
    }

    fun addRectangle(x1: Double, y1: Double, width: Double, height: Double): SketchEntity.RectangleEntity {
        val p1 = addPoint(x1, y1)
        val p2 = addPoint(x1 + width, y1 + height)
        val rect = SketchEntity.RectangleEntity(pMinId = p1.id, pMaxId = p2.id)
        entities.add(rect)
        return rect
    }

    fun addCircle(cx: Double, cy: Double, radius: Double): SketchEntity.CircleEntity {
        val center = addPoint(cx, cy)
        val circle = SketchEntity.CircleEntity(centerId = center.id, radius = radius)
        entities.add(circle)
        return circle
    }

    fun addArc(cx: Double, cy: Double, radius: Double, startAngleRad: Double, endAngleRad: Double): SketchEntity.ArcEntity {
        val center = addPoint(cx, cy)
        val arc = SketchEntity.ArcEntity(centerId = center.id, radius = radius, startAngleRad = startAngleRad, endAngleRad = endAngleRad)
        entities.add(arc)
        return arc
    }
}
