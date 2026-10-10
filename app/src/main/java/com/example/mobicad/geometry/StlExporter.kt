package com.example.mobicad.geometry

import com.example.mobicad.model.Mesh3D
import java.util.Locale

object StlExporter {

    /**
     * Generates a valid ASCII STL triangulated surface with unit millimeter scale.
     * Ready for 3D slicers (Cura, PrusaSlicer, Bambu Studio) and CAM systems.
     */
    fun exportToAsciiStl(mesh: Mesh3D, solidName: String = "MobiCAD_Solid"): String {
        val sb = StringBuilder()
        sb.append("solid ").append(solidName.replace(" ", "_")).append("\n")

        for (t in mesh.triangles) {
            val n = t.normal
            sb.append(String.format(Locale.US, "  facet normal %.6e %.6e %.6e\n", n.x, n.y, n.z))
            sb.append("    outer loop\n")
            sb.append(String.format(Locale.US, "      vertex %.6e %.6e %.6e\n", t.v0.x, t.v0.y, t.v0.z))
            sb.append(String.format(Locale.US, "      vertex %.6e %.6e %.6e\n", t.v1.x, t.v1.y, t.v1.z))
            sb.append(String.format(Locale.US, "      vertex %.6e %.6e %.6e\n", t.v2.x, t.v2.y, t.v2.z))
            sb.append("    endloop\n")
            sb.append("  endfacet\n")
        }

        sb.append("endsolid ").append(solidName.replace(" ", "_")).append("\n")
        return sb.toString()
    }
}
