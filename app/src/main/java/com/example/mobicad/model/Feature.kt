package com.example.mobicad.model

import java.util.UUID

enum class FeatureType {
    EXTRUDE,
    CUT_EXTRUDE,
    REVOLVE,
    REVOLVED_CUT,
    HOLE,
    FILLET,
    CHAMFER,
    SHELL,
    LINEAR_PATTERN,
    CIRCULAR_PATTERN,
    MIRROR,
    BOOLEAN_UNION,
    BOOLEAN_SUBTRACT,
    BOOLEAN_INTERSECT,
    REFERENCE_PLANE,
    ROBOTICS_COMPONENT,
    PRIMITIVE_BOX,
    PRIMITIVE_CYLINDER,
    PRIMITIVE_SPHERE,
    PRIMITIVE_CONE,
    PRIMITIVE_TORUS
}

data class Feature(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: FeatureType,
    val sketchId: String? = null,
    val numericParams: MutableMap<String, Double> = mutableMapOf(),
    val stringParams: MutableMap<String, String> = mutableMapOf(),
    val parentDependencies: MutableList<String> = mutableListOf(),
    var isSuppressed: Boolean = false,
    var isValid: Boolean = true,
    var errorMessage: String? = null
) {
    fun getParam(key: String, default: Double): Double = numericParams[key] ?: default
    fun getStringParam(key: String, default: String): String = stringParams[key] ?: default
}
