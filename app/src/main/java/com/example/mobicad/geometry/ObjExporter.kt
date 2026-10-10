package com.example.mobicad.geometry

import com.example.mobicad.model.Mesh3D
import java.util.Locale

object ObjExporter {

    /**
     * Exports Mesh3D to Wavefront OBJ format for interoperability with CAD and DCC software.
     */
    fun exportToObj(mesh: Mesh3D, objectName: String = "MobiCAD_Object"): String {
        val sb = StringBuilder()
        sb.append("# MobiCAD Pro 3D OBJ Export\n")
        sb.append("# Offline Mechanical Parametric CAD for Android\n")
        sb.append("o ").append(objectName.replace(" ", "_")).append("\n\n")

        var vIndex = 1
        for (t in mesh.triangles) {
            sb.append(String.format(Locale.US, "v %.4f %.4f %.4f\n", t.v0.x, t.v0.y, t.v0.z))
            sb.append(String.format(Locale.US, "v %.4f %.4f %.4f\n", t.v1.x, t.v1.y, t.v1.z))
            sb.append(String.format(Locale.US, "v %.4f %.4f %.4f\n", t.v2.x, t.v2.y, t.v2.z))
            sb.append(String.format(Locale.US, "vn %.4f %.4f %.4f\n", t.normal.x, t.normal.y, t.normal.z))
            sb.append(String.format(Locale.US, "f %d//%d %d//%d %d//%d\n", vIndex, vIndex / 3 + 1, vIndex + 1, vIndex / 3 + 1, vIndex + 2, vIndex / 3 + 1))
            vIndex += 3
        }

        return sb.toString()
    }
}
