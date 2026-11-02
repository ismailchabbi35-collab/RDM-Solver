package com.example.data

/**
 * Types of structural supports for beams in French RDM convention.
 */
enum class SupportType(val labelFr: String, val labelAr: String, val labelEn: String) {
    ROLLER("Appui simple (Rouleau)", "مسند بسيط (متدحرج)", "Roller support"),
    PINNED("Articulation (Appui fixe)", "مسند مفصلي (ثابت)", "Pinned support"),
    FIXED("Encastrement", "تثبيت تام (وثاقة)", "Fixed support / Cantilever")
}

/**
 * Structural support definition.
 * @param id Unique identifier
 * @param name Label (e.g., "A", "B")
 * @param type ROLLER (Ry), PINNED (Rx, Ry), FIXED (Rx, Ry, M)
 * @param position Abscissa x in meters [0, L]
 */
data class Support(
    val id: String,
    val name: String,
    val type: SupportType,
    val position: Double
)

/**
 * Types of external loads applied to the beam.
 */
enum class LoadType(val labelFr: String, val labelAr: String, val labelEn: String) {
    POINT_FORCE("Force ponctuelle", "قوة مركزة", "Point load"),
    DISTRIBUTED_UNIFORM("Charge uniforme répartie", "حمولة موزعة بانتظام", "Uniform distributed load"),
    DISTRIBUTED_TRIANGULAR("Charge triangulaire", "حمولة مثلثية", "Triangular load"),
    MOMENT("Couple / Moment ponctuel", "عزم مركز", "Concentrated moment")
}

/**
 * Applied load representation.
 * @param id Unique identifier
 * @param type Load type
 * @param positionStart Start abscissa x1 in meters
 * @param positionEnd End abscissa x2 in meters (for distributed loads)
 * @param magnitude Magnitude in kN (for forces), kN/m (for distributed), kN·m (for moments)
 *                  Convention: Positive = downward for vertical forces, clockwise for moments.
 * @param magnitudeEnd End magnitude in kN/m for triangular/trapezoidal loads
 * @param angleDegrees Angle relative to beam axis (90° = vertical downward)
 */
data class Load(
    val id: String,
    val type: LoadType,
    val positionStart: Double,
    val positionEnd: Double = positionStart,
    val magnitude: Double,
    val magnitudeEnd: Double = magnitude,
    val angleDegrees: Double = 90.0
)

/**
 * Entire beam structure representation.
 */
data class BeamStructure(
    val id: String = "beam_01",
    val name: String = "Poutre principale",
    val length: Double = 6.0,
    val supports: List<Support> = emptyList(),
    val loads: List<Load> = emptyList(),
    val section: CrossSection = CrossSection(),
    val material: Material = Material.STEEL_S235
)
