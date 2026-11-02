package com.example.data

/**
 * Result of the deflection and elastic curve calculations (Phase 3).
 */
data class DeflectionResult(
    val maxDeflection_mm: Double,       // Maximum downward deflection in mm
    val maxDeflectionX_m: Double,       // Abscissa x of maximum deflection in meters
    val maxRotation_mrad: Double,       // Maximum cross-section rotation in mrad
    val maxRotationX_m: Double,         // Abscissa x of maximum rotation in meters
    val deflectionLimit_mm: Double,     // Permissible deflection limit (e.g. L / 500)
    val limitRatioDenominator: Int = 500, // Default Eurocode limit L / 500
    val isElsCompliant: Boolean,        // maxDeflection_mm <= deflectionLimit_mm
    val elsUtilizationRatio: Double,    // maxDeflection_mm / deflectionLimit_mm
    val deflectionPoints: List<DiagramPoint>, // Displacements v(x) in mm
    val rotationPoints: List<DiagramPoint>    // Rotations theta(x) in mrad
)
