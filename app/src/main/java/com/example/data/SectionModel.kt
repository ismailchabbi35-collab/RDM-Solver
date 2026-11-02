package com.example.data

/**
 * Geometric shape types for beam cross-sections.
 */
enum class SectionShapeType(val labelFr: String, val labelAr: String, val labelEn: String) {
    RECTANGLE("Section Rectangulaire", "مقطع مستطيل", "Rectangular section"),
    I_BEAM("Profilé en I (IPE / HEA)", "مقطع على شكل I", "I-beam profile"),
    T_BEAM("Profilé en T", "مقطع على شكل T", "T-beam profile"),
    CIRCLE("Section Circulaire", "مقطع دائري", "Circular section"),
    HOLLOW_BOX("Section Caisson Creux", "مقطع صندوقي مفرغ", "Hollow box section")
}

/**
 * Material definition with mechanical properties.
 */
data class Material(
    val name: String = "Acier S235",
    val youngModulusE_GPa: Double = 210.0, // GPa
    val yieldStrengthFy_MPa: Double = 235.0, // MPa (N/mm²)
    val shearModulusG_GPa: Double = 80.0, // GPa
    val poissonRatio: Double = 0.3
) {
    companion object {
        val STEEL_S235 = Material("Acier S235", 210.0, 235.0)
        val STEEL_S355 = Material("Acier S355", 210.0, 355.0)
        val TIMBER_C24 = Material("Bois Résineux C24", 11.0, 24.0)
        val CONCRETE_C25 = Material("Béton C25/30", 31.0, 25.0)
        val ALUMINUM_6061 = Material("Aluminium 6061-T6", 70.0, 240.0)

        val ALL_MATERIALS = listOf(STEEL_S235, STEEL_S355, TIMBER_C24, CONCRETE_C25, ALUMINUM_6061)
    }
}

/**
 * Geometric cross-section definition.
 * Dimensions stored in millimeters (mm) for standard engineering convention.
 */
data class CrossSection(
    val shapeType: SectionShapeType = SectionShapeType.RECTANGLE,
    val name: String = "Section Poutre",
    // Rectangle / Overall dimensions (mm)
    val width_mm: Double = 150.0,
    val height_mm: Double = 300.0,
    // I-beam / T-beam specific dimensions (mm)
    val flangeThickness_mm: Double = 10.0,
    val webThickness_mm: Double = 6.0,
    // Circle specific (mm)
    val diameter_mm: Double = 200.0,
    // Hollow box inner dimensions (mm)
    val innerWidth_mm: Double = 120.0,
    val innerHeight_mm: Double = 260.0
)

/**
 * Evaluated geometric properties of the section.
 */
data class SectionProperties(
    val area_cm2: Double,               // cm²
    val totalHeight_mm: Double,         // mm
    val centroidY_inf_mm: Double,       // mm from bottom to neutral axis (y_inf)
    val centroidY_sup_mm: Double,       // mm from neutral axis to top (y_sup)
    val inertiaIz_cm4: Double,          // cm⁴
    val modulusWel_sup_cm3: Double,     // cm³ (I / y_sup)
    val modulusWel_inf_cm3: Double,     // cm³ (I / y_inf)
    val staticMomentMax_cm3: Double,    // cm³ (S_max at neutral axis)
    val widthAtNeutralAxis_mm: Double   // mm
)

/**
 * Calculated stress state at a specific section or at maximum envelope.
 */
data class StressResult(
    val sectionX: Double,               // m
    val normalForce_kN: Double,         // kN
    val shearForce_kN: Double,          // kN
    val bendingMoment_kNm: Double,      // kN·m
    val normalStressAxial_MPa: Double,  // N/A in MPa
    val normalStressSup_MPa: Double,    // σ at top fibers in MPa (compression if negative)
    val normalStressInf_MPa: Double,    // σ at bottom fibers in MPa (tension if positive)
    val maxNormalStress_MPa: Double,    // max(|σ_sup|, |σ_inf|) in MPa
    val maxShearStress_MPa: Double,     // τ_max at neutral axis in MPa
    val vonMisesStress_MPa: Double,     // sqrt(σ² + 3τ²) in MPa
    val utilizationRatio: Double,       // σ_VM / f_y (e.g. 0.65 = 65%)
    val isSafe: Boolean                 // utilizationRatio <= 1.0
)
