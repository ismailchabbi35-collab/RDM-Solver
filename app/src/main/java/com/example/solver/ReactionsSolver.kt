package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

/**
 * Equivalent resultant of an applied load for global equilibrium calculations.
 */
data class EquivalentResultant(
    val loadId: String,
    val type: LoadType,
    val fx: Double,          // kN (horizontal, positive to the right)
    val fy: Double,          // kN (vertical, positive downwards)
    val momentAboutZero: Double, // kN·m (moment about origin x=0, counter-clockwise positive)
    val description: String,
    val centroidX: Double,
    val totalMagnitude: Double
)

/**
 * Reactions solver: Evaluates global static equilibrium and produces detailed derivation steps.
 */
object ReactionsSolver {

    private const val EPS = 1e-6

    /**
     * Compute equivalent resultants of all loads on the beam.
     */
    fun computeResultants(loads: List<Load>): List<EquivalentResultant> {
        return loads.map { load ->
            when (load.type) {
                LoadType.POINT_FORCE -> {
                    val rad = Math.toRadians(load.angleDegrees)
                    val fx = load.magnitude * cos(rad)
                    val fy = load.magnitude * sin(rad)
                    // Downward force produces clockwise moment about origin: -fy * x
                    // Horizontal force along beam axis: moment about origin y=0 is zero
                    val m0 = -fy * load.positionStart
                    EquivalentResultant(
                        loadId = load.id,
                        type = load.type,
                        fx = fx,
                        fy = fy,
                        momentAboutZero = m0,
                        description = "Force ponctuelle F = %.2f kN à x = %.2f m (angle %.0f°)".format(
                            Locale.US, load.magnitude, load.positionStart, load.angleDegrees
                        ),
                        centroidX = load.positionStart,
                        totalMagnitude = load.magnitude
                    )
                }
                LoadType.DISTRIBUTED_UNIFORM -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    val totalQ = load.magnitude * len
                    val xG = (load.positionStart + load.positionEnd) / 2.0
                    val m0 = -totalQ * xG
                    EquivalentResultant(
                        loadId = load.id,
                        type = load.type,
                        fx = 0.0,
                        fy = totalQ,
                        momentAboutZero = m0,
                        description = "Charge uniforme q = %.2f kN/m sur [%.2f, %.2f] m (Q = q·L = %.2f kN, x_G = %.2f m)".format(
                            Locale.US, load.magnitude, load.positionStart, load.positionEnd, totalQ, xG
                        ),
                        centroidX = xG,
                        totalMagnitude = totalQ
                    )
                }
                LoadType.DISTRIBUTED_TRIANGULAR -> {
                    val len = max(0.0, load.positionEnd - load.positionStart)
                    val q1 = load.magnitude
                    val q2 = load.magnitudeEnd
                    val totalQ = 0.5 * (q1 + q2) * len
                    val xG = if (abs(q1 + q2) > EPS) {
                        load.positionStart + (len / 3.0) * ((q1 + 2.0 * q2) / (q1 + q2))
                    } else {
                        (load.positionStart + load.positionEnd) / 2.0
                    }
                    val m0 = -totalQ * xG
                    EquivalentResultant(
                        loadId = load.id,
                        type = load.type,
                        fx = 0.0,
                        fy = totalQ,
                        momentAboutZero = m0,
                        description = "Charge trapézoïdale q1=%.2f -> q2=%.2f kN/m sur [%.2f, %.2f] m (Q = %.2f kN, x_G = %.2f m)".format(
                            Locale.US, q1, q2, load.positionStart, load.positionEnd, totalQ, xG
                        ),
                        centroidX = xG,
                        totalMagnitude = totalQ
                    )
                }
                LoadType.MOMENT -> {
                    // Clockwise moment = negative in CCW convention
                    val m0 = -load.magnitude
                    EquivalentResultant(
                        loadId = load.id,
                        type = load.type,
                        fx = 0.0,
                        fy = 0.0,
                        momentAboutZero = m0,
                        description = "Couple concentré C = %.2f kN·m à x = %.2f m".format(
                            Locale.US, load.magnitude, load.positionStart
                        ),
                        centroidX = load.positionStart,
                        totalMagnitude = load.magnitude
                    )
                }
            }
        }
    }

    data class EquilibriumResult(
        val isDeterminate: Boolean,
        val isMechanicallyStable: Boolean,
        val statusMessage: String,
        val reactions: List<SupportReaction>,
        val steps: List<StepItem>
    )

    /**
     * Solves for reactions on statically determinate beams with rigorous engineering derivation steps.
     */
    fun solveReactions(structure: BeamStructure): EquilibriumResult {
        val steps = mutableListOf<StepItem>()
        val supports = structure.supports.sortedBy { it.position }
        val loads = structure.loads

        // Count degrees of restraint
        var unknownsCount = 0
        var hasFixed = false
        for (s in supports) {
            when (s.type) {
                SupportType.FIXED -> {
                    unknownsCount += 3
                    hasFixed = true
                }
                SupportType.PINNED -> unknownsCount += 2
                SupportType.ROLLER -> unknownsCount += 1
            }
        }

        // Stability & Determinacy Checks
        if (supports.isEmpty()) {
            return EquilibriumResult(
                isDeterminate = false,
                isMechanicallyStable = false,
                statusMessage = "Structure instable : aucun appui défini (mécanisme libre).",
                reactions = emptyList(),
                steps = listOf(
                    StepItem(
                        stepNumber = 1,
                        title = "Vérification de la stabilité statique",
                        formula = "d = r - m",
                        substitution = "r = 0 liaisons, m = 3 équations de la statique",
                        result = "d = -3 (Mécanisme)",
                        unit = "-",
                        details = "La structure ne possède aucun appui. Ajoutez au moins 3 degrés de liaison (ex: 1 encastrement ou 1 articulation + 1 appui simple)."
                    )
                )
            )
        }

        if (unknownsCount < 3) {
            val missingDof = 3 - unknownsCount
            return EquilibriumResult(
                isDeterminate = false,
                isMechanicallyStable = false,
                statusMessage = "Structure instable (mécanisme) : il manque $missingDof degré(s) de liaison.",
                reactions = emptyList(),
                steps = listOf(
                    StepItem(
                        stepNumber = 1,
                        title = "Degré d'hyperstaticité / instabilité",
                        formula = "h = r - 3",
                        substitution = "r = $unknownsCount, équations = 3",
                        result = "h = ${unknownsCount - 3} < 0",
                        unit = "DDL manquants",
                        details = "Structure instable. Ajoutez un appui pour bloquer les mouvements de corps rigide."
                    )
                )
            )
        }

        if (unknownsCount > 3) {
            val deg = unknownsCount - 3
            return EquilibriumResult(
                isDeterminate = false,
                isMechanicallyStable = true,
                statusMessage = "Structure hyperstatique (degré d'hyperstaticité h = $deg).",
                reactions = emptyList(),
                steps = listOf(
                    StepItem(
                        stepNumber = 1,
                        title = "Degré d'hyperstaticité",
                        formula = "h = r - 3",
                        substitution = "r = $unknownsCount inconnues statiques, 3 équations d'équilibre",
                        result = "h = $deg (Hyperstatique)",
                        unit = "-",
                        details = "Cette structure comporte des inconnues redondantes. Sa résolution relève de la Phase 5 (Théorème des 3 moments de Clapeyron ou Méthode des forces)."
                    )
                )
            )
        }

        // Compute external resultants
        val resultants = computeResultants(loads)
        val sumFx = resultants.sumOf { it.fx }
        val sumFy = resultants.sumOf { it.fy } // positive downwards

        // Step 1: Sign convention & Modeling
        steps.add(
            StepItem(
                stepNumber = 1,
                title = "Modélisation et conventions de signes RDM",
                formula = "\\text{Axe } x \\in [0, L], \\quad y \\uparrow +, \\quad \\circlearrowleft +",
                substitution = "L = %.2f \\text{ m}, \\quad \\text{Nombre d'appuis} = %d, \\quad \\text{Nombre de charges} = %d".format(
                    Locale.US, structure.length, supports.size, loads.size
                ),
                result = "Structure isostatique (h = 0)",
                unit = "-",
                details = "Convention française Eurocode : réactions ascendantes positives (+Ry ↑), moments antihoraires positifs (+M ↺)."
            )
        )

        // Step 2: Resultants of distributed loads
        val resultantsSummary = if (resultants.isNotEmpty()) {
            resultants.joinToString("\n• ") { it.description }
        } else {
            "Aucune charge extérieure appliquée."
        }
        steps.add(
            StepItem(
                stepNumber = 2,
                title = "Bilan des actions extérieures et résultantes",
                formula = "Q = \\int q(x) dx, \\quad x_G = \\frac{\\int x q(x) dx}{Q}",
                substitution = "Somme des forces verticales : \\sum F_{y,\\text{ext}} = %.2f \\text{ kN}".format(Locale.US, sumFy),
                result = "Q_{\\text{total}} = %.2f".format(Locale.US, sumFy),
                unit = "kN",
                details = "• $resultantsSummary"
            )
        )

        val reactions = mutableListOf<SupportReaction>()

        // CASE 1: Single FIXED Support (Cantilever beam, e.g. at x = xA)
        if (supports.size == 1 && supports[0].type == SupportType.FIXED) {
            val sup = supports[0]
            val xA = sup.position
            // Equilibrium equations:
            // 1) sum Fx = 0 => Rx + sumFx_ext = 0 => Rx = -sumFx
            val rx = -sumFx
            // 2) sum Fy = 0 => Ry - sumFy = 0 => Ry = sumFy (upwards)
            val ry = sumFy
            // 3) sum M/A = 0 => M_enc + sum M_A(ext) = 0
            // External moment of forces about A:
            var sumMomentAboutA = 0.0
            for (res in resultants) {
                if (res.type == LoadType.MOMENT) {
                    sumMomentAboutA += res.momentAboutZero
                } else {
                    sumMomentAboutA += -res.fy * (res.centroidX - xA)
                }
            }
            // For a fixed support at xA:
            // Downward loads produce clockwise rotation around A (sumMomentAboutA < 0),
            // which creates a negative (hogging) bending moment M_enc = sumMomentAboutA.
            val mEnc = sumMomentAboutA

            reactions.add(
                SupportReaction(
                    supportId = sup.id,
                    supportName = sup.name,
                    supportType = sup.type,
                    position = sup.position,
                    rx = rx,
                    ry = ry,
                    m = mEnc
                )
            )

            steps.add(
                StepItem(
                    stepNumber = 3,
                    title = "Équations d'équilibre statique (Console)",
                    formula = "\\sum F_x = 0, \\quad \\sum F_y = 0, \\quad \\sum M_{/A} = 0",
                    substitution = "R_{Ax} + (%.2f) = 0 \\implies R_{Ax} = %.2f \\text{ kN}\nR_{Ay} - (%.2f) = 0 \\implies R_{Ay} = %.2f \\text{ kN}\nM_{A} + (%.2f) = 0 \\implies M_{A} = %.2f \\text{ kN}\\cdot\\text{m}".format(
                        Locale.US, sumFx, rx, sumFy, ry, sumMomentAboutA, mEnc
                    ),
                    result = "R_{Ay} = %.2f \\text{ kN}, \\quad M_A = %.2f \\text{ kN}\\cdot\\text{m}".format(
                        Locale.US, ry, mEnc
                    ),
                    unit = "kN / kN·m",
                    details = "L'encastrement en ${sup.name} bloque les translations x, y et la rotation. Le moment d'encastrement équilibre le couple exercé par les charges."
                )
            )

            return EquilibriumResult(
                isDeterminate = true,
                isMechanicallyStable = true,
                statusMessage = "Structure isostatique résolue avec succès.",
                reactions = reactions,
                steps = steps
            )
        }

        // CASE 2: Two supports (Pinned + Roller, or 2 Rollers with vertical loads)
        if (supports.size == 2) {
            val supA = supports[0]
            val supB = supports[1]
            val xA = supA.position
            val xB = supB.position
            val span = xB - xA

            if (abs(span) < EPS) {
                return EquilibriumResult(
                    isDeterminate = false,
                    isMechanicallyStable = false,
                    statusMessage = "Erreur : les deux appuis sont situés à la même position (x = $xA m).",
                    reactions = emptyList(),
                    steps = emptyList()
                )
            }

            // Horizontal reaction
            val rxA = if (supA.type == SupportType.PINNED) -sumFx else 0.0
            val rxB = if (supB.type == SupportType.PINNED && supA.type != SupportType.PINNED) -sumFx else 0.0

            // Moment of all external loads about pivot A:
            // Downward force at x exerts clockwise moment: -fy * (x - xA)
            // Upward reaction R_By at xB exerts counter-clockwise moment: +R_By * (xB - xA)
            // sum M/A = 0 => R_By * (xB - xA) + sum M_ext/A = 0
            // => R_By = -sum M_ext/A / (xB - xA)
            var sumMomentExtAboutA = 0.0
            for (res in resultants) {
                if (res.type == LoadType.MOMENT) {
                    sumMomentExtAboutA += res.momentAboutZero
                } else {
                    sumMomentExtAboutA += -res.fy * (res.centroidX - xA)
                }
            }

            val rBy = -sumMomentExtAboutA / span
            val rAy = sumFy - rBy

            reactions.add(
                SupportReaction(
                    supportId = supA.id,
                    supportName = supA.name,
                    supportType = supA.type,
                    position = xA,
                    rx = rxA,
                    ry = rAy,
                    m = 0.0
                )
            )

            reactions.add(
                SupportReaction(
                    supportId = supB.id,
                    supportName = supB.name,
                    supportType = supB.type,
                    position = xB,
                    rx = rxB,
                    ry = rBy,
                    m = 0.0
                )
            )

            steps.add(
                StepItem(
                    stepNumber = 3,
                    title = "Équilibre des moments par rapport à l'appui ${supA.name} (x = %.2f m)".format(Locale.US, xA),
                    formula = "\\sum M_{/%s} = 0 \\implies R_{%sy} \\cdot (x_%s - x_%s) + \\sum M_{\\text{ext}/%s} = 0".format(
                        supA.name, supB.name, supB.name, supA.name, supA.name
                    ),
                    substitution = "R_{%sy} \\cdot (%.2f - %.2f) + (%.2f) = 0\n\\implies R_{%sy} \\cdot %.2f = %.2f".format(
                        Locale.US, supB.name, xB, xA, sumMomentExtAboutA, supB.name, span, -sumMomentExtAboutA
                    ),
                    result = "R_{%sy} = %.2f".format(Locale.US, supB.name, rBy),
                    unit = "kN",
                    details = "Le moment résultant des forces extérieures par rapport à ${supA.name} vaut %.2f kN·m.".format(
                        Locale.US, sumMomentExtAboutA
                    )
                )
            )

            steps.add(
                StepItem(
                    stepNumber = 4,
                    title = "Équilibre des forces verticales (\\sum F_y = 0)",
                    formula = "R_{%sy} + R_{%sy} - \\sum F_{y,\\text{ext}} = 0".format(supA.name, supB.name),
                    substitution = "R_{%sy} + %.2f - %.2f = 0 \\implies R_{%sy} = %.2f - %.2f".format(
                        Locale.US, supA.name, rBy, sumFy, supA.name, sumFy, rBy
                    ),
                    result = "R_{%sy} = %.2f".format(Locale.US, supA.name, rAy),
                    unit = "kN",
                    details = "Vérification croisée de la seconde réaction d'appui verticale."
                )
            )

            if (abs(sumFx) > EPS || supA.type == SupportType.PINNED || supB.type == SupportType.PINNED) {
                steps.add(
                    StepItem(
                        stepNumber = 5,
                        title = "Équilibre des forces horizontales (\\sum F_x = 0)",
                        formula = "R_{Ax} + \\sum F_{x,\\text{ext}} = 0",
                        substitution = "R_{Ax} + (%.2f) = 0".format(Locale.US, sumFx),
                        result = "R_{Ax} = %.2f".format(Locale.US, rxA),
                        unit = "kN",
                        details = "Reprise de l'effort axial par l'appui articulé ${supA.name}."
                    )
                )
            }

            return EquilibriumResult(
                isDeterminate = true,
                isMechanicallyStable = true,
                statusMessage = "Structure isostatique résolue avec succès.",
                reactions = reactions,
                steps = steps
            )
        }

        return EquilibriumResult(
            isDeterminate = false,
            isMechanicallyStable = false,
            statusMessage = "Configuration non supportée en Phase 1 (isostatique classique à 1 ou 2 appuis requise).",
            reactions = emptyList(),
            steps = steps
        )
    }
}
