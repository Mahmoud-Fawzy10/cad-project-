package com.example

import com.example.mobicad.geometry.*
import com.example.mobicad.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MobiCadEngineTest {

    @Test
    fun testRectangleExtrusionExactDimensions() {
        val project = CadProject(name = "Test Part", material = Material.PLA)
        val sketch = Sketch2D(name = "Base Sketch", plane = SketchPlane.XY_TOP)
        sketch.addRectangle(0.0, 0.0, 20.0, 30.0)
        project.sketches.add(sketch)

        val feat = Feature(name = "Extrude 1", type = FeatureType.EXTRUDE, sketchId = sketch.id)
        feat.numericParams["depth"] = 10.0
        project.features.add(feat)

        ParametricKernel.recompute(project)
        val mesh = project.generatedMesh
        assertNotNull(mesh)
        assertFalse(mesh!!.triangles.isEmpty())

        // Verify bounding box exact dimensions
        val bb = mesh.boundingBox
        assertEquals(20.0, bb.width, 1e-3)
        assertEquals(30.0, bb.height, 1e-3)
        assertEquals(10.0, bb.depth, 1e-3)

        // Verify exact volume: 20 * 30 * 10 = 6000 mm³
        val volume = mesh.calculateVolume()
        assertEquals(6000.0, volume, 1e-2)

        // Verify PLA mass: 6.00 cm³ * 1.24 g/cm³ = 7.44 g
        val mass = project.material.calculateMassGrams(volume)
        assertEquals(7.44, mass, 1e-2)
    }

    @Test
    fun testParametricFeatureEditAndRecompute() {
        val project = CadProject(name = "Parametric Edit", material = Material.ALUMINUM_6061)
        val sketch = Sketch2D(name = "Base Sketch", plane = SketchPlane.XY_TOP)
        sketch.addRectangle(0.0, 0.0, 20.0, 30.0)
        project.sketches.add(sketch)

        val feat = Feature(name = "Extrude 1", type = FeatureType.EXTRUDE, sketchId = sketch.id)
        feat.numericParams["depth"] = 10.0
        project.features.add(feat)

        ParametricKernel.recompute(project)
        assertEquals(6000.0, project.generatedMesh!!.calculateVolume(), 1e-2)

        // Edit extrusion height from 10mm to 15mm
        feat.numericParams["depth"] = 15.0
        ParametricKernel.recompute(project)

        val updatedMesh = project.generatedMesh!!
        val bb = updatedMesh.boundingBox
        assertEquals(15.0, bb.depth, 1e-3)
        assertEquals(9000.0, updatedMesh.calculateVolume(), 1e-2)
    }

    @Test
    fun testHoleSubtraction() {
        val project = CadProject(name = "Hole Test")
        val sketch = Sketch2D(name = "Plate")
        sketch.addRectangle(0.0, 0.0, 40.0, 40.0)
        project.sketches.add(sketch)

        val feat1 = Feature(name = "Base", type = FeatureType.EXTRUDE, sketchId = sketch.id)
        feat1.numericParams["depth"] = 10.0
        project.features.add(feat1)

        val feat2 = Feature(name = "M4 Hole", type = FeatureType.HOLE)
        feat2.numericParams["diameter"] = 4.5
        feat2.numericParams["depth"] = 10.0
        project.features.add(feat2)

        ParametricKernel.recompute(project)
        val mesh = project.generatedMesh
        assertNotNull(mesh)
        assertTrue(mesh!!.triangles.isNotEmpty())
    }

    @Test
    fun testStlAndObjExport() {
        val project = CadProject(name = "Export Test")
        val sketch = Sketch2D(name = "Profile")
        sketch.addRectangle(0.0, 0.0, 15.0, 25.0)
        project.sketches.add(sketch)

        val feat = Feature(name = "Extrude", type = FeatureType.EXTRUDE, sketchId = sketch.id)
        feat.numericParams["depth"] = 8.0
        project.features.add(feat)

        ParametricKernel.recompute(project)
        val mesh = project.generatedMesh!!

        // Test ASCII STL export
        val stl = StlExporter.exportToAsciiStl(mesh, "ExportPart")
        assertTrue(stl.startsWith("solid ExportPart"))
        assertTrue(stl.contains("facet normal"))
        assertTrue(stl.contains("outer loop"))
        assertTrue(stl.contains("vertex"))
        assertTrue(stl.endsWith("endsolid ExportPart\n"))

        // Test OBJ export
        val obj = ObjExporter.exportToObj(mesh, "ExportPart")
        assertTrue(obj.contains("v "))
        assertTrue(obj.contains("f "))
    }

    @Test
    fun testProjectJsonSerializationRoundtrip() {
        val original = ProjectSerializer.createSampleProject("ESP32 Enclosure")
        val json = ProjectSerializer.toJson(original)
        assertNotNull(json)
        assertTrue(json.contains("ESP32 Sensor Enclosure"))

        val restored = ProjectSerializer.fromJson(json)
        assertEquals(original.name, restored.name)
        assertEquals(original.features.size, restored.features.size)
        assertNotNull(restored.generatedMesh)
        assertTrue(restored.generatedMesh!!.triangles.isNotEmpty())
    }

    @Test
    fun testRoboticsCatalogGeneration() {
        val esp32 = RoboticsLibrary.generateComponent("ESP32", emptyMap())
        assertFalse(esp32.triangles.isEmpty())

        val nema17 = RoboticsLibrary.generateComponent("NEMA17", emptyMap())
        assertFalse(nema17.triangles.isEmpty())

        val servo = RoboticsLibrary.generateComponent("SERVO_SG90", emptyMap())
        assertFalse(servo.triangles.isEmpty())

        val bearing = RoboticsLibrary.generateComponent("BEARING_608", emptyMap())
        assertFalse(bearing.triangles.isEmpty())
    }
}
