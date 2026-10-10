package com.example.mobicad.geometry

import android.content.Context
import com.example.mobicad.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ProjectSerializer {

    fun toJson(project: CadProject): String {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("units", project.units)
        root.put("materialName", project.material.name)
        root.put("materialDensity", project.material.densityGramsPerCm3)
        root.put("lastModifiedTimestamp", project.lastModifiedTimestamp)

        // Sketches
        val sketchesArr = JSONArray()
        for (s in project.sketches) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("name", s.name)
            sObj.put("plane", s.plane.name)
            sObj.put("planeOffsetMm", s.planeOffsetMm)

            // Points
            val ptsArr = JSONArray()
            for (p in s.points.values) {
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("x", p.x)
                pObj.put("y", p.y)
                ptsArr.put(pObj)
            }
            sObj.put("points", ptsArr)

            // Entities
            val entArr = JSONArray()
            for (e in s.entities) {
                val eObj = JSONObject()
                eObj.put("id", e.id)
                eObj.put("isConstruction", e.isConstruction)
                when (e) {
                    is SketchEntity.LineEntity -> {
                        eObj.put("type", "LINE")
                        eObj.put("p1Id", e.p1Id)
                        eObj.put("p2Id", e.p2Id)
                    }
                    is SketchEntity.RectangleEntity -> {
                        eObj.put("type", "RECTANGLE")
                        eObj.put("pMinId", e.pMinId)
                        eObj.put("pMaxId", e.pMaxId)
                    }
                    is SketchEntity.CircleEntity -> {
                        eObj.put("type", "CIRCLE")
                        eObj.put("centerId", e.centerId)
                        eObj.put("radius", e.radius)
                    }
                    is SketchEntity.ArcEntity -> {
                        eObj.put("type", "ARC")
                        eObj.put("centerId", e.centerId)
                        eObj.put("radius", e.radius)
                        eObj.put("startAngleRad", e.startAngleRad)
                        eObj.put("endAngleRad", e.endAngleRad)
                    }
                    is SketchEntity.PolygonEntity -> {
                        eObj.put("type", "POLYGON")
                        eObj.put("centerId", e.centerId)
                        eObj.put("radius", e.radius)
                        eObj.put("sides", e.sides)
                    }
                    else -> {}
                }
                entArr.put(eObj)
            }
            sObj.put("entities", entArr)

            // Constraints
            val constrArr = JSONArray()
            for (c in s.constraints) {
                val cObj = JSONObject()
                cObj.put("id", c.id)
                cObj.put("type", c.type.name)
                cObj.put("entity1Id", c.entity1Id)
                if (c.entity2Id != null) cObj.put("entity2Id", c.entity2Id)
                if (c.targetValue != null) cObj.put("targetValue", c.targetValue)
                constrArr.put(cObj)
            }
            sObj.put("constraints", constrArr)

            sketchesArr.put(sObj)
        }
        root.put("sketches", sketchesArr)

        // Features
        val featArr = JSONArray()
        for (f in project.features) {
            val fObj = JSONObject()
            fObj.put("id", f.id)
            fObj.put("name", f.name)
            fObj.put("type", f.type.name)
            if (f.sketchId != null) fObj.put("sketchId", f.sketchId)
            fObj.put("isSuppressed", f.isSuppressed)

            val npObj = JSONObject()
            for ((k, v) in f.numericParams) {
                npObj.put(k, v)
            }
            fObj.put("numericParams", npObj)

            val spObj = JSONObject()
            for ((k, v) in f.stringParams) {
                spObj.put(k, v)
            }
            fObj.put("stringParams", spObj)

            featArr.put(fObj)
        }
        root.put("features", featArr)

        // Assemblies
        val asmArr = JSONArray()
        for (a in project.assemblyParts) {
            val aObj = JSONObject()
            aObj.put("id", a.id)
            aObj.put("partDefId", a.partDefId)
            aObj.put("name", a.name)
            aObj.put("tx", a.translation.x)
            aObj.put("ty", a.translation.y)
            aObj.put("tz", a.translation.z)
            aObj.put("rx", a.rotationDeg.x)
            aObj.put("ry", a.rotationDeg.y)
            aObj.put("rz", a.rotationDeg.z)
            aObj.put("isGrounded", a.isGrounded)
            asmArr.put(aObj)
        }
        root.put("assemblyParts", asmArr)

        val jntArr = JSONArray()
        for (j in project.assemblyJoints) {
            val jObj = JSONObject()
            jObj.put("id", j.id)
            jObj.put("name", j.name)
            jObj.put("type", j.type.name)
            jObj.put("part1Id", j.part1Id)
            jObj.put("part2Id", j.part2Id)
            jObj.put("currentValue", j.currentValue)
            jntArr.put(jObj)
        }
        root.put("assemblyJoints", jntArr)

        return root.toString(2)
    }

    fun fromJson(jsonStr: String): CadProject {
        val root = JSONObject(jsonStr)
        val id = root.optString("id")
        val name = root.optString("name", "Untitled")
        val units = root.optString("units", "mm")
        val matName = root.optString("materialName", "PLA (3D Print)")
        val matDens = root.optDouble("materialDensity", 1.24)
        val material = Material(matName, matDens)

        val project = CadProject(id = id, name = name, units = units, material = material)

        val sketchesArr = root.optJSONArray("sketches")
        if (sketchesArr != null) {
            for (i in 0 until sketchesArr.length()) {
                val sObj = sketchesArr.getJSONObject(i)
                val sId = sObj.getString("id")
                val sName = sObj.optString("name", "Sketch")
                val plane = try { SketchPlane.valueOf(sObj.optString("plane", "XY_TOP")) } catch (e: Exception) { SketchPlane.XY_TOP }
                val offset = sObj.optDouble("planeOffsetMm", 0.0)

                val sketch = Sketch2D(id = sId, name = sName, plane = plane, planeOffsetMm = offset)

                val ptsArr = sObj.optJSONArray("points")
                if (ptsArr != null) {
                    for (pIdx in 0 until ptsArr.length()) {
                        val pObj = ptsArr.getJSONObject(pIdx)
                        val pId = pObj.getString("id")
                        val px = pObj.getDouble("x")
                        val py = pObj.getDouble("y")
                        sketch.points[pId] = SketchEntity.PointEntity(id = pId, x = px, y = py)
                    }
                }

                val entArr = sObj.optJSONArray("entities")
                if (entArr != null) {
                    for (eIdx in 0 until entArr.length()) {
                        val eObj = entArr.getJSONObject(eIdx)
                        val eId = eObj.getString("id")
                        val isConst = eObj.optBoolean("isConstruction", false)
                        val type = eObj.optString("type")
                        when (type) {
                            "LINE" -> {
                                sketch.entities.add(SketchEntity.LineEntity(eId, eObj.getString("p1Id"), eObj.getString("p2Id"), isConst))
                            }
                            "RECTANGLE" -> {
                                sketch.entities.add(SketchEntity.RectangleEntity(eId, eObj.getString("pMinId"), eObj.getString("pMaxId"), isConst))
                            }
                            "CIRCLE" -> {
                                sketch.entities.add(SketchEntity.CircleEntity(eId, eObj.getString("centerId"), eObj.getDouble("radius"), isConst))
                            }
                            "ARC" -> {
                                sketch.entities.add(SketchEntity.ArcEntity(eId, eObj.getString("centerId"), eObj.getDouble("radius"), eObj.getDouble("startAngleRad"), eObj.getDouble("endAngleRad"), isConst))
                            }
                            "POLYGON" -> {
                                sketch.entities.add(SketchEntity.PolygonEntity(eId, eObj.getString("centerId"), eObj.getDouble("radius"), eObj.optInt("sides", 6), isConst))
                            }
                        }
                    }
                }

                val constrArr = sObj.optJSONArray("constraints")
                if (constrArr != null) {
                    for (cIdx in 0 until constrArr.length()) {
                        val cObj = constrArr.getJSONObject(cIdx)
                        val cType = try { ConstraintType.valueOf(cObj.getString("type")) } catch (e: Exception) { ConstraintType.HORIZONTAL }
                        val e1 = cObj.getString("entity1Id")
                        val e2 = if (cObj.has("entity2Id")) cObj.getString("entity2Id") else null
                        val tVal = if (cObj.has("targetValue")) cObj.getDouble("targetValue") else null
                        sketch.constraints.add(SketchConstraint(cObj.getString("id"), cType, e1, e2, tVal))
                    }
                }

                project.sketches.add(sketch)
            }
        }

        val featArr = root.optJSONArray("features")
        if (featArr != null) {
            for (i in 0 until featArr.length()) {
                val fObj = featArr.getJSONObject(i)
                val fId = fObj.getString("id")
                val fName = fObj.getString("name")
                val fType = try { FeatureType.valueOf(fObj.getString("type")) } catch (e: Exception) { FeatureType.EXTRUDE }
                val sketchId = if (fObj.has("sketchId")) fObj.getString("sketchId") else null
                val isSupp = fObj.optBoolean("isSuppressed", false)

                val feat = Feature(id = fId, name = fName, type = fType, sketchId = sketchId, isSuppressed = isSupp)

                val npObj = fObj.optJSONObject("numericParams")
                if (npObj != null) {
                    val keys = npObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        feat.numericParams[k] = npObj.getDouble(k)
                    }
                }

                val spObj = fObj.optJSONObject("stringParams")
                if (spObj != null) {
                    val keys = spObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        feat.stringParams[k] = spObj.getString(k)
                    }
                }

                project.features.add(feat)
            }
        }

        val asmArr = root.optJSONArray("assemblyParts")
        if (asmArr != null) {
            for (i in 0 until asmArr.length()) {
                val aObj = asmArr.getJSONObject(i)
                val aPart = AssemblyPartInstance(
                    id = aObj.getString("id"),
                    partDefId = aObj.getString("partDefId"),
                    name = aObj.getString("name"),
                    translation = Vector3D(aObj.optDouble("tx", 0.0), aObj.optDouble("ty", 0.0), aObj.optDouble("tz", 0.0)),
                    rotationDeg = Vector3D(aObj.optDouble("rx", 0.0), aObj.optDouble("ry", 0.0), aObj.optDouble("rz", 0.0)),
                    isGrounded = aObj.optBoolean("isGrounded", false)
                )
                project.assemblyParts.add(aPart)
            }
        }

        val jntArr = root.optJSONArray("assemblyJoints")
        if (jntArr != null) {
            for (i in 0 until jntArr.length()) {
                val jObj = jntArr.getJSONObject(i)
                val jType = try { JointType.valueOf(jObj.getString("type")) } catch (e: Exception) { JointType.FIXED }
                project.assemblyJoints.add(
                    AssemblyJoint(
                        id = jObj.getString("id"),
                        name = jObj.getString("name"),
                        type = jType,
                        part1Id = jObj.getString("part1Id"),
                        part2Id = jObj.getString("part2Id"),
                        currentValue = jObj.optDouble("currentValue", 0.0)
                    )
                )
            }
        }

        ParametricKernel.recompute(project)
        return project
    }

    fun saveProjectLocally(context: Context, project: CadProject): File {
        val dir = File(context.filesDir, "mobicad_projects")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${project.id}.mobicad")
        project.lastModifiedTimestamp = System.currentTimeMillis()
        file.writeText(toJson(project))
        return file
    }

    fun listLocalProjects(context: Context): List<Pair<String, String>> {
        val dir = File(context.filesDir, "mobicad_projects")
        if (!dir.exists()) return emptyList()
        val files = dir.listFiles { f -> f.extension == "mobicad" } ?: return emptyList()
        return files.map { f ->
            try {
                val json = JSONObject(f.readText())
                Pair(f.nameWithoutExtension, json.optString("name", "Untitled"))
            } catch (e: Exception) {
                Pair(f.nameWithoutExtension, f.nameWithoutExtension)
            }
        }
    }

    fun loadProjectLocally(context: Context, projectId: String): CadProject? {
        val dir = File(context.filesDir, "mobicad_projects")
        val file = File(dir, "$projectId.mobicad")
        if (!file.exists()) return null
        return try {
            fromJson(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun createSampleProject(sampleName: String): CadProject {
        return when (sampleName) {
            "ESP32 Enclosure" -> {
                val p = CadProject(name = "ESP32 Sensor Enclosure", material = Material.PLA)
                val s1 = Sketch2D(name = "Outer Profile", plane = SketchPlane.XY_TOP)
                s1.addRectangle(-32.0, -20.0, 64.0, 40.0)
                p.sketches.add(s1)
                val f1 = Feature(name = "Base Extrude", type = FeatureType.EXTRUDE, sketchId = s1.id)
                f1.numericParams["depth"] = 18.0
                p.features.add(f1)
                val f2 = Feature(name = "ESP32 Carrier", type = FeatureType.ROBOTICS_COMPONENT)
                f2.stringParams["component"] = "ESP32"
                p.features.add(f2)
                ParametricKernel.recompute(p)
                p
            }
            "NEMA 17 Mount" -> {
                val p = CadProject(name = "NEMA 17 Stepper Mount", material = Material.ALUMINUM_6061)
                val f1 = Feature(name = "Motor Bracket", type = FeatureType.ROBOTICS_COMPONENT)
                f1.stringParams["component"] = "NEMA17"
                f1.numericParams["thickness"] = 5.0
                p.features.add(f1)
                ParametricKernel.recompute(p)
                p
            }
            "Differential Chassis" -> {
                val p = CadProject(name = "Autonomous Robot Chassis", material = Material.PETG)
                val f1 = Feature(name = "Chassis Plate", type = FeatureType.ROBOTICS_COMPONENT)
                f1.stringParams["component"] = "CHASSIS_PLATE"
                p.features.add(f1)
                ParametricKernel.recompute(p)
                p
            }
            else -> {
                val p = CadProject(name = "Mechanical Bracket", material = Material.PLA)
                val s1 = Sketch2D(name = "Base Sketch", plane = SketchPlane.XY_TOP)
                s1.addRectangle(-20.0, -15.0, 40.0, 30.0)
                p.sketches.add(s1)
                val f1 = Feature(name = "Extrude 1", type = FeatureType.EXTRUDE, sketchId = s1.id)
                f1.numericParams["depth"] = 12.0
                p.features.add(f1)
                val f2 = Feature(name = "M3 Center Hole", type = FeatureType.HOLE)
                f2.numericParams["diameter"] = 3.4
                f2.numericParams["depth"] = 12.0
                p.features.add(f2)
                ParametricKernel.recompute(p)
                p
            }
        }
    }
}
