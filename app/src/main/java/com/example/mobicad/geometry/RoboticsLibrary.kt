package com.example.mobicad.geometry

import com.example.mobicad.model.*
import kotlin.math.PI

object RoboticsLibrary {

    data class ComponentDef(
        val id: String,
        val nameEn: String,
        val nameAr: String,
        val descriptionEn: String,
        val descriptionAr: String,
        val defaultParams: Map<String, Double>
    )

    val CATALOG = listOf(
        ComponentDef(
            "ESP32",
            "ESP32 DevKit Mount",
            "حامل شريحة ESP32",
            "Standard ESP32 30-pin board bracket with 4x M2.5 mounting holes and USB clearance",
            "قاعدة تثبيت لشريحة ESP32 مع 4 ثقوب M2.5 وفتحة كابل USB",
            mapOf("width" to 54.0, "length" to 28.5, "wall" to 2.5, "height" to 8.0)
        ),
        ComponentDef(
            "ARDUINO_UNO",
            "Arduino Uno R3 Mounting Base",
            "قاعدة تثبيت آردوينو أونو",
            "Accurate 68.6 x 53.4 mm mounting footprint with official 4-hole coordinates",
            "قاعدة آردوينو أونو بالأبعاد الأصلية مع ثقوب التثبيت الأربعة",
            mapOf("thickness" to 3.0, "border" to 4.0)
        ),
        ComponentDef(
            "NEMA17",
            "NEMA 17 Stepper Motor Bracket",
            "حامل محرك خطوي NEMA 17",
            "42.3 mm faceplate, 31 mm M3 bolt pattern, 22 mm center collar",
            "لوحة تثبيت NEMA 17 مع ثقوب M3 بتباعد 31 مم وفتحة جلبة 22 مم",
            mapOf("thickness" to 4.0, "faceWidth" to 42.3)
        ),
        ComponentDef(
            "SERVO_SG90",
            "SG90 Micro Servo Bracket",
            "حامل سيرفو SG90",
            "23 x 12.5 mm pocket for standard 9g micro servos with side mounting ears",
            "حامل لسيرفو 9 جرام الصغير مع آذان تثبيت بمسامير M2",
            mapOf("wall" to 2.0, "depth" to 16.0)
        ),
        ComponentDef(
            "BATTERY_18650",
            "18650 Dual Cell Battery Holder",
            "حاوية بطاريتين 18650 ليثيوم",
            "Holds two 18x65 mm cylindrical Li-ion cells with terminal channels",
            "علبة لخلbrowsتين 18650 ليثيوم مع قنوات التوصيل",
            mapOf("wall" to 2.0, "height" to 20.0)
        ),
        ComponentDef(
            "BEARING_608",
            "608 Skateboard/Roller Bearing Seat",
            "مقعد رولمان بلي 608",
            "22 mm outer diameter, 7 mm depth, 8 mm through-hole with retention rim",
            "تجويف رولمان بلي 608 (قطر خارجي 22 مم، عمق 7 مم، ثقب عمود 8 مم)",
            mapOf("wall" to 3.0, "depth" to 8.0)
        ),
        ComponentDef(
            "CHASSIS_PLATE",
            "Differential Drive Robot Chassis",
            "لوحة هيكل روبوت بعجلات",
            "120 x 90 mm main chassis with TT motor cutouts, caster mount, and wire slots",
            "لوحة هيكل روبوت رئيسية مع قنوات المحركات وثقوب تمرير الأسلاك",
            mapOf("length" to 120.0, "width" to 90.0, "thickness" to 3.0)
        ),
        ComponentDef(
            "HEX_STANDOFF",
            "M3 Hex Standoff Spacer",
            "ركيزة مسدس M3 مباعدة",
            "M3 metric internal hole, 5.5 mm hex across flats",
            "ركيزة مباعدة سداسية M3 بارتفاع قابل للتعديل",
            mapOf("height" to 15.0, "diameter" to 5.5, "holeDia" to 3.2)
        ),
        ComponentDef(
            "BRACKET_L",
            "90° Stiffened L-Bracket",
            "زاوية تثبيت 90 درجة مع دعامة",
            "Rigid corner bracket with stiffening rib and 4x M4 screw holes",
            "زاوية تثبيت صلبة 90 درجة مع عصب تقوية و4 ثقوب مسامير M4",
            mapOf("width" to 25.0, "legLength" to 25.0, "thickness" to 3.0)
        )
    )

    fun generateComponent(type: String, params: Map<String, Double>): Mesh3D {
        return when (type) {
            "ESP32" -> generateEsp32Mount(params)
            "ARDUINO_UNO" -> generateArduinoMount(params)
            "NEMA17" -> generateNema17Bracket(params)
            "SERVO_SG90" -> generateServoBracket(params)
            "BATTERY_18650" -> generateBatteryCase(params)
            "BEARING_608" -> generateBearingSeat(params)
            "CHASSIS_PLATE" -> generateChassisPlate(params)
            "HEX_STANDOFF" -> generateHexStandoff(params)
            "BRACKET_L" -> generateLBracket(params)
            else -> ParametricKernel.createBox(-15.0, -15.0, 0.0, 15.0, 15.0, 10.0)
        }
    }

    private fun generateEsp32Mount(p: Map<String, Double>): Mesh3D {
        val w = p["width"] ?: 54.0
        val l = p["length"] ?: 28.5
        val h = p["height"] ?: 8.0
        val t = p["wall"] ?: 2.5

        val outer = ParametricKernel.createBox(-w / 2.0 - t, -l / 2.0 - t, 0.0, w / 2.0 + t, l / 2.0 + t, h)
        val innerPocket = ParametricKernel.createBox(-w / 2.0, -l / 2.0, t, w / 2.0, l / 2.0, h + 1.0)
        val hollow = ParametricKernel.booleanSubtract(outer, innerPocket)

        // 4 corner screw holes M2.5 (2.8mm dia)
        val h1 = ParametricKernel.createCylinder(1.4, h + 2.0, -w / 2.0 + 3.0, -l / 2.0 + 3.0, -1.0)
        val h2 = ParametricKernel.createCylinder(1.4, h + 2.0, w / 2.0 - 3.0, -l / 2.0 + 3.0, -1.0)
        val h3 = ParametricKernel.createCylinder(1.4, h + 2.0, -w / 2.0 + 3.0, l / 2.0 - 3.0, -1.0)
        val h4 = ParametricKernel.createCylinder(1.4, h + 2.0, w / 2.0 - 3.0, l / 2.0 - 3.0, -1.0)

        var res = ParametricKernel.booleanSubtract(hollow, h1)
        res = ParametricKernel.booleanSubtract(res, h2)
        res = ParametricKernel.booleanSubtract(res, h3)
        res = ParametricKernel.booleanSubtract(res, h4)

        // USB cutout at one end
        val usbCut = ParametricKernel.createBox(-w / 2.0 - t - 1.0, -6.0, t, -w / 2.0 + 2.0, 6.0, h + 1.0)
        return ParametricKernel.booleanSubtract(res, usbCut).copy(name = "ESP32 DevKit Mount")
    }

    private fun generateArduinoMount(p: Map<String, Double>): Mesh3D {
        val t = p["thickness"] ?: 3.0
        val l = 68.6
        val w = 53.4

        val base = ParametricKernel.createBox(0.0, 0.0, 0.0, l, w, t)
        // Standard Arduino Uno screw holes (M3: 3.4mm dia)
        // Offsets: (14.0, 2.5), (65.0, 17.8), (65.0, 45.7), (15.2, 50.8)
        val h1 = ParametricKernel.createCylinder(1.7, t + 2.0, 14.0, 2.5, -1.0)
        val h2 = ParametricKernel.createCylinder(1.7, t + 2.0, 65.0, 17.8, -1.0)
        val h3 = ParametricKernel.createCylinder(1.7, t + 2.0, 65.0, 45.7, -1.0)
        val h4 = ParametricKernel.createCylinder(1.7, t + 2.0, 15.2, 50.8, -1.0)

        var res = ParametricKernel.booleanSubtract(base, h1)
        res = ParametricKernel.booleanSubtract(res, h2)
        res = ParametricKernel.booleanSubtract(res, h3)
        return ParametricKernel.booleanSubtract(res, h4).copy(name = "Arduino Uno Mount")
    }

    private fun generateNema17Bracket(p: Map<String, Double>): Mesh3D {
        val t = p["thickness"] ?: 4.0
        val size = p["faceWidth"] ?: 42.3
        val half = size / 2.0

        val plate = ParametricKernel.createBox(-half, -half, 0.0, half, half, t)
        // Center collar hole 22mm dia (radius 11mm)
        val centerHole = ParametricKernel.createCylinder(11.0, t + 2.0, 0.0, 0.0, -1.0)
        var res = ParametricKernel.booleanSubtract(plate, centerHole)

        // 4 M3 holes at 31.04 mm spacing (15.52 mm from center)
        val d = 15.52
        val h1 = ParametricKernel.createCylinder(1.7, t + 2.0, -d, -d, -1.0)
        val h2 = ParametricKernel.createCylinder(1.7, t + 2.0, d, -d, -1.0)
        val h3 = ParametricKernel.createCylinder(1.7, t + 2.0, -d, d, -1.0)
        val h4 = ParametricKernel.createCylinder(1.7, t + 2.0, d, d, -1.0)

        res = ParametricKernel.booleanSubtract(res, h1)
        res = ParametricKernel.booleanSubtract(res, h2)
        res = ParametricKernel.booleanSubtract(res, h3)
        return ParametricKernel.booleanSubtract(res, h4).copy(name = "NEMA 17 Bracket")
    }

    private fun generateServoBracket(p: Map<String, Double>): Mesh3D {
        val wall = p["wall"] ?: 2.0
        val h = p["depth"] ?: 16.0
        // SG90 body: 23.0 x 12.5 mm
        val bodyL = 23.0; val bodyW = 12.5

        val outer = ParametricKernel.createBox(-bodyL / 2.0 - wall - 6.0, -bodyW / 2.0 - wall, 0.0, bodyL / 2.0 + wall + 6.0, bodyW / 2.0 + wall, h)
        val cutout = ParametricKernel.createBox(-bodyL / 2.0, -bodyW / 2.0, -1.0, bodyL / 2.0, bodyW / 2.0, h + 2.0)
        var res = ParametricKernel.booleanSubtract(outer, cutout)

        // Mounting holes on tabs for M2 (2.2mm dia)
        val tabHole1 = ParametricKernel.createCylinder(1.1, h + 2.0, -bodyL / 2.0 - 4.0, 0.0, -1.0)
        val tabHole2 = ParametricKernel.createCylinder(1.1, h + 2.0, bodyL / 2.0 + 4.0, 0.0, -1.0)
        res = ParametricKernel.booleanSubtract(res, tabHole1)
        return ParametricKernel.booleanSubtract(res, tabHole2).copy(name = "SG90 Servo Bracket")
    }

    private fun generateBatteryCase(p: Map<String, Double>): Mesh3D {
        val wall = p["wall"] ?: 2.0
        val cellR = 9.2 // 18.4mm diameter for 18650
        val cellL = 66.0
        val h = p["height"] ?: 20.0

        val totalW = (cellR * 4.0) + (wall * 3.0)
        val totalL = cellL + (wall * 2.0)

        val outer = ParametricKernel.createBox(-totalW / 2.0, -totalL / 2.0, 0.0, totalW / 2.0, totalL / 2.0, h)
        // Two cylindrical battery slots
        val slot1 = ParametricKernel.createCylinder(cellR, cellL, -cellR - wall / 2.0, 0.0, wall, SketchPlane.XZ_FRONT)
        val slot2 = ParametricKernel.createCylinder(cellR, cellL, cellR + wall / 2.0, 0.0, wall, SketchPlane.XZ_FRONT)

        var res = ParametricKernel.booleanSubtract(outer, slot1)
        return ParametricKernel.booleanSubtract(res, slot2).copy(name = "18650 Battery Case")
    }

    private fun generateBearingSeat(p: Map<String, Double>): Mesh3D {
        val wall = p["wall"] ?: 3.0
        val outerDia = 22.0 // 608 outer
        val innerDia = 8.0  // 608 bore
        val depth = p["depth"] ?: 8.0
        val seatR = outerDia / 2.0
        val boreR = innerDia / 2.0
        val totalR = seatR + wall

        val outerCyl = ParametricKernel.createCylinder(totalR, depth, 0.0, 0.0, 0.0)
        val pocket = ParametricKernel.createCylinder(seatR, 7.0, 0.0, 0.0, depth - 7.0)
        val bore = ParametricKernel.createCylinder(boreR, depth + 2.0, 0.0, 0.0, -1.0)

        var res = ParametricKernel.booleanSubtract(outerCyl, pocket)
        return ParametricKernel.booleanSubtract(res, bore).copy(name = "608 Bearing Seat")
    }

    private fun generateChassisPlate(p: Map<String, Double>): Mesh3D {
        val l = p["length"] ?: 120.0
        val w = p["width"] ?: 90.0
        val t = p["thickness"] ?: 3.0

        val plate = ParametricKernel.createBox(-l / 2.0, -w / 2.0, 0.0, l / 2.0, w / 2.0, t)

        // Caster wheel mount holes (M3)
        val c1 = ParametricKernel.createCylinder(1.6, t + 2.0, -l / 2.0 + 15.0, -10.0, -1.0)
        val c2 = ParametricKernel.createCylinder(1.6, t + 2.0, -l / 2.0 + 15.0, 10.0, -1.0)
        // TT motor pass-through wire slots
        val slot1 = ParametricKernel.createBox(10.0, -w / 2.0 + 8.0, -1.0, 30.0, -w / 2.0 + 14.0, t + 2.0)
        val slot2 = ParametricKernel.createBox(10.0, w / 2.0 - 14.0, -1.0, 30.0, w / 2.0 - 8.0, t + 2.0)

        var res = ParametricKernel.booleanSubtract(plate, c1)
        res = ParametricKernel.booleanSubtract(res, c2)
        res = ParametricKernel.booleanSubtract(res, slot1)
        return ParametricKernel.booleanSubtract(res, slot2).copy(name = "Differential Chassis")
    }

    private fun generateHexStandoff(p: Map<String, Double>): Mesh3D {
        val h = p["height"] ?: 15.0
        val dia = p["diameter"] ?: 5.5
        val holeDia = p["holeDia"] ?: 3.2
        val r = dia / (2.0 * kotlin.math.cos(PI / 6.0))

        val hexPrism = ParametricKernel.createPolygonPrism(0.0, 0.0, r, 6, h, 0.0, SketchPlane.XY_TOP)
        val centerHole = ParametricKernel.createCylinder(holeDia / 2.0, h + 2.0, 0.0, 0.0, -1.0)
        return ParametricKernel.booleanSubtract(hexPrism, centerHole).copy(name = "M3 Hex Standoff")
    }

    private fun generateLBracket(p: Map<String, Double>): Mesh3D {
        val w = p["width"] ?: 25.0
        val l = p["legLength"] ?: 25.0
        val t = p["thickness"] ?: 3.0

        // Horizontal leg
        val legH = ParametricKernel.createBox(0.0, -w / 2.0, 0.0, l, w / 2.0, t)
        // Vertical leg
        val legV = ParametricKernel.createBox(0.0, -w / 2.0, 0.0, t, w / 2.0, l)
        var bracket = ParametricKernel.booleanUnion(legH, legV)

        // 2 holes on horizontal leg
        val h1 = ParametricKernel.createCylinder(2.2, t + 2.0, l / 2.0, 0.0, -1.0)
        // 2 holes on vertical leg
        val h2 = ParametricKernel.createCylinder(2.2, t + 2.0, -1.0, 0.0, l / 2.0, SketchPlane.YZ_RIGHT)

        bracket = ParametricKernel.booleanSubtract(bracket, h1)
        return ParametricKernel.booleanSubtract(bracket, h2).copy(name = "L-Bracket 90°")
    }
}
