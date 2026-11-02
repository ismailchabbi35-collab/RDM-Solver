package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

object TrussSolver {

    private const val EPS = 1e-4

    /**
     * Solves a 2D planar truss structure completely using the Method of Joints / Direct Matrix Equilibrium.
     */
    fun solve(truss: TrussStructure): TrussSolution {
        val steps = mutableListOf<StepItem>()
        val checks = mutableListOf<EngineeringCheck>()

        val nodes = truss.nodes
        val bars = truss.bars
        val jCount = nodes.size
        val bCount = bars.size

        if (jCount == 0 || bCount == 0) {
            return TrussSolution(
                structure = truss,
                reactions = emptyList(),
                barResults = emptyList(),
                steps = listOf(StepItem(1, "Structure vide", "", "", "0 nœuds", "-")),
                checks = emptyList(),
                isIsostatic = false,
                isStable = false,
                statusMessage = "Treillis vide : aucun nœud ou aucune barre définie."
            )
        }

        // Count reaction components r
        var rCount = 0
        for (node in nodes) {
            when (node.support) {
                SupportType.PINNED -> rCount += 2
                SupportType.ROLLER -> rCount += 1
                SupportType.FIXED -> rCount += 3
                null -> {}
            }
        }

        // Isostatic condition check: b + r = 2j
        val equationCount = 2 * jCount
        val unknownCount = bCount + rCount
        val staticDeterminacy = unknownCount - equationCount

        val isIsostatic = (staticDeterminacy == 0)
        val isStable = (unknownCount >= equationCount)

        steps.add(
            StepItem(
                stepNumber = 1,
                title = "Vérification de l'isostaticité du treillis (Mécanique des structures)",
                formula = "b + r = 2 j \\implies h = (b + r) - 2 j",
                substitution = "b = $bCount \\text{ barres}, \\quad r = $rCount \\text{ réactions}, \\quad j = $jCount \\text{ nœuds}\nh = ($bCount + $rCount) - 2 \\times $jCount = $staticDeterminacy",
                result = if (isIsostatic) "Treillis Isostatique (h = 0)" else if (staticDeterminacy > 0) "Treillis Hyperstatique (h = $staticDeterminacy)" else "Treillis Instable / Mécanisme (h = $staticDeterminacy)",
                unit = "-",
                details = "Dans un treillis plan articulé, chaque nœud offre 2 équations de projection (ΣFx=0, ΣFy=0). Pour une résolution isostatique, le nombre d'inconnues (barres + réactions) doit égaler 2×j."
            )
        )

        if (!isStable) {
            return TrussSolution(
                structure = truss,
                reactions = emptyList(),
                barResults = emptyList(),
                steps = steps,
                checks = listOf(
                    EngineeringCheck(
                        name = "Condition de stabilité globale (b + r ≥ 2j)",
                        isPassed = false,
                        details = "Instable : b + r = $unknownCount < 2j = $equationCount",
                        tolerance = "b + r >= 2j"
                    )
                ),
                isIsostatic = false,
                isStable = false,
                statusMessage = "Treillis instable (mécanisme) : il manque ${-staticDeterminacy} barre(s) ou réaction(s) d'appui."
            )
        }

        // Solve global reactions first via moment equilibrium about origin or pivot
        // Map nodes to matrix indices
        val nodeIndexMap = nodes.mapIndexed { idx, n -> n.id to idx }.toMap()

        // Build 2j x 2j linear system for direct solution
        // Unknown vector X = [N_1, N_2, ..., N_b, R_1, R_2, ..., R_r]
        val totalUnknowns = bCount + rCount
        val matrix = Array(equationCount) { DoubleArray(totalUnknowns + 1) }

        // Fill equations for each node j_k
        // Equation 2*k: Sum Fx = 0
        // Equation 2*k + 1: Sum Fy = 0
        bars.forEachIndexed { bIdx, bar ->
            val startNode = nodes.firstOrNull { it.id == bar.nodeStartId }
            val endNode = nodes.firstOrNull { it.id == bar.nodeEndId }

            if (startNode != null && endNode != null) {
                val dx = endNode.x - startNode.x
                val dy = endNode.y - startNode.y
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6)
                val cosA = dx / len
                val sinA = dy / len

                val sIdx = nodeIndexMap[startNode.id]!!
                val eIdx = nodeIndexMap[endNode.id]!!

                // Tension convention: Bar pulls away from node
                // At start node (force directed towards endNode): +cosA, +sinA
                matrix[2 * sIdx][bIdx] += cosA
                matrix[2 * sIdx + 1][bIdx] += sinA

                // At end node (force directed towards startNode): -cosA, -sinA
                matrix[2 * eIdx][bIdx] -= cosA
                matrix[2 * eIdx + 1][bIdx] -= sinA
            }
        }

        // Reaction columns
        var rColIndex = bCount
        val reactionMap = mutableListOf<Triple<String, String, Char>>() // (nodeId, supportName, direction 'X' or 'Y')

        nodes.forEachIndexed { nIdx, node ->
            when (node.support) {
                SupportType.PINNED -> {
                    // Rx at node
                    matrix[2 * nIdx][rColIndex] = 1.0
                    reactionMap.add(Triple(node.id, node.name, 'X'))
                    rColIndex++

                    // Ry at node
                    matrix[2 * nIdx + 1][rColIndex] = 1.0
                    reactionMap.add(Triple(node.id, node.name, 'Y'))
                    rColIndex++
                }
                SupportType.ROLLER -> {
                    // Ry at node
                    matrix[2 * nIdx + 1][rColIndex] = 1.0
                    reactionMap.add(Triple(node.id, node.name, 'Y'))
                    rColIndex++
                }
                SupportType.FIXED -> {
                    matrix[2 * nIdx][rColIndex] = 1.0
                    reactionMap.add(Triple(node.id, node.name, 'X'))
                    rColIndex++

                    matrix[2 * nIdx + 1][rColIndex] = 1.0
                    reactionMap.add(Triple(node.id, node.name, 'Y'))
                    rColIndex++
                }
                null -> {}
            }

            // External applied loads on right hand side (RHS)
            // Equation is: internal + reaction + F_ext = 0 => internal + reaction = -F_ext
            matrix[2 * nIdx][totalUnknowns] = -node.fx_kN
            matrix[2 * nIdx + 1][totalUnknowns] = -node.fy_kN
        }

        // Solve system using Gaussian Elimination with Partial Pivoting
        val solutionVec = solveGaussian(matrix, equationCount, totalUnknowns)

        if (solutionVec == null) {
            return TrussSolution(
                structure = truss,
                reactions = emptyList(),
                barResults = emptyList(),
                steps = steps,
                checks = emptyList(),
                isIsostatic = isIsostatic,
                isStable = false,
                statusMessage = "Singularité géométrique : le treillis est instable (mécanisme géométrique)."
            )
        }

        // Extract Bar Forces and Reactions
        val barResults = mutableListOf<BarForceResult>()
        val reactionsList = mutableListOf<SupportReaction>()

        // Group reactions by support node
        val reactionValues = mutableMapOf<String, Pair<Double, Double>>() // nodeId -> (Rx, Ry)

        for (i in 0 until rCount) {
            val rVal = solutionVec[bCount + i]
            val (nId, sName, dir) = reactionMap[i]
            val current = reactionValues.getOrDefault(nId, Pair(0.0, 0.0))
            if (dir == 'X') {
                reactionValues[nId] = Pair(rVal, current.second)
            } else {
                reactionValues[nId] = Pair(current.first, rVal)
            }
        }

        nodes.filter { it.support != null }.forEach { node ->
            val (rx, ry) = reactionValues.getOrDefault(node.id, Pair(0.0, 0.0))
            reactionsList.add(
                SupportReaction(
                    supportId = node.id,
                    supportName = node.name,
                    supportType = node.support!!,
                    position = node.x,
                    rx = rx,
                    ry = ry,
                    m = 0.0
                )
            )
        }

        steps.add(
            StepItem(
                stepNumber = 2,
                title = "Calcul des réactions d'appui aux nœuds",
                formula = "\\sum F_x = 0, \\quad \\sum F_y = 0",
                substitution = reactionsList.joinToString("\n") {
                    "Nœud ${it.supportName} (${it.supportType.labelFr}) : R_{x} = %.2f \\text{ kN}, \\quad R_{y} = %.2f \\text{ kN}".format(
                        Locale.US, it.rx, it.ry
                    )
                },
                result = "Réactions globales calculées",
                unit = "kN"
            )
        )

        // Process each member force
        var zeroForceCount = 0
        bars.forEachIndexed { idx, bar ->
            val fVal = solutionVec[idx]
            val sNode = nodes.first { it.id == bar.nodeStartId }
            val eNode = nodes.first { it.id == bar.nodeEndId }
            val dx = eNode.x - sNode.x
            val dy = eNode.y - sNode.y
            val len = sqrt(dx * dx + dy * dy)

            val state = when {
                abs(fVal) < EPS -> {
                    zeroForceCount++
                    TrussBarState.ZERO_FORCE
                }
                fVal > 0 -> TrussBarState.TRACTION
                else -> TrussBarState.COMPRESSION
            }

            // Stress sigma = N / A (N in kN => 1000 N, A in cm² => 100 mm² => N/A in N/mm² = MPa)
            val area_mm2 = bar.area_cm2 * 100.0
            val stress_MPa = (fVal * 1000.0) / area_mm2
            val util = abs(stress_MPa) / truss.material.yieldStrengthFy_MPa
            val safe = util <= 1.0

            barResults.add(
                BarForceResult(
                    barId = bar.id,
                    barName = bar.name,
                    nodeStartName = sNode.name,
                    nodeEndName = eNode.name,
                    length_m = len,
                    force_kN = fVal,
                    state = state,
                    stress_MPa = stress_MPa,
                    utilizationRatio = util,
                    isSafe = safe
                )
            )
        }

        steps.add(
            StepItem(
                stepNumber = 3,
                title = "Résolution des efforts axiaux par la méthode des nœuds (N_i)",
                formula = "\\sum F_{x, \\text{nœud}} = 0, \\quad \\sum F_{y, \\text{nœud}} = 0",
                substitution = "Nombre total de barres : $bCount\n• Barres en Traction (+) : ${barResults.count { it.state == TrussBarState.TRACTION }}\n• Barres en Compression (-) : ${barResults.count { it.state == TrussBarState.COMPRESSION }}\n• Barres Nulles (0) : $zeroForceCount",
                result = "Efforts axiaux déterminés",
                unit = "kN",
                details = "Convention : N > 0 désigne un effort de traction (la barre tire sur le nœud) ; N < 0 désigne de la compression."
            )
        )

        // Verification checks
        // 1. Global X equilibrium
        val totalFxExt = nodes.sumOf { it.fx_kN }
        val totalRx = reactionsList.sumOf { it.rx }
        val passFx = abs(totalFxExt + totalRx) < EPS
        checks.add(
            EngineeringCheck(
                name = "Équilibre global horizontal du treillis (\\sum F_x = 0)",
                isPassed = passFx,
                details = "Résidu : %.4f kN".format(Locale.US, totalFxExt + totalRx),
                tolerance = "< 1e-3 kN"
            )
        )

        // 2. Global Y equilibrium
        val totalFyExt = nodes.sumOf { it.fy_kN }
        val totalRy = reactionsList.sumOf { it.ry }
        val passFy = abs(totalFyExt + totalRy) < EPS
        checks.add(
            EngineeringCheck(
                name = "Équilibre global vertical du treillis (\\sum F_y = 0)",
                isPassed = passFy,
                details = "Résidu : %.4f kN".format(Locale.US, totalFyExt + totalRy),
                tolerance = "< 1e-3 kN"
            )
        )

        // 3. Resistance check
        val allBarsSafe = barResults.all { it.isSafe }
        val maxUtilBar = barResults.maxByOrNull { it.utilizationRatio }
        checks.add(
            EngineeringCheck(
                name = "Résistance des barres au flambement / traction (ELU)",
                isPassed = allBarsSafe,
                details = "Taux maximal : %.1f\\%% sur la barre ${maxUtilBar?.barName ?: "-"} (\\sigma = %.2f \\text{ MPa})".format(
                    Locale.US, (maxUtilBar?.utilizationRatio ?: 0.0) * 100.0, maxUtilBar?.stress_MPa ?: 0.0
                ),
                tolerance = "\\eta \\le 100\\%"
            )
        )

        return TrussSolution(
            structure = truss,
            reactions = reactionsList,
            barResults = barResults,
            steps = steps,
            checks = checks,
            isIsostatic = isIsostatic,
            isStable = true,
            statusMessage = "Treillis résolu avec succès par la méthode des nœuds."
        )
    }

    /**
     * Gaussian elimination solver for matrix system A * X = B.
     */
    private fun solveGaussian(matrix: Array<DoubleArray>, rows: Int, cols: Int): DoubleArray? {
        if (rows < cols) return null

        val mat = Array(rows) { r -> matrix[r].clone() }

        var pivotRow = 0
        for (col in 0 until cols) {
            // Find pivot
            var maxRow = pivotRow
            var maxVal = abs(mat[pivotRow][col])
            for (r in pivotRow + 1 until rows) {
                if (abs(mat[r][col]) > maxVal) {
                    maxVal = abs(mat[r][col])
                    maxRow = r
                }
            }

            if (maxVal < 1e-8) {
                // Column is zero / singular
                continue
            }

            // Swap pivot row
            val temp = mat[pivotRow]
            mat[pivotRow] = mat[maxRow]
            mat[maxRow] = temp

            // Normalize pivot row
            val pivot = mat[pivotRow][col]
            for (c in col..cols) {
                mat[pivotRow][c] /= pivot
            }

            // Eliminate lower and upper entries
            for (r in 0 until rows) {
                if (r != pivotRow) {
                    val factor = mat[r][col]
                    if (abs(factor) > 1e-12) {
                        for (c in col..cols) {
                            mat[r][c] -= factor * mat[pivotRow][c]
                        }
                    }
                }
            }
            pivotRow++
            if (pivotRow >= rows) break
        }

        val result = DoubleArray(cols)
        for (c in 0 until cols) {
            result[c] = mat[c][cols]
        }
        return result
    }
}
