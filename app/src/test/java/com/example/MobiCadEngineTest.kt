package com.example

import com.example.mobicad.geometry.*
import com.example.mobicad.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test
    fun test3DPrimitivesCreation() {
        val sphere = ParametricKernel.createSphere(radius = 12.0)
        assertFalse(sphere.triangles.isEmpty())
        assertTrue(sphere.calculateVolume() > 0)

        val cone = ParametricKernel.createCone(radius = 10.0, height = 25.0)
        assertFalse(cone.triangles.isEmpty())
        assertTrue(cone.calculateVolume() > 0)

        val torus = ParametricKernel.createTorus(majorR = 20.0, minorR = 5.0)
        assertFalse(torus.triangles.isEmpty())
    }

    @Test
    fun testPerspectiveAndOrthoCameraProjections() {
        val camera = com.example.mobicad.viewport.Camera3D()
        val pt = Vector3D(10.0, 10.0, 10.0)

        // Perspective
        camera.isPerspective = true
        val perspProj = camera.project(pt, 800.0, 600.0)
        assertNotNull(perspProj)

        // Orthographic
        camera.isPerspective = false
        val orthoProj = camera.project(pt, 800.0, 600.0)
        assertNotNull(orthoProj)
    }

    @Test
    fun testHitTestObjectSelection() {
        val box = ParametricKernel.createBox(-10.0, -10.0, -10.0, 10.0, 10.0, 10.0)
        val camera = com.example.mobicad.viewport.Camera3D().apply {
            yawDeg = 0.0
            pitchDeg = 0.0
            distance = 50.0
        }

        // Center tap (400, 300 on an 800x600 screen) should hit the box centered at origin
        val hitCenter = com.example.mobicad.viewport.ViewportRenderer.hitTest(
            400f, 300f, camera, box, 800.0, 600.0
        )
        assertTrue(hitCenter)

        // Tap far off at corner (10, 10) should not hit
        val hitCorner = com.example.mobicad.viewport.ViewportRenderer.hitTest(
            10f, 10f, camera, box, 800.0, 600.0
        )
        assertFalse(hitCorner)
    }

    @Test
    fun testCodeCadScriptParsingAndExecution() {
        val project = CadProject(name = "Script Test")
        val script = """
            // Test Script
            material("ALUMINUM_6061")
            width = 40.0
            height = 20.0
            box(w=width, l=30.0, h=height)
            hole(diameter=4.0, depth=height, x=0.0, y=0.0)
            fillet(radius=1.5)
        """.trimIndent()

        val result = CodeCadEngine.parseAndExecute(script, project)
        assertTrue(result.isSuccess)
        assertEquals(3, result.featuresCount)
        assertEquals(Material.ALUMINUM_6061, project.material)
        assertNotNull(project.generatedMesh)
        assertTrue(project.generatedMesh!!.triangles.isNotEmpty())
    }

    @Test
    fun testProjectToScriptRoundtrip() {
        val project = CadProject(name = "Roundtrip Part", material = Material.BRASS_C360)
        val boxFeat = Feature(name = "Box", type = FeatureType.PRIMITIVE_BOX)
        boxFeat.numericParams["width"] = 50.0
        boxFeat.numericParams["length"] = 40.0
        boxFeat.numericParams["height"] = 25.0
        project.features.add(boxFeat)

        val script = CodeCadEngine.projectToScript(project)
        assertTrue(script.contains("material(\"Brass C36000\")"))
        assertTrue(script.contains("box("))

        // Re-execute script into a fresh project
        val freshProject = CadProject(name = "Fresh")
        val result = CodeCadEngine.parseAndExecute(script, freshProject)
        assertTrue(result.isSuccess)
        assertEquals(Material.BRASS_C360, freshProject.material)
        assertEquals(1, freshProject.features.size)
        assertNotNull(freshProject.generatedMesh)
    }

    @Test
    fun testCodeCadTemplatesExecution() {
        for ((tmplName, tmplCode) in CodeCadEngine.TEMPLATES) {
            val project = CadProject(name = tmplName)
            val res = CodeCadEngine.parseAndExecute(tmplCode, project)
            assertTrue("Template '$tmplName' failed: ${res.message}", res.isSuccess)
            assertNotNull("Template '$tmplName' produced null mesh", project.generatedMesh)
            assertTrue("Template '$tmplName' produced empty triangles", project.generatedMesh!!.triangles.isNotEmpty())
        }
    }

    @Test
    fun testAiCadGenerativeOfflineEngine() {
        val project = CadProject(name = "AI Test")

        // 1. ESP32 enclosure prompt
        val resEnclosure = AiCadGenerator.offlineGenerativeCad("صمم علبة ESP32 مع ثقوب M3", project, true)
        assertTrue(resEnclosure.isSuccess)
        assertTrue(resEnclosure.generatedScript.contains("box("))
        assertTrue(resEnclosure.generatedScript.contains("hole("))

        // Execute generated script to verify validity
        val exec1 = CodeCadEngine.parseAndExecute(resEnclosure.generatedScript, project)
        assertTrue(exec1.isSuccess)

        // 2. Motor mount prompt
        val resMotor = AiCadGenerator.offlineGenerativeCad("Create a NEMA 17 motor bracket with mounting holes", project, false)
        assertTrue(resMotor.isSuccess)
        assertTrue(resMotor.generatedScript.contains("NEMA17") || resMotor.generatedScript.contains("hole("))

        val exec2 = CodeCadEngine.parseAndExecute(resMotor.generatedScript, project)
        assertTrue(exec2.isSuccess)
    }
}

