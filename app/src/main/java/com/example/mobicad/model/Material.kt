package com.example.mobicad.model

data class Material(
    val name: String,
    val densityGramsPerCm3: Double, // g/cm³
    val youngsModulusGpa: Double = 3.0, // GPa
    val yieldStrengthMpa: Double = 50.0 // MPa
) {
    fun calculateMassGrams(volumeMm3: Double): Double {
        val volumeCm3 = volumeMm3 / 1000.0
        return volumeCm3 * densityGramsPerCm3
    }

    companion object {
        val PLA = Material("PLA (3D Print)", 1.24, 3.5, 45.0)
        val ABS = Material("ABS (3D Print)", 1.04, 2.3, 40.0)
        val PETG = Material("PETG (3D Print)", 1.27, 2.1, 50.0)
        val ALUMINUM_6061 = Material("Aluminum 6061-T6", 2.70, 68.9, 276.0)
        val STEEL_1018 = Material("Steel AISI 1018", 7.85, 205.0, 370.0)
        val BRASS_C360 = Material("Brass C36000", 8.50, 97.0, 310.0)
        val CARBON_FIBER = Material("Carbon Fiber Composite", 1.55, 120.0, 600.0)
        val DELRIN_POM = Material("Delrin / POM", 1.42, 3.1, 65.0)
        val ACRYLIC = Material("Acrylic (PMMA)", 1.18, 3.2, 70.0)

        val PRESETS = listOf(
            PLA, ABS, PETG, ALUMINUM_6061, STEEL_1018, BRASS_C360, CARBON_FIBER, DELRIN_POM, ACRYLIC
        )
    }
}
