package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

object InternalForcesSolver {

    private const val EPS = 1e-6

    data class InternalForcesResult(
        val segments: List<SegmentEquation>,
        val diagramN: List<DiagramPoint>,
        val diagramT: List<DiagramPoint>,
        val diagramM: List<DiagramPoint>,
        val remarkablePoints: List<RemarkablePoint>,
        val maxMoment: RemarkablePoint?,
        val minMoment: RemarkablePoint?,
        val maxShear: RemarkablePoint?,
        val steps: List<StepItem>
    )

    /**
     * Compute internal forces N(x), T(x), M(x) across all segments of the beam.
     */
    fun computeInternalForces(
        structure: BeamStructure,
        reactions: List<SupportReaction>
    ): InternalForcesResult {
        val beamLength = structure.length
        val supports = structure.supports
        val loads = structure.loads

        // 1. Gather all transition / singularity points
        val points = sortedSetOf<Double>()
        points.add(0.0)
        points.add(beamLength)
        supports.forEach { points.add(it.position.coerceIn(0.0, beamLength)) }
        loads.forEach { load ->
            points.add(load.positionStart.coerceIn(0.0, beamLength))
            if (load.type == LoadType.DISTRIBUTED_UNIFORM || load.type == LoadType.DISTRIBUTED_TRIANGULAR) {
                points.add(load.positionEnd.coerceIn(0.0, beamLength))
            }
        }

        // Clean & merge points closer than 1e-4
        val sortedPoints = points.toList()
        val distinctPoints = mutableListOf<Double>()
        for (p in sortedPoints) {
            if (distinctPoints.isEmpty() || abs(p - distinctPoints.last()) > 1e-4) {
                distinctPoints.add(p)
            }
        }

        val segments = mutableListOf<SegmentEquation>()
        val derivationSteps = mutableListOf<StepItem>()

        // 2. Build equations for each segment [xStart, xEnd]
        for (i in 0 until distinctPoints.size - 1) {
            val xStart = distinctPoints[i]
            val xEnd = distinctPoints[i + 1]
            val segIndex = i + 1

            // Evaluator function for any x strictly inside or at boundary of [xStart, xEnd]
            val nFunc: (Double) -> Double = { x -> evalN(x, structure, reactions) }
            val tFunc: (Double) -> Double = { x -> evalT(x, structure, reactions) }
            val mFunc: (Double) -> Double = { x -> evalM(x, structure, reactions) }

            // Sample values at start, mid, end to generate representative analytical expression
            val xMid = (xStart + xEnd) / 2.0
            val t0 = tFunc(xStart + 1e-5)
            val t1 = tFunc(xEnd - 1e-5)
            val m0 = mFunc(xStart + 1e-5)
            val mMid = mFunc(xMid)
            val m1 = mFunc(xEnd - 1e-5)

            // Approximate polynomial coefficients for display
            val tSlope = (t1 - t0) / (xEnd - xStart)
            val tConst = t0 - tSlope * xStart

            val tExpr = if (abs(tSlope) < 1e-3) {
                "%.2f \\text{ kN}".format(Locale.US, t0)
            } else {
                "%.2f %s %.2f x \\text{ kN}".format(
                    Locale.US, tConst, if (tSlope >= 0) "+" else "-", abs(tSlope)
                )
            }

            val mExpr = if (abs(tSlope) < 1e-3) {
                val mSlope = (m1 - m0) / (xEnd - xStart)
                val mConst = m0 - mSlope * xStart
                if (abs(mSlope) < 1e-3) {
                    "%.2f \\text{ kN}\\cdot\\text{m}".format(Locale.US, m0)
                } else {
                    "%.2f %s %.2f x \\text{ kN}\\cdot\\text{m}".format(
                        Locale.US, mConst, if (mSlope >= 0) "+" else "-", abs(mSlope)
                    )
                }
            } else {
                val aQuad = tSlope / 2.0
                val bLin = tConst
                val cConst = m0 - (aQuad * xStart * xStart + bLin * xStart)
                "%.2f x^2 %s %.2f x %s %.2f \\text{ kN}\\cdot\\text{m}".format(
                    Locale.US, aQuad,
                    if (bLin >= 0) "+" else "-", abs(bLin),
                    if (cConst >= 0) "+" else "-", abs(cConst)
                )
            }

            val nVal = nFunc(xMid)
            val nExpr = "%.2f \\text{ kN}".format(Locale.US, nVal)

            segments.add(
                SegmentEquation(
                    segmentIndex = segIndex,
                    xStart = xStart,
                    xEnd = xEnd,
                    nExpr = nExpr,
                    tExpr = tExpr,
                    mExpr = mExpr,
                    nFunc = nFunc,
                    tFunc = tFunc,
                    mFunc = mFunc
                )
            )

            derivationSteps.add(
                StepItem(
                    stepNumber = 5 + i,
                    title = "Tronçon %d : x \\in [%.2f, %.2f] m".format(segIndex, xStart, xEnd),
                    formula = "T(x) = \\sum F_{y,\\text{gauche}}, \\quad M(x) = \\sum M_{(x),\\text{gauche}}",
                    substitution = "T(x) = %s\nM(x) = %s".format(tExpr, mExpr),
                    result = "T(%.2f) = %.2f \\text{ kN}, \\quad M(%.2f) = %.2f \\text{ kN}\\cdot\\text{m}".format(
                        Locale.US, xEnd, t1, xEnd, m1
                    ),
                    unit = "kN / kN·m",
                    details = "Effort normal N(x) = %s. Dérivée : dM/dx = T(x).".format(nExpr)
                )
            )
        }

        // 3. Dense sampling for diagrams (200 points + discontinuities)
        val numSamples = 200
        val diagN = mutableListOf<DiagramPoint>()
        val diagT = mutableListOf<DiagramPoint>()
        val diagM = mutableListOf<DiagramPoint>()

        for (seg in segments) {
            val stepsPerSeg = max(10, (numSamples * ((seg.xEnd - seg.xStart) / beamLength)).toInt())
            for (s in 0..stepsPerSeg) {
                val frac = s.toDouble() / stepsPerSeg
                val x = seg.xStart + frac * (seg.xEnd - seg.xStart)
                val safeX = if (s == 0) x + 1e-6 else if (s == stepsPerSeg) x - 1e-6 else x
                diagN.add(DiagramPoint(x, seg.nFunc(safeX)))
                diagT.add(DiagramPoint(x, seg.tFunc(safeX)))
                diagM.add(DiagramPoint(x, seg.mFunc(safeX)))
            }
        }

        // 4. Find Remarkable Points (Extrema, Roots of T, Supports)
        val remarkable = mutableListOf<RemarkablePoint>()

        for (p in distinctPoints) {
            val leftT = evalT(max(0.0, p - 1e-5), structure, reactions)
            val rightT = evalT(min(beamLength, p + 1e-5), structure, reactions)
            val leftM = evalM(max(0.0, p - 1e-5), structure, reactions)
            val rightM = evalM(min(beamLength, p + 1e-5), structure, reactions)
            val nVal = evalN(p, structure, reactions)

            remarkable.add(
                RemarkablePoint(
                    x = p,
                    label = "x = %.2f m".format(Locale.US, p),
                    valueN = nVal,
                    valueT = rightT,
                    valueM = max(abs(leftM), abs(rightM)) * (if (abs(leftM) > abs(rightM)) sign(leftM) else sign(rightM))
                )
            )
        }

        // Find roots of T(x) = 0 inside each segment (where M achieves local extremum)
        for (seg in segments) {
            val tStart = seg.tFunc(seg.xStart + 1e-5)
            val tEnd = seg.tFunc(seg.xEnd - 1e-5)
            if (tStart * tEnd < 0.0) {
                // Linear root finding
                val rootX = seg.xStart + tStart * (seg.xEnd - seg.xStart) / (tStart - tEnd)
                if (rootX > seg.xStart + 1e-4 && rootX < seg.xEnd - 1e-4) {
                    val mRoot = seg.mFunc(rootX)
                    val nRoot = seg.nFunc(rootX)
                    remarkable.add(
                        RemarkablePoint(
                            x = rootX,
                            label = "T=0 (M_extr)",
                            valueN = nRoot,
                            valueT = 0.0,
                            valueM = mRoot,
                            isExtremumM = true
                        )
                    )
                }
            }
        }

        val sortedRemarkable = remarkable.sortedBy { it.x }
        val maxM = sortedRemarkable.maxByOrNull { it.valueM }
        val minM = sortedRemarkable.minByOrNull { it.valueM }
        val maxT = sortedRemarkable.maxByOrNull { abs(it.valueT) }

        return InternalForcesResult(
            segments = segments,
            diagramN = diagN,
            diagramT = diagT,
            diagramM = diagM,
            remarkablePoints = sortedRemarkable,
            maxMoment = maxM,
            minMoment = minM,
            maxShear = maxT,
            steps = derivationSteps
        )
    }

    /**
     * Compute normal force N(x) using left-cut method.
     * N(x) = - sum Fx_gauche
     */
    private fun evalN(x: Double, structure: BeamStructure, reactions: List<SupportReaction>): Double {
        var n = 0.0
        // Reactions to the left
        for (r in reactions) {
            if (r.position <= x + EPS) {
                n -= r.rx
            }
        }
        // Loads to the left
        for (l in structure.loads) {
            if (l.type == LoadType.POINT_FORCE && l.positionStart <= x + EPS) {
                val rad = Math.toRadians(l.angleDegrees)
                val fx = l.magnitude * cos(rad)
                n += fx // positive x causes compression
            }
        }
        return n
    }

    /**
     * Compute shear force T(x) using left-cut method.
     * T(x) = sum Fy_gauche (Upwards = positive)
     */
    private fun evalT(x: Double, structure: BeamStructure, reactions: List<SupportReaction>): Double {
        var t = 0.0
        // 1. Reactions to the left (upwards is positive)
        for (r in reactions) {
            if (r.position <= x) {
                t += r.ry
            }
        }

        // 2. Loads to the left (downwards is negative in left-cut)
        for (l in structure.loads) {
            when (l.type) {
                LoadType.POINT_FORCE -> {
                    if (l.positionStart <= x) {
                        val rad = Math.toRadians(l.angleDegrees)
                        val fy = l.magnitude * sin(rad)
                        t -= fy
                    }
                }
                LoadType.DISTRIBUTED_UNIFORM -> {
                    if (x > l.positionStart) {
                        val activeLen = min(x, l.positionEnd) - l.positionStart
                        if (activeLen > 0.0) {
                            t -= l.magnitude * activeLen
                        }
                    }
                }
                LoadType.DISTRIBUTED_TRIANGULAR -> {
                    if (x > l.positionStart) {
                        val totalLen = max(1e-5, l.positionEnd - l.positionStart)
                        val u = min(x, l.positionEnd) - l.positionStart
                        if (u > 0.0) {
                            val q1 = l.magnitude
                            val q2 = l.magnitudeEnd
                            val qAtU = q1 + (q2 - q1) * (u / totalLen)
                            val integratedQ = 0.5 * (q1 + qAtU) * u
                            t -= integratedQ
                        }
                    }
                }
                LoadType.MOMENT -> {
                    // Concentrated moment does not affect shear force directly
                }
            }
        }
        return t
    }

    /**
     * Compute bending moment M(x) using left-cut method.
     * M(x) = sum M_gauche(x) (Tension in bottom fibers is positive: +M)
     * Upward force R_y at x_r < x: exerts clockwise moment around cut x?
     * Wait, let's verify sign convention!
     * Convention de gauche:
     * A cut at x: the left piece has the external upward reaction R_y at x_r (x_r < x).
     * The lever arm is (x - x_r) > 0.
     * The force R_y points upwards (+y).
     * Around the point x, an upward force to the left rotates CLOCKWISE (+).
     * Clockwise rotation of the left part bends the beam sagging (fibres inférieures tendues!).
     * Therefore, an upward reaction produces POSITIVE bending moment +R_y * (x - x_r).
     * A downward load F at x_f < x produces NEGATIVE bending moment -F * (x - x_f).
     * For a simple beam with downward central load:
     * Reaction at left is R_A = F/2 (upward).
     * At midspan x = L/2: M(L/2) = R_A * (L/2) = (F/2) * (L/2) = +FL/4 > 0.
     * Bottom fibers are in tension. Exactly standard RDM!
     */
    private fun evalM(x: Double, structure: BeamStructure, reactions: List<SupportReaction>): Double {
        var m = 0.0

        // 1. Reactions to the left
        for (r in reactions) {
            if (r.position <= x) {
                // Vertical reaction creates +Ry * (x - r.position)
                m += r.ry * (x - r.position)
                // Encastrement moment (counter-clockwise is positive M_enc in statics)
                // Counter-clockwise on the left part pushes bottom fibers into tension?
                // Wait: counter-clockwise external moment on left part curves beam with center of curvature ABOVE beam -> sagging (+)!
                m += r.m
            }
        }

        // 2. Loads to the left
        for (l in structure.loads) {
            when (l.type) {
                LoadType.POINT_FORCE -> {
                    if (l.positionStart <= x) {
                        val rad = Math.toRadians(l.angleDegrees)
                        val fy = l.magnitude * sin(rad)
                        m -= fy * (x - l.positionStart)
                    }
                }
                LoadType.DISTRIBUTED_UNIFORM -> {
                    if (x > l.positionStart) {
                        val u = min(x, l.positionEnd) - l.positionStart
                        if (u > 0.0) {
                            if (x <= l.positionEnd) {
                                // Cut is inside distributed load
                                val q = l.magnitude
                                m -= 0.5 * q * u * u
                            } else {
                                // Cut is to the right of distributed load
                                val totalQ = l.magnitude * (l.positionEnd - l.positionStart)
                                val xG = (l.positionStart + l.positionEnd) / 2.0
                                m -= totalQ * (x - xG)
                            }
                        }
                    }
                }
                LoadType.DISTRIBUTED_TRIANGULAR -> {
                    if (x > l.positionStart) {
                        val totalLen = max(1e-5, l.positionEnd - l.positionStart)
                        val u = min(x, l.positionEnd) - l.positionStart
                        if (u > 0.0) {
                            val q1 = l.magnitude
                            val q2 = l.magnitudeEnd
                            if (x <= l.positionEnd) {
                                // Integral of q(x')(x - x') from xStart to x:
                                // q(x') = q1 + (q2 - q1)*t/L, where t = x' - xStart, t in [0, u]
                                // (x - x') = u - t
                                // int_0^u [q1 + (q2 - q1)*t/L] * (u - t) dt
                                // = q1 * u^2 / 2 + (q2 - q1)/L * u^3 / 6
                                val mLoad = q1 * u * u / 2.0 + (q2 - q1) / totalLen * (u * u * u) / 6.0
                                m -= mLoad
                            } else {
                                val totalQ = 0.5 * (q1 + q2) * totalLen
                                val xG = l.positionStart + (totalLen / 3.0) * ((q1 + 2.0 * q2) / (q1 + q2))
                                m -= totalQ * (x - xG)
                            }
                        }
                    }
                }
                LoadType.MOMENT -> {
                    if (l.positionStart <= x) {
                        // Clockwise moment applied externally
                        // Convention: Clockwise external couple on the left face creates positive bending moment
                        m += l.magnitude
                    }
                }
            }
        }
        return m
    }
}
