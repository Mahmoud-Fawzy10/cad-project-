package com.example.mobicad.geometry

import com.example.mobicad.model.*
import java.util.UUID

data class CodeExecResult(
    val isSuccess: Boolean,
    val message: String,
    val errorLine: Int? = null,
    val featuresCount: Int = 0,
    val executionTimeMs: Long = 0
)

object CodeCadEngine {

    val TEMPLATES = listOf(
        Pair(
            "ESP32 Enclosure with M3 Holes",
            """// ESP32 Electronics Enclosure
material("PLA")
box(width=65.0, length=45.0, height=22.0, x=0, y=0, z=0)
shell(thickness=2.5)
hole(diameter=3.4, depth=15.0, x=26.0, y=16.0, z=0)
hole(diameter=3.4, depth=15.0, x=-26.0, y=16.0, z=0)
hole(diameter=3.4, depth=15.0, x=26.0, y=-16.0, z=0)
hole(diameter=3.4, depth=15.0, x=-26.0, y=-16.0, z=0)
fillet(radius=2.0)
"""
        ),
        Pair(
            "Robotics Chassis & NEMA 17 Mount",
            """// Robotics Chassis Base with Stepper Mount
material("ALUMINUM_6061")
robotics(component="CHASSIS_PLATE")
robotics(component="NEMA17")
robotics(component="BEARING_608")
hole(diameter=4.5, depth=10.0, x=20.0, y=0.0, z=0)
hole(diameter=4.5, depth=10.0, x=-20.0, y=0.0, z=0)
"""
        ),
        Pair(
            "Precision Motor Shaft Coupler",
            """// Cylindrical Shaft Coupler with Set-Screw Holes
material("ALUMINUM_6061")
cylinder(radius=12.0, height=35.0, x=0, y=0, z=0)
hole(diameter=5.0, depth=35.0, x=0, y=0, z=0)
hole(diameter=3.0, depth=10.0, x=0, y=0, z=10.0)
hole(diameter=3.0, depth=10.0, x=0, y=0, z=25.0)
chamfer(distance=1.0)
"""
        ),
        Pair(
            "High-Strength 90° Corner Bracket",
            """// Heavy-Duty Stiffened Corner Bracket
material("STEEL_AISI_1020")
robotics(component="BRACKET_L")
hole(diameter=4.2, depth=8.0, x=8.0, y=8.0, z=0)
hole(diameter=4.2, depth=8.0, x=-8.0, y=8.0, z=0)
fillet(radius=1.5)
"""
        ),
        Pair(
            "Dual 18650 Battery Pack Case",
            """// Dual 18650 Battery Pack Housing
material("PETG")
robotics(component="BATTERY_18650")
box(width=44.0, length=72.0, height=22.0, x=0, y=0, z=0)
shell(thickness=2.0)
hole(diameter=3.2, depth=10.0, x=18.0, y=30.0, z=0)
hole(diameter=3.2, depth=10.0, x=-18.0, y=30.0, z=0)
"""
        ),
        Pair(
            "Parametric Flanged Bushing",
            """// Flanged Bushing with Central Bore
material("BRASS")
cylinder(radius=10.0, height=25.0, x=0, y=0, z=0)
cylinder(radius=16.0, height=4.0, x=0, y=0, z=0)
hole(diameter=10.0, depth=30.0, x=0, y=0, z=0)
chamfer(distance=1.0)
"""
        )
    )

    /**
     * Executes a CAD script string against the given project.
     * Replaces features and recomputes the 3D solid geometry.
     */
    fun parseAndExecute(script: String, project: CadProject): CodeExecResult {
        val startTime = System.currentTimeMillis()
        val lines = script.lines()
        val variables = mutableMapOf<String, Double>()
        val newFeatures = mutableListOf<Feature>()
        var newMaterial = project.material

        for ((idx, rawLine) in lines.withIndex()) {
            val lineNumber = idx + 1
            var line = rawLine.trim()

            // Strip comments
            if (line.startsWith("//") || line.startsWith("#") || line.isEmpty()) {
                continue
            }
            val commentIdx = line.indexOf("//")
            if (commentIdx != -1) {
                line = line.substring(0, commentIdx).trim()
            }

            // Variable assignment: width = 50.0
            if (line.contains("=") && !line.contains("(") && !line.contains(")")) {
                val parts = line.split("=").map { it.trim() }
                if (parts.size == 2) {
                    val varName = parts[0]
                    val varVal = evaluateExpression(parts[1], variables)
                    if (varVal != null) {
                        variables[varName] = varVal
                        continue
                    } else {
                        return CodeExecResult(
                            false,
                            "Line $lineNumber: Invalid variable value '${parts[1]}'",
                            lineNumber
                        )
                    }
                }
            }

            // Function call: funcName(args...)
            val openParen = line.indexOf('(')
            val closeParen = line.lastIndexOf(')')
            if (openParen == -1 || closeParen == -1 || closeParen < openParen) {
                return CodeExecResult(
                    false,
                    "Line $lineNumber: Syntax error in command '$line'",
                    lineNumber
                )
            }

            val funcName = line.substring(0, openParen).trim().lowercase()
            val argsStr = line.substring(openParen + 1, closeParen).trim()
            val args = parseArguments(argsStr, variables)

            try {
                when (funcName) {
                    "material" -> {
                        val matName = extractStringLiteral(argsStr) ?: args.firstOrNull()?.second?.toString() ?: "PLA"
                        newMaterial = when {
                            matName.contains("ALUM", ignoreCase = true) -> Material.ALUMINUM_6061
                            matName.contains("STEEL", ignoreCase = true) -> Material.STEEL_1018
                            matName.contains("BRASS", ignoreCase = true) -> Material.BRASS_C360
                            matName.contains("ABS", ignoreCase = true) -> Material.ABS
                            matName.contains("PETG", ignoreCase = true) -> Material.PETG
                            matName.contains("CARBON", ignoreCase = true) -> Material.CARBON_FIBER
                            matName.contains("DELRIN", ignoreCase = true) || matName.contains("POM", ignoreCase = true) -> Material.DELRIN_POM
                            matName.contains("ACRYL", ignoreCase = true) -> Material.ACRYLIC
                            else -> Material.PLA
                        }
                    }

                    "box", "cube" -> {
                        val w = getArgDouble(args, listOf("width", "w", "x_size"), 0, 30.0)
                        val l = getArgDouble(args, listOf("length", "l", "depth", "y_size"), 1, 30.0)
                        val h = getArgDouble(args, listOf("height", "h", "thickness", "z_size"), 2, 20.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 3, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 4, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 5, 0.0)

                        val feat = Feature(
                            name = "Code Box [${w}×${l}×${h}]",
                            type = FeatureType.PRIMITIVE_BOX
                        )
                        feat.numericParams["width"] = w
                        feat.numericParams["length"] = l
                        feat.numericParams["height"] = h
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "cylinder", "cyl" -> {
                        val r = getArgDouble(args, listOf("radius", "r"), 0, null)
                            ?: (getArgDouble(args, listOf("diameter", "dia", "d"), 0, 30.0) / 2.0)
                        val h = getArgDouble(args, listOf("height", "h", "length"), 1, 30.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 2, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 3, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 4, 0.0)

                        val feat = Feature(
                            name = "Code Cylinder [r=$r, h=$h]",
                            type = FeatureType.PRIMITIVE_CYLINDER
                        )
                        feat.numericParams["radius"] = r
                        feat.numericParams["height"] = h
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "sphere" -> {
                        val r = getArgDouble(args, listOf("radius", "r"), 0, 15.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 1, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 2, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 3, 0.0)

                        val feat = Feature(
                            name = "Code Sphere [r=$r]",
                            type = FeatureType.PRIMITIVE_SPHERE
                        )
                        feat.numericParams["radius"] = r
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "cone" -> {
                        val r = getArgDouble(args, listOf("radius", "r"), 0, 15.0)
                        val h = getArgDouble(args, listOf("height", "h"), 1, 30.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 2, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 3, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 4, 0.0)

                        val feat = Feature(
                            name = "Code Cone [r=$r, h=$h]",
                            type = FeatureType.PRIMITIVE_CONE
                        )
                        feat.numericParams["radius"] = r
                        feat.numericParams["height"] = h
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "torus" -> {
                        val majorR = getArgDouble(args, listOf("major", "majorradius", "r1"), 0, 20.0)
                        val minorR = getArgDouble(args, listOf("minor", "minorradius", "r2"), 1, 5.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 2, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 3, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 4, 0.0)

                        val feat = Feature(
                            name = "Code Torus [R=$majorR, r=$minorR]",
                            type = FeatureType.PRIMITIVE_TORUS
                        )
                        feat.numericParams["majorRadius"] = majorR
                        feat.numericParams["minorRadius"] = minorR
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "hole" -> {
                        val dia = getArgDouble(args, listOf("diameter", "dia", "d"), 0, null)
                            ?: ((getArgDouble(args, listOf("radius", "r"), 0, 1.7)) * 2.0)
                        val depth = getArgDouble(args, listOf("depth", "h", "length"), 1, 15.0)
                        val cx = getArgDouble(args, listOf("x", "cx"), 2, 0.0)
                        val cy = getArgDouble(args, listOf("y", "cy"), 3, 0.0)
                        val cz = getArgDouble(args, listOf("z", "cz"), 4, 0.0)

                        val feat = Feature(
                            name = "Code Hole [Ø$dia at ($cx,$cy)]",
                            type = FeatureType.HOLE
                        )
                        feat.numericParams["diameter"] = dia
                        feat.numericParams["depth"] = depth
                        feat.numericParams["x"] = cx
                        feat.numericParams["y"] = cy
                        feat.numericParams["z"] = cz
                        newFeatures.add(feat)
                    }

                    "fillet" -> {
                        val r = getArgDouble(args, listOf("radius", "r"), 0, 2.0)
                        val feat = Feature(name = "Code Fillet [r=$r]", type = FeatureType.FILLET)
                        feat.numericParams["radius"] = r
                        newFeatures.add(feat)
                    }

                    "chamfer" -> {
                        val d = getArgDouble(args, listOf("distance", "d", "dist"), 0, 1.5)
                        val feat = Feature(name = "Code Chamfer [d=$d]", type = FeatureType.CHAMFER)
                        feat.numericParams["distance"] = d
                        newFeatures.add(feat)
                    }

                    "shell" -> {
                        val t = getArgDouble(args, listOf("thickness", "t", "wall"), 0, 2.0)
                        val feat = Feature(name = "Code Shell [t=$t]", type = FeatureType.SHELL)
                        feat.numericParams["thickness"] = t
                        newFeatures.add(feat)
                    }

                    "linear_pattern", "pattern_linear" -> {
                        val count = getArgDouble(args, listOf("count", "n"), 0, 3.0)
                        val spacing = getArgDouble(args, listOf("spacing", "s", "step"), 1, 20.0)
                        val feat = Feature(name = "Code Linear Pattern [n=$count]", type = FeatureType.LINEAR_PATTERN)
                        feat.numericParams["count"] = count
                        feat.numericParams["spacing"] = spacing
                        newFeatures.add(feat)
                    }

                    "circular_pattern", "pattern_circular" -> {
                        val count = getArgDouble(args, listOf("count", "n"), 0, 4.0)
                        val angle = getArgDouble(args, listOf("angle", "a"), 1, 360.0)
                        val feat = Feature(name = "Code Circular Pattern [n=$count]", type = FeatureType.CIRCULAR_PATTERN)
                        feat.numericParams["count"] = count
                        feat.numericParams["angle"] = angle
                        newFeatures.add(feat)
                    }

                    "robotics", "component" -> {
                        val compName = extractStringLiteral(argsStr)
                            ?: args.find { it.first == "component" || it.first == "name" || it.first == "type" }?.second?.toString()
                            ?: args.firstOrNull()?.second?.toString()
                            ?: "ESP32"

                        val compClean = compName.trim().replace("\"", "").replace("'", "").uppercase()
                        val feat = Feature(
                            name = "Robotics: $compClean",
                            type = FeatureType.ROBOTICS_COMPONENT
                        )
                        feat.stringParams["component"] = compClean

                        // Pass any custom numeric parameters
                        for ((k, v) in args) {
                            if (k != null && k != "component" && k != "name" && v is Double) {
                                feat.numericParams[k] = v
                            }
                        }
                        newFeatures.add(feat)
                    }

                    "extrude" -> {
                        val depth = getArgDouble(args, listOf("depth", "d", "height"), 0, 15.0)
                        val symmetric = getArgDouble(args, listOf("symmetric", "sym"), 1, 0.0)
                        val sketchId = project.sketches.lastOrNull()?.id
                        val feat = Feature(
                            name = "Code Extrude [d=$depth]",
                            type = FeatureType.EXTRUDE,
                            sketchId = sketchId
                        )
                        feat.numericParams["depth"] = depth
                        feat.numericParams["symmetric"] = symmetric
                        newFeatures.add(feat)
                    }

                    "revolve" -> {
                        val angle = getArgDouble(args, listOf("angle", "a"), 0, 360.0)
                        val sketchId = project.sketches.lastOrNull()?.id
                        val feat = Feature(
                            name = "Code Revolve [a=$angle°]",
                            type = FeatureType.REVOLVE,
                            sketchId = sketchId
                        )
                        feat.numericParams["angle"] = angle
                        newFeatures.add(feat)
                    }

                    else -> {
                        return CodeExecResult(
                            false,
                            "Line $lineNumber: Unknown CAD function '$funcName()'",
                            lineNumber
                        )
                    }
                }
            } catch (e: Exception) {
                return CodeExecResult(
                    false,
                    "Line $lineNumber: Error processing '$funcName': ${e.message}",
                    lineNumber
                )
            }
        }

        if (newFeatures.isEmpty()) {
            return CodeExecResult(
                false,
                "Script did not define any valid 3D operations or geometry features."
            )
        }

        // Apply features to project
        project.features.clear()
        project.features.addAll(newFeatures)
        project.material = newMaterial
        project.lastModifiedTimestamp = System.currentTimeMillis()

        // Recompute geometry solid
        ParametricKernel.recompute(project)

        val totalTime = System.currentTimeMillis() - startTime
        return CodeExecResult(
            isSuccess = true,
            message = "Successfully compiled ${newFeatures.size} features in ${totalTime}ms",
            featuresCount = newFeatures.size,
            executionTimeMs = totalTime
        )
    }

    /**
     * Converts a CadProject into clean, readable Code CAD script.
     */
    fun projectToScript(project: CadProject): String {
        val sb = StringBuilder()
        sb.append("// MobiCAD Pro Parametric Script - ${project.name}\n")
        sb.append("material(\"${project.material.name}\")\n\n")

        for (feat in project.features) {
            when (feat.type) {
                FeatureType.PRIMITIVE_BOX -> {
                    val w = feat.getParam("width", 30.0)
                    val l = feat.getParam("length", 30.0)
                    val h = feat.getParam("height", 20.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("box(width=$w, length=$l, height=$h, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.PRIMITIVE_CYLINDER -> {
                    val r = feat.getParam("radius", 15.0)
                    val h = feat.getParam("height", 30.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("cylinder(radius=$r, height=$h, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.PRIMITIVE_SPHERE -> {
                    val r = feat.getParam("radius", 15.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("sphere(radius=$r, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.PRIMITIVE_CONE -> {
                    val r = feat.getParam("radius", 15.0)
                    val h = feat.getParam("height", 30.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("cone(radius=$r, height=$h, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.PRIMITIVE_TORUS -> {
                    val r1 = feat.getParam("majorRadius", 20.0)
                    val r2 = feat.getParam("minorRadius", 5.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("torus(major=$r1, minor=$r2, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.HOLE -> {
                    val d = feat.getParam("diameter", 3.4)
                    val depth = feat.getParam("depth", 15.0)
                    val cx = feat.getParam("x", 0.0)
                    val cy = feat.getParam("y", 0.0)
                    val cz = feat.getParam("z", 0.0)
                    sb.append("hole(diameter=$d, depth=$depth, x=$cx, y=$cy, z=$cz)\n")
                }
                FeatureType.FILLET -> {
                    val r = feat.getParam("radius", 2.0)
                    sb.append("fillet(radius=$r)\n")
                }
                FeatureType.CHAMFER -> {
                    val d = feat.getParam("distance", 1.5)
                    sb.append("chamfer(distance=$d)\n")
                }
                FeatureType.SHELL -> {
                    val t = feat.getParam("thickness", 2.0)
                    sb.append("shell(thickness=$t)\n")
                }
                FeatureType.LINEAR_PATTERN -> {
                    val count = feat.getParam("count", 3.0)
                    val sp = feat.getParam("spacing", 20.0)
                    sb.append("linear_pattern(count=$count, spacing=$sp)\n")
                }
                FeatureType.CIRCULAR_PATTERN -> {
                    val count = feat.getParam("count", 4.0)
                    val angle = feat.getParam("angle", 360.0)
                    sb.append("circular_pattern(count=$count, angle=$angle)\n")
                }
                FeatureType.ROBOTICS_COMPONENT -> {
                    val comp = feat.getStringParam("component", "ESP32")
                    sb.append("robotics(component=\"$comp\")\n")
                }
                FeatureType.EXTRUDE -> {
                    val d = feat.getParam("depth", 15.0)
                    sb.append("extrude(depth=$d)\n")
                }
                FeatureType.REVOLVE -> {
                    val a = feat.getParam("angle", 360.0)
                    sb.append("revolve(angle=$a)\n")
                }
                else -> {
                    // Generic
                }
            }
        }

        if (project.features.isEmpty()) {
            sb.append("// Empty Project - Insert solid geometry:\n")
            sb.append("box(width=40.0, length=40.0, height=20.0)\n")
        }

        return sb.toString()
    }

    // Helper utilities
    private fun parseArguments(argsStr: String, vars: Map<String, Double>): List<Pair<String?, Any>> {
        if (argsStr.isBlank()) return emptyList()

        val result = mutableListOf<Pair<String?, Any>>()
        val tokens = splitRespectingQuotes(argsStr, ',')

        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.contains("=")) {
                val kv = trimmed.split("=").map { it.trim() }
                val k = kv[0]
                val vStr = kv[1]
                val stringLit = extractStringLiteral(vStr)
                if (stringLit != null) {
                    result.add(Pair(k, stringLit))
                } else {
                    val num = evaluateExpression(vStr, vars) ?: 0.0
                    result.add(Pair(k, num))
                }
            } else {
                val stringLit = extractStringLiteral(trimmed)
                if (stringLit != null) {
                    result.add(Pair(null, stringLit))
                } else {
                    val num = evaluateExpression(trimmed, vars) ?: 0.0
                    result.add(Pair(null, num))
                }
            }
        }
        return result
    }

    private fun getArgDouble(
        args: List<Pair<String?, Any>>,
        keys: List<String>,
        positionalIndex: Int,
        default: Double?
    ): Double {
        // Check named match
        for (arg in args) {
            if (arg.first != null && keys.contains(arg.first!!.lowercase())) {
                return (arg.second as? Double) ?: (arg.second.toString().toDoubleOrNull() ?: (default ?: 0.0))
            }
        }
        // Check positional match
        val positionalArgs = args.filter { it.first == null }
        if (positionalIndex < positionalArgs.size) {
            val v = positionalArgs[positionalIndex].second
            return (v as? Double) ?: (v.toString().toDoubleOrNull() ?: (default ?: 0.0))
        }

        return default ?: 0.0
    }

    private fun extractStringLiteral(s: String): String? {
        val trimmed = s.trim()
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))
        ) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        return null
    }

    private fun evaluateExpression(expr: String, vars: Map<String, Double>): Double? {
        val trimmed = expr.trim()
        // Direct number
        val direct = trimmed.toDoubleOrNull()
        if (direct != null) return direct

        // Variable lookup
        if (vars.containsKey(trimmed)) return vars[trimmed]

        // Simple arithmetic: a + b, a - b, a * b, a / b
        for (op in listOf("+", "-", "*", "/")) {
            val parts = trimmed.split(op)
            if (parts.size == 2) {
                val left = evaluateExpression(parts[0], vars) ?: continue
                val right = evaluateExpression(parts[1], vars) ?: continue
                return when (op) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> if (right != 0.0) left / right else 0.0
                    else -> null
                }
            }
        }
        return null
    }

    private fun splitRespectingQuotes(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        var quoteChar = ' '
        val sb = StringBuilder()

        for (ch in input) {
            if (!inQuotes && (ch == '"' || ch == '\'')) {
                inQuotes = true
                quoteChar = ch
                sb.append(ch)
            } else if (inQuotes && ch == quoteChar) {
                inQuotes = false
                sb.append(ch)
            } else if (!inQuotes && ch == delimiter) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }
}
