package com.example.solver

import com.example.data.*
import java.util.Locale

object RdmSolver {

    /**
     * Solves the given beam structure completely, returning reactions, piecewise equations,
     * diagrams, remarkable points, step-by-step resolution, and automated engineering checks.
     */
    fun solve(structure: BeamStructure): RdmSolution {
        // 1. Solve global reactions
        val eqResult = ReactionsSolver.solveReactions(structure)

        if (!eqResult.isMechanicallyStable || !eqResult.isDeterminate) {
            return RdmSolution(
                structure = structure,
                reactions = emptyList(),
                segments = emptyList(),
                diagramN = emptyList(),
                diagramT = emptyList(),
                diagramM = emptyList(),
                remarkablePoints = emptyList(),
                maxMoment = null,
                minMoment = null,
                maxShear = null,
                steps = eqResult.steps,
                checks = emptyList(),
                isDeterminate = eqResult.isDeterminate,
                isMechanicallyStable = eqResult.isMechanicallyStable,
                statusMessage = eqResult.statusMessage
            )
        }

        // 2. Solve internal forces N(x), T(x), M(x)
        val internalResult = InternalForcesSolver.computeInternalForces(structure, eqResult.reactions)

        // 3. Assemble all steps
        val allSteps = mutableListOf<StepItem>()
        allSteps.addAll(eqResult.steps)
        allSteps.addAll(internalResult.steps)

        // Step for Max Moment
        internalResult.maxMoment?.let { maxM ->
            allSteps.add(
                StepItem(
                    stepNumber = allSteps.size + 1,
                    title = "Valeurs remarquables et extremum du moment fléchissant",
                    formula = "T(x) = 0 \\implies \\frac{dM(x)}{dx} = 0",
                    substitution = "Extremum situé à l'abscisse x = %.3f m".format(Locale.US, maxM.x),
                    result = "M_{\\max} = %.2f".format(Locale.US, maxM.valueM),
                    unit = "kN·m",
                    details = "Le moment fléchissant atteint son extremum là où l'effort tranchant s'annule ou change de signe."
                )
            )
        }

        // 4. Phase 2: Section Geometry and Stress Distribution (Navier, Jourawski, Von Mises)
        val sectionProps = SectionSolver.computeProperties(structure.section)
        
        // Critical section: Evaluate stress state at the point of maximum bending moment or maximum shear
        val critMomentPoint = internalResult.remarkablePoints.maxByOrNull { kotlin.math.abs(it.valueM) }
        val critShearPoint = internalResult.remarkablePoints.maxByOrNull { kotlin.math.abs(it.valueT) }
        
        val critX = critMomentPoint?.x ?: (structure.length / 2.0)
        val critN = critMomentPoint?.valueN ?: 0.0
        val critT = critShearPoint?.valueT ?: (critMomentPoint?.valueT ?: 0.0)
        val critM = critMomentPoint?.valueM ?: 0.0

        val maxStress = SectionSolver.computeStresses(
            sectionX = critX,
            n_kN = critN,
            t_kN = critT,
            m_kNm = critM,
            props = sectionProps,
            material = structure.material
        )

        val sectionSteps = SectionSolver.generateSectionSteps(
            section = structure.section,
            props = sectionProps,
            material = structure.material,
            stress = maxStress,
            stepOffset = allSteps.size
        )
        allSteps.addAll(sectionSteps)

        // 5. Phase 3: Deflection and Elastic Line (Double Integration & ELS Check)
        val deflectionResult = DeflectionSolver.computeDeflection(
            structure = structure,
            segments = internalResult.segments,
            props = sectionProps,
            material = structure.material
        )

        val deflectionSteps = DeflectionSolver.generateDeflectionSteps(
            structure = structure,
            props = sectionProps,
            material = structure.material,
            deflection = deflectionResult,
            stepOffset = allSteps.size
        )
        allSteps.addAll(deflectionSteps)

        // 6. Automated Civil Engineering Verifications
        val checks = VerificationEngine.verifySolution(
            structure = structure,
            reactions = eqResult.reactions,
            segments = internalResult.segments
        ).toMutableList()

        // Resistance check (ELU / Von Mises)
        checks.add(
            EngineeringCheck(
                name = "Résistance mécanique de la section (ELU / Von Mises)",
                isPassed = maxStress.isSafe,
                details = "\\sigma_{VM} = %.2f \\text{ MPa} \\le f_y = %.1f \\text{ MPa} (\\eta = %.1f\\%%)".format(
                    Locale.US, maxStress.vonMisesStress_MPa, structure.material.yieldStrengthFy_MPa, maxStress.utilizationRatio * 100.0
                ),
                tolerance = "\\eta \\le 100\\%"
            )
        )

        // Deflection check (ELS / Eurocode)
        checks.add(
            EngineeringCheck(
                name = "Critère de flèche admissible (ELS / Eurocode L/%d)".format(deflectionResult.limitRatioDenominator),
                isPassed = deflectionResult.isElsCompliant,
                details = "f_{\\max} = %.2f \\text{ mm} \\le f_{\\text{lim}} = %.2f \\text{ mm} (\\eta_{\\text{ELS}} = %.1f\\%%)".format(
                    Locale.US, kotlin.math.abs(deflectionResult.maxDeflection_mm), deflectionResult.deflectionLimit_mm, deflectionResult.elsUtilizationRatio * 100.0
                ),
                tolerance = "f \\le L/500"
            )
        )

        return RdmSolution(
            structure = structure,
            reactions = eqResult.reactions,
            segments = internalResult.segments,
            diagramN = internalResult.diagramN,
            diagramT = internalResult.diagramT,
            diagramM = internalResult.diagramM,
            remarkablePoints = internalResult.remarkablePoints,
            maxMoment = internalResult.maxMoment,
            minMoment = internalResult.minMoment,
            maxShear = internalResult.maxShear,
            steps = allSteps,
            checks = checks,
            isDeterminate = true,
            isMechanicallyStable = true,
            statusMessage = "Solution exacte calculée avec succès.",
            sectionProperties = sectionProps,
            maxStressResult = maxStress,
            deflectionResult = deflectionResult
        )
    }
}
