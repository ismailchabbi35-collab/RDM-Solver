package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

object VerificationEngine {

    private const val TOLERANCE = 1e-3

    /**
     * Run all automated civil engineering checks on the solved beam structure.
     */
    fun verifySolution(
        structure: BeamStructure,
        reactions: List<SupportReaction>,
        segments: List<SegmentEquation>
    ): List<EngineeringCheck> {
        val checks = mutableListOf<EngineeringCheck>()

        // 1. Global Horizontal Equilibrium (sum Fx = 0)
        var sumFx = reactions.sumOf { it.rx }
        for (load in structure.loads) {
            if (load.type == LoadType.POINT_FORCE) {
                val rad = Math.toRadians(load.angleDegrees)
                sumFx += load.magnitude * cos(rad)
            }
        }
        val passFx = abs(sumFx) < TOLERANCE
        checks.add(
            EngineeringCheck(
                name = "Équilibre global horizontal (\\sum F_x = 0)",
                isPassed = passFx,
                details = "Résidu : %.4f kN (Tolérance < %.3f kN)".format(Locale.US, sumFx, TOLERANCE),
                tolerance = "< 1e-3 kN"
            )
        )

        // 2. Global Vertical Equilibrium (sum Fy = 0)
        // Upward reactions + Fy_reactions, downward loads - Fy_loads
        var sumFy = reactions.sumOf { it.ry }
        for (load in structure.loads) {
            when (load.type) {
                LoadType.POINT_FORCE -> {
                    val rad = Math.toRadians(load.angleDegrees)
                    sumFy -= load.magnitude * sin(rad)
                }
                LoadType.DISTRIBUTED_UNIFORM -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    sumFy -= load.magnitude * len
                }
                LoadType.DISTRIBUTED_TRIANGULAR -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    sumFy -= 0.5 * (load.magnitude + load.magnitudeEnd) * len
                }
                LoadType.MOMENT -> {}
            }
        }
        val passFy = abs(sumFy) < TOLERANCE
        checks.add(
            EngineeringCheck(
                name = "Équilibre global vertical (\\sum F_y = 0)",
                isPassed = passFy,
                details = "Résidu : %.4f kN (Tolérance < %.3f kN)".format(Locale.US, sumFy, TOLERANCE),
                tolerance = "< 1e-3 kN"
            )
        )

        // 3. Global Moment Equilibrium about origin (sum M/0 = 0)
        var sumM0 = reactions.sumOf { it.ry * it.position + it.m }
        for (load in structure.loads) {
            when (load.type) {
                LoadType.POINT_FORCE -> {
                    val rad = Math.toRadians(load.angleDegrees)
                    val fy = load.magnitude * sin(rad)
                    sumM0 -= fy * load.positionStart
                }
                LoadType.DISTRIBUTED_UNIFORM -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    val qTotal = load.magnitude * len
                    val xG = (load.positionStart + load.positionEnd) / 2.0
                    sumM0 -= qTotal * xG
                }
                LoadType.DISTRIBUTED_TRIANGULAR -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    val qTotal = 0.5 * (load.magnitude + load.magnitudeEnd) * len
                    val q1 = load.magnitude
                    val q2 = load.magnitudeEnd
                    val xG = if (abs(q1 + q2) > 1e-6) {
                        load.positionStart + (len / 3.0) * ((q1 + 2.0 * q2) / (q1 + q2))
                    } else {
                        (load.positionStart + load.positionEnd) / 2.0
                    }
                    sumM0 -= qTotal * xG
                }
                LoadType.MOMENT -> {
                    sumM0 -= load.magnitude
                }
            }
        }
        val passM0 = abs(sumM0) < TOLERANCE
        checks.add(
            EngineeringCheck(
                name = "Équilibre global des moments (\\sum M_{/0} = 0)",
                isPassed = passM0,
                details = "Résidu : %.4f kN·m (Tolérance < %.3f kN·m)".format(Locale.US, sumM0, TOLERANCE),
                tolerance = "< 1e-3 kN·m"
            )
        )

        // 4. Boundary conditions: M = 0 at simple supports / free ends without concentrated couples
        for (sup in structure.supports) {
            if (sup.type == SupportType.PINNED || sup.type == SupportType.ROLLER) {
                // If there is no concentrated moment at this support, M must be 0
                val hasMomentAtSup = structure.loads.any {
                    it.type == LoadType.MOMENT && abs(it.positionStart - sup.position) < 1e-3
                }
                if (!hasMomentAtSup) {
                    val seg = segments.firstOrNull { sup.position >= it.xStart - 1e-4 && sup.position <= it.xEnd + 1e-4 }
                    if (seg != null) {
                        val mVal = seg.mFunc(sup.position)
                        val pass = abs(mVal) < 0.05
                        checks.add(
                            EngineeringCheck(
                                name = "Condition aux limites en ${sup.name} (x = %.2f m)".format(Locale.US, sup.position),
                                isPassed = pass,
                                details = "M(${sup.name}) = %.4f kN·m (Appui simple sans couple imposé => M = 0)".format(Locale.US, mVal),
                                tolerance = "< 0.05 kN·m"
                            )
                        )
                    }
                }
            }
        }

        // 5. Differential relation dM/dx = T(x) check
        var maxDiffError = 0.0
        val delta = 1e-4
        for (seg in segments) {
            val xTest = (seg.xStart + seg.xEnd) / 2.0
            val tVal = seg.tFunc(xTest)
            val dM = (seg.mFunc(xTest + delta) - seg.mFunc(xTest - delta)) / (2.0 * delta)
            val err = abs(dM - tVal)
            if (err > maxDiffError) maxDiffError = err
        }
        val passDiff = maxDiffError < 0.05
        checks.add(
            EngineeringCheck(
                name = "Relation différentielle fondamentale (dM/dx = T)",
                isPassed = passDiff,
                details = "Écart maximal sur les tronçons : %.4f (Vérification de la pente de M)".format(Locale.US, maxDiffError),
                tolerance = "< 0.05"
            )
        )

        // 6. Shear Jump Check at Point Loads (Delta T = applied vertical force)
        for (load in structure.loads) {
            if (load.type == LoadType.POINT_FORCE) {
                val rad = Math.toRadians(load.angleDegrees)
                val fy = load.magnitude * sin(rad)
                val x = load.positionStart
                val tLeft = segments.firstOrNull { it.xStart <= x && it.xEnd >= x }?.tFunc(max(0.0, x - 1e-5)) ?: 0.0
                val tRight = segments.lastOrNull { it.xStart <= x && it.xEnd >= x }?.tFunc(min(structure.length, x + 1e-5)) ?: 0.0
                val jump = abs((tLeft - tRight) - fy)
                val passJump = jump < 0.05
                checks.add(
                    EngineeringCheck(
                        name = "Saut d'effort tranchant sous force ponctuelle (x = %.2f m)".format(Locale.US, x),
                        isPassed = passJump,
                        details = "\\Delta T = T(x^-) - T(x^+) = %.2f kN (Force appliquée : %.2f kN)".format(Locale.US, tLeft - tRight, fy),
                        tolerance = "< 0.05 kN"
                    )
                )
            }
        }

        return checks
    }
}
