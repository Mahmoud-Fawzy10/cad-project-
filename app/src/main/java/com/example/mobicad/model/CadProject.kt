package com.example.mobicad.model

import java.util.UUID

data class CadProject(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Untitled Part",
    var units: String = "mm",
    var material: Material = Material.PLA,
    val sketches: MutableList<Sketch2D> = mutableListOf(),
    val features: MutableList<Feature> = mutableListOf(),
    var generatedMesh: Mesh3D? = null,
    val assemblyParts: MutableList<AssemblyPartInstance> = mutableListOf(),
    val assemblyJoints: MutableList<AssemblyJoint> = mutableListOf(),
    var lastModifiedTimestamp: Long = System.currentTimeMillis()
)
