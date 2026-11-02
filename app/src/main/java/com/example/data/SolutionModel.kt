package com.example.data

/**
 * Support reaction result.
 */
data class SupportReaction(
    val supportId: String,
    val supportName: String,
    val supportType: SupportType,
    val position: Double,
    val rx: Double = 0.0, // kN (horizontal)
    val ry: Double = 0.0, // kN (vertical, positive upwards)
    val m: Double = 0.0   // kN·m (moment d'encastrement, positive counter-clockwise)
)

/**
 * Continuous mathematical equations of internal forces on a segment [xStart, xEnd].
 */
data class SegmentEquation(
    val segmentIndex: Int,
    val xStart: Double,
    val xEnd: Double,
    val nExpr: String,
    val tExpr: String,
    val mExpr: String,
    val nFunc: (Double) -> Double,
    val tFunc: (Double) -> Double,
    val mFunc: (Double) -> Double
)

/**
 * Discrete point on a diagram for graphing.
 */
data class DiagramPoint(
    val x: Double,
    val value: Double
)

/**
 * Notable physical point (maximum, minimum, support, zero-crossing).
 */
data class RemarkablePoint(
    val x: Double,
    val label: String,
    val valueN: Double,
    val valueT: Double,
    val valueM: Double,
    val isExtremumM: Boolean = false,
    val isExtremumT: Boolean = false
)

/**
 * Step in the step-by-step engineering resolution.
 * Follows: Formula -> Substitution -> Result -> Unit
 */
data class StepItem(
    val stepNumber: Int,
    val title: String,
    val formula: String,
    val substitution: String,
    val result: String,
    val unit: String,
    val details: String = ""
)

/**
 * Automated engineering verification check.
 */
data class EngineeringCheck(
    val name: String,
    val isPassed: Boolean,
    val details: String,
    val tolerance: String = "< 1e-4"
)

/**
 * Full solution returned by RdmSolver.
 */
data class RdmSolution(
    val structure: BeamStructure,
    val reactions: List<SupportReaction>,
    val segments: List<SegmentEquation>,
    val diagramN: List<DiagramPoint>,
    val diagramT: List<DiagramPoint>,
    val diagramM: List<DiagramPoint>,
    val remarkablePoints: List<RemarkablePoint>,
    val maxMoment: RemarkablePoint?,
    val minMoment: RemarkablePoint?,
    val maxShear: RemarkablePoint?,
    val steps: List<StepItem>,
    val checks: List<EngineeringCheck>,
    val isDeterminate: Boolean,
    val isMechanicallyStable: Boolean,
    val statusMessage: String,
    val sectionProperties: SectionProperties? = null,
    val maxStressResult: StressResult? = null,
    val deflectionResult: DeflectionResult? = null
)
