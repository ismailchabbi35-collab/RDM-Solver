package com.example.data

/**
 * State of axial force in a truss member.
 */
enum class TrussBarState(val labelFr: String, val labelAr: String, val labelEn: String) {
    TRACTION("Traction (+)", "شد (+)", "Tension (+)"),
    COMPRESSION("Compression (-)", "ضغط (-)", "Compression (-)"),
    ZERO_FORCE("Barre Nulle (0)", "عنصر خالي (0)", "Zero-force member (0)")
}

/**
 * Joint/Node in a 2D planar truss structure.
 */
data class TrussNode(
    val id: String,
    val name: String,
    val x: Double, // meters
    val y: Double, // meters
    val support: SupportType? = null,
    val fx_kN: Double = 0.0, // External force X (kN, positive to the right)
    val fy_kN: Double = 0.0  // External force Y (kN, positive upwards)
)

/**
 * Member/Bar connecting two joints in a 2D truss.
 */
data class TrussBar(
    val id: String,
    val name: String,
    val nodeStartId: String,
    val nodeEndId: String,
    val area_cm2: Double = 15.0 // Cross-sectional area in cm²
)

/**
 * Complete 2D planar truss structure definition.
 */
data class TrussStructure(
    val id: String = "truss_01",
    val name: String = "Ferme de Toiture (Treillis)",
    val nodes: List<TrussNode> = emptyList(),
    val bars: List<TrussBar> = emptyList(),
    val material: Material = Material.STEEL_S235
)

/**
 * Computed axial force and stress result in a single truss member.
 */
data class BarForceResult(
    val barId: String,
    val barName: String,
    val nodeStartName: String,
    val nodeEndName: String,
    val length_m: Double,
    val force_kN: Double, // Positive = Tension, Negative = Compression
    val state: TrussBarState,
    val stress_MPa: Double, // N / A_mm²
    val utilizationRatio: Double, // |sigma| / f_y
    val isSafe: Boolean
)

/**
 * Full solution returned by the Truss Solver.
 */
data class TrussSolution(
    val structure: TrussStructure,
    val reactions: List<SupportReaction>,
    val barResults: List<BarForceResult>,
    val steps: List<StepItem>,
    val checks: List<EngineeringCheck>,
    val isIsostatic: Boolean,
    val isStable: Boolean,
    val statusMessage: String
)
