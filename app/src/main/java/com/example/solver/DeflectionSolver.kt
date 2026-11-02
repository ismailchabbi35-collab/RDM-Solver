package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

object DeflectionSolver {

    private const val NUM_INTERVALS = 600

    /**
     * Compute deflection v(x) in mm and rotation theta(x) in mrad across the beam.
     */
    fun computeDeflection(
        structure: BeamStructure,
        segments: List<SegmentEquation>,
        props: SectionProperties,
        material: Material
    ): DeflectionResult {
        val beamLength = structure.length
        val supports = structure.supports.sortedBy { it.position }

        // Bending stiffness EI in kN·m²:
        // E in GPa = E * 1e6 kN/m²
        // Iz in cm⁴ = Iz * 1e-8 m⁴
        // EI = E * 1e6 * Iz * 1e-8 = E * Iz * 1e-2 kN·m²
        val ei = material.youngModulusE_GPa * props.inertiaIz_cm4 * 0.01

        if (ei <= 1e-6 || segments.isEmpty()) {
            return DeflectionResult(
                maxDeflection_mm = 0.0,
                maxDeflectionX_m = 0.0,
                maxRotation_mrad = 0.0,
                maxRotationX_m = 0.0,
                deflectionLimit_mm = (beamLength * 1000.0) / 500.0,
                limitRatioDenominator = 500,
                isElsCompliant = true,
                elsUtilizationRatio = 0.0,
                deflectionPoints = emptyList(),
                rotationPoints = emptyList()
            )
        }

        // Helper to evaluate M(x) at any point along the beam
        fun evalM(x: Double): Double {
            val clampedX = x.coerceIn(0.0, beamLength)
            val seg = segments.firstOrNull { clampedX >= it.xStart - 1e-5 && clampedX <= it.xEnd + 1e-5 }
                ?: segments.last()
            return seg.mFunc(clampedX)
        }

        // Simpson composite integration for I1(x) = int_0^x M(u)/EI du
        // and I2(x) = int_0^x (x - u) * M(u)/EI du
        fun computeI1AndI2(x: Double): Pair<Double, Double> {
            if (x <= 1e-7) return Pair(0.0, 0.0)
            val n = 200 // even number
            val h = x / n
            var sumI1 = evalM(0.0) / ei + evalM(x) / ei
            var sumI2 = 0.0 // at u=0: (x - 0)*M(0)/ei, at u=x: (x - x)*M(x)/ei = 0
            sumI2 += x * (evalM(0.0) / ei)

            for (i in 1 until n) {
                val u = i * h
                val mVal = evalM(u) / ei
                val weight = if (i % 2 == 1) 4.0 else 2.0
                sumI1 += weight * mVal
                sumI2 += weight * (x - u) * mVal
            }
            val i1 = (h / 3.0) * sumI1
            val i2 = (h / 3.0) * sumI2
            return Pair(i1, i2)
        }

        // Solve integration constants theta0 and v0 using boundary conditions:
        var theta0 = 0.0
        var v0 = 0.0

        if (supports.size == 1 && supports[0].type == SupportType.FIXED) {
            val sup = supports[0]
            val xA = sup.position
            // Boundary condition: v(xA) = 0 and theta(xA) = 0
            val (i1A, i2A) = computeI1AndI2(xA)
            theta0 = -i1A
            v0 = -i2A - theta0 * xA
        } else if (supports.size >= 2) {
            val xA = supports[0].position
            val xB = supports[1].position
            val span = xB - xA
            if (abs(span) > 1e-5) {
                val (_, i2A) = computeI1AndI2(xA)
                val (_, i2B) = computeI1AndI2(xB)
                // v(xA) = i2A + theta0 * xA + v0 = 0
                // v(xB) = i2B + theta0 * xB + v0 = 0
                // theta0 * (xB - xA) + (i2B - i2A) = 0
                theta0 = -(i2B - i2A) / span
                v0 = -i2A - theta0 * xA
            }
        }

        // Sample deflection v(x) and rotation theta(x) across 200 points
        val numSamples = 200
        val deflectionPoints = mutableListOf<DiagramPoint>()
        val rotationPoints = mutableListOf<DiagramPoint>()

        var maxDeflectionMm = 0.0
        var maxDeflectionX = 0.0
        var maxRotationMrad = 0.0
        var maxRotationX = 0.0

        for (s in 0..numSamples) {
            val x = (s.toDouble() / numSamples) * beamLength
            val (i1, i2) = computeI1AndI2(x)
            // v(x) in meters, upward positive
            val vMeters = i2 + theta0 * x + v0
            // Rotation theta in radians
            val thetaRad = i1 + theta0

            // Downward deflection f = -v in mm:
            val fMm = -vMeters * 1000.0
            val thetaMrad = thetaRad * 1000.0

            deflectionPoints.add(DiagramPoint(x, fMm))
            rotationPoints.add(DiagramPoint(x, thetaMrad))

            if (abs(fMm) > abs(maxDeflectionMm)) {
                maxDeflectionMm = fMm
                maxDeflectionX = x
            }
            if (abs(thetaMrad) > abs(maxRotationMrad)) {
                maxRotationMrad = thetaMrad
                maxRotationX = x
            }
        }

        val limitRatio = 500
        val deflectionLimitMm = (beamLength * 1000.0) / limitRatio
        val isCompliant = abs(maxDeflectionMm) <= deflectionLimitMm
        val utilization = if (deflectionLimitMm > 1e-5) abs(maxDeflectionMm) / deflectionLimitMm else 0.0

        return DeflectionResult(
            maxDeflection_mm = maxDeflectionMm,
            maxDeflectionX_m = maxDeflectionX,
            maxRotation_mrad = maxRotationMrad,
            maxRotationX_m = maxRotationX,
            deflectionLimit_mm = deflectionLimitMm,
            limitRatioDenominator = limitRatio,
            isElsCompliant = isCompliant,
            elsUtilizationRatio = utilization,
            deflectionPoints = deflectionPoints,
            rotationPoints = rotationPoints
        )
    }

    /**
     * Generate step-by-step resolution cards for Deflection (Phase 3).
     */
    fun generateDeflectionSteps(
        structure: BeamStructure,
        props: SectionProperties,
        material: Material,
        deflection: DeflectionResult,
        stepOffset: Int
    ): List<StepItem> {
        val steps = mutableListOf<StepItem>()
        val ei = material.youngModulusE_GPa * props.inertiaIz_cm4 * 0.01

        // Step 1: Differential equation of the elastic line
        steps.add(
            StepItem(
                stepNumber = stepOffset + 1,
                title = "Équation différentielle de la ligne élastique",
                formula = "E I_z \\cdot \\frac{d^2 v(x)}{dx^2} = M(x), \\quad E I_z = %.2f \\text{ kN}\\cdot\\text{m}^2".format(Locale.US, ei),
                substitution = "E = %.1f \\text{ GPa} = %.2e \\text{ kN/m}^2, \\quad I_z = %.2f \\text{ cm}^4 = %.2e \\text{ m}^4\nE I_z = %.2e \\times %.2e = %.2f \\text{ kN}\\cdot\\text{m}^2".format(
                    Locale.US, material.youngModulusE_GPa, material.youngModulusE_GPa * 1e6, props.inertiaIz_cm4, props.inertiaIz_cm4 * 1e-8,
                    material.youngModulusE_GPa * 1e6, props.inertiaIz_cm4 * 1e-8, ei
                ),
                result = "E I_z = %.2f".format(Locale.US, ei),
                unit = "kN·m²",
                details = "La rigidité flexionnelle E·Iz régit l'amplitude de la courbure 1/R = M/(EI) sous les charges appliquées."
            )
        )

        // Step 2: Double integration and boundary conditions
        val bCondText = if (structure.supports.size == 1 && structure.supports[0].type == SupportType.FIXED) {
            "Encastrement en x = 0 m : v(0) = 0, \\theta(0) = 0 \\implies C_1 = 0, C_2 = 0"
        } else {
            val supA = structure.supports.getOrNull(0)?.position ?: 0.0
            val supB = structure.supports.getOrNull(1)?.position ?: structure.length
            "Appuis simples : v(%.2f) = 0, \\quad v(%.2f) = 0 \\implies \\text{Résolution de } C_1, C_2".format(Locale.US, supA, supB)
        }
        steps.add(
            StepItem(
                stepNumber = stepOffset + 2,
                title = "Double intégration et conditions aux limites",
                formula = "\\theta(x) = \\int \\frac{M(x)}{E I_z} dx + C_1, \\quad v(x) = \\int \\theta(x) dx + C_2",
                substitution = bCondText,
                result = "\\theta_{\\max} = %.2f \\text{ mrad à x = %.2f m}".format(Locale.US, deflection.maxRotation_mrad, deflection.maxRotationX_m),
                unit = "mrad",
                details = "Les constantes d'intégration C1 et C2 sont déterminées de manière univoque par l'annulation des déplacements aux appuis."
            )
        )

        // Step 3: Maximum Deflection & ELS Verification
        val elsStatus = if (deflection.isElsCompliant) "CONFORME (Flèche admissible respectée)" else "NON CONFORME (Flèche excessive)"
        steps.add(
            StepItem(
                stepNumber = stepOffset + 3,
                title = "Flèche maximale et vérification aux états limites de service (ELS)",
                formula = "f_{\\max} = |v(x)|_{\\max} \\le f_{\\text{lim}} = \\frac{L}{%d}".format(deflection.limitRatioDenominator),
                substitution = "f_{\\max} = %.2f \\text{ mm en x = %.2f m}\nf_{\\text{lim}} = \\frac{%.2f \\times 1000}{%d} = %.2f \\text{ mm}".format(
                    Locale.US, abs(deflection.maxDeflection_mm), deflection.maxDeflectionX_m, structure.length, deflection.limitRatioDenominator, deflection.deflectionLimit_mm
                ),
                result = "f_{\\max} = %.2f \\text{ mm} \\le %.2f \\text{ mm} \\quad (\\eta_{\\text{ELS}} = %.1f\\%%) \\to %s".format(
                    Locale.US, abs(deflection.maxDeflection_mm), deflection.deflectionLimit_mm, deflection.elsUtilizationRatio * 100.0, elsStatus
                ),
                unit = "mm / %",
                details = "Critère Eurocode standard L/500 garantissant le confort d'usage et l'absence de fissuration des cloisons ou revêtements."
            )
        )

        return steps
    }
}
