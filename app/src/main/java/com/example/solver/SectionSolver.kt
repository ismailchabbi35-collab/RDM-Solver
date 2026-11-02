package com.example.solver

import com.example.data.*
import java.util.Locale
import kotlin.math.*

object SectionSolver {

    private const val EPS = 1e-6

    /**
     * Compute geometric properties of the cross-section (A, y_G, I_z, W_el, S_max).
     */
    fun computeProperties(section: CrossSection): SectionProperties {
        return when (section.shapeType) {
            SectionShapeType.RECTANGLE -> {
                val b = section.width_mm
                val h = section.height_mm
                val area_mm2 = b * h
                val y_inf = h / 2.0
                val y_sup = h / 2.0
                val iz_mm4 = (b * h * h * h) / 12.0
                val sMax_mm3 = (b * h * h) / 8.0 // static moment above neutral axis

                SectionProperties(
                    area_cm2 = area_mm2 / 100.0,
                    totalHeight_mm = h,
                    centroidY_inf_mm = y_inf,
                    centroidY_sup_mm = y_sup,
                    inertiaIz_cm4 = iz_mm4 / 10000.0,
                    modulusWel_sup_cm3 = (iz_mm4 / y_sup) / 1000.0,
                    modulusWel_inf_cm3 = (iz_mm4 / y_inf) / 1000.0,
                    staticMomentMax_cm3 = sMax_mm3 / 1000.0,
                    widthAtNeutralAxis_mm = b
                )
            }
            SectionShapeType.I_BEAM -> {
                val b = section.width_mm
                val h = section.height_mm
                val tf = section.flangeThickness_mm
                val tw = section.webThickness_mm
                val hw = h - 2.0 * tf

                val areaFlanges_mm2 = 2.0 * b * tf
                val areaWeb_mm2 = tw * max(0.0, hw)
                val totalArea_mm2 = areaFlanges_mm2 + areaWeb_mm2

                val y_inf = h / 2.0
                val y_sup = h / 2.0

                // Iz = (b * h^3 - (b - tw) * hw^3) / 12
                val iz_mm4 = (b * h.pow(3) - (b - tw) * hw.coerceAtLeast(0.0).pow(3)) / 12.0

                // Static moment above neutral axis:
                // S_flange = (b * tf) * (h/2 - tf/2)
                // S_web_half = (tw * (h/2 - tf)) * ((h/2 - tf) / 2)
                val sFlange = (b * tf) * (h / 2.0 - tf / 2.0)
                val halfWebH = max(0.0, h / 2.0 - tf)
                val sWebHalf = (tw * halfWebH) * (halfWebH / 2.0)
                val sMax_mm3 = sFlange + sWebHalf

                SectionProperties(
                    area_cm2 = totalArea_mm2 / 100.0,
                    totalHeight_mm = h,
                    centroidY_inf_mm = y_inf,
                    centroidY_sup_mm = y_sup,
                    inertiaIz_cm4 = iz_mm4 / 10000.0,
                    modulusWel_sup_cm3 = (iz_mm4 / y_sup) / 1000.0,
                    modulusWel_inf_cm3 = (iz_mm4 / y_inf) / 1000.0,
                    staticMomentMax_cm3 = sMax_mm3 / 1000.0,
                    widthAtNeutralAxis_mm = tw
                )
            }
            SectionShapeType.T_BEAM -> {
                val bf = section.width_mm
                val tf = section.flangeThickness_mm
                val tw = section.webThickness_mm
                val h = section.height_mm
                val hw = max(0.0, h - tf)

                // Sub-elements: 1 = Web (bottom), 2 = Flange (top)
                val aWeb = tw * hw
                val yWeb = hw / 2.0
                val aFlange = bf * tf
                val yFlange = hw + tf / 2.0

                val totalArea_mm2 = aWeb + aFlange
                val y_inf = (aWeb * yWeb + aFlange * yFlange) / totalArea_mm2
                val y_sup = h - y_inf

                // Huygens theorem: Iz = sum(Iz0 + A * d^2)
                val izWeb = (tw * hw.pow(3)) / 12.0 + aWeb * (yWeb - y_inf).pow(2)
                val izFlange = (bf * tf.pow(3)) / 12.0 + aFlange * (yFlange - y_inf).pow(2)
                val iz_mm4 = izWeb + izFlange

                // Static moment above neutral axis:
                val sMax_mm3 = if (y_inf <= hw) {
                    // Neutral axis cuts the web
                    val webAbove = hw - y_inf
                    val sFlangePart = aFlange * (yFlange - y_inf)
                    val sWebPart = (tw * webAbove) * (webAbove / 2.0)
                    sFlangePart + sWebPart
                } else {
                    // Neutral axis in flange
                    val flangeAbove = h - y_inf
                    (bf * flangeAbove) * (flangeAbove / 2.0)
                }

                val widthAtNA = if (y_inf <= hw) tw else bf

                SectionProperties(
                    area_cm2 = totalArea_mm2 / 100.0,
                    totalHeight_mm = h,
                    centroidY_inf_mm = y_inf,
                    centroidY_sup_mm = y_sup,
                    inertiaIz_cm4 = iz_mm4 / 10000.0,
                    modulusWel_sup_cm3 = (iz_mm4 / y_sup) / 1000.0,
                    modulusWel_inf_cm3 = (iz_mm4 / y_inf) / 1000.0,
                    staticMomentMax_cm3 = sMax_mm3 / 1000.0,
                    widthAtNeutralAxis_mm = widthAtNA
                )
            }
            SectionShapeType.CIRCLE -> {
                val d = section.diameter_mm
                val r = d / 2.0
                val area_mm2 = PI * r * r
                val y_inf = r
                val y_sup = r
                val iz_mm4 = (PI * d.pow(4)) / 64.0
                // Static moment for half circle: S = 2/3 * r^3
                val sMax_mm3 = (2.0 / 3.0) * r.pow(3)

                SectionProperties(
                    area_cm2 = area_mm2 / 100.0,
                    totalHeight_mm = d,
                    centroidY_inf_mm = y_inf,
                    centroidY_sup_mm = y_sup,
                    inertiaIz_cm4 = iz_mm4 / 10000.0,
                    modulusWel_sup_cm3 = (iz_mm4 / y_sup) / 1000.0,
                    modulusWel_inf_cm3 = (iz_mm4 / y_inf) / 1000.0,
                    staticMomentMax_cm3 = sMax_mm3 / 1000.0,
                    widthAtNeutralAxis_mm = d
                )
            }
            SectionShapeType.HOLLOW_BOX -> {
                val b = section.width_mm
                val h = section.height_mm
                val bIn = section.innerWidth_mm.coerceAtMost(b - 2.0)
                val hIn = section.innerHeight_mm.coerceAtMost(h - 2.0)

                val area_mm2 = (b * h) - (bIn * hIn)
                val y_inf = h / 2.0
                val y_sup = h / 2.0
                val iz_mm4 = (b * h.pow(3) - bIn * hIn.pow(3)) / 12.0
                val sMax_mm3 = ((b * h.pow(2)) - (bIn * hIn.pow(2))) / 8.0
                val twTotal = b - bIn // two webs

                SectionProperties(
                    area_cm2 = area_mm2 / 100.0,
                    totalHeight_mm = h,
                    centroidY_inf_mm = y_inf,
                    centroidY_sup_mm = y_sup,
                    inertiaIz_cm4 = iz_mm4 / 10000.0,
                    modulusWel_sup_cm3 = (iz_mm4 / y_sup) / 1000.0,
                    modulusWel_inf_cm3 = (iz_mm4 / y_inf) / 1000.0,
                    staticMomentMax_cm3 = sMax_mm3 / 1000.0,
                    widthAtNeutralAxis_mm = twTotal
                )
            }
        }
    }

    /**
     * Compute stress state (sigma, tau, Von Mises) at a given section.
     */
    fun computeStresses(
        sectionX: Double,
        n_kN: Double,
        t_kN: Double,
        m_kNm: Double,
        props: SectionProperties,
        material: Material
    ): StressResult {
        // Conversions to SI / Engineering Units:
        // N in N: n_kN * 1e3
        // M in N·mm: m_kNm * 1e6
        // Area in mm²: props.area_cm2 * 100.0
        // Iz in mm⁴: props.inertiaIz_cm4 * 10000.0
        // S_max in mm³: props.staticMomentMax_cm3 * 1000.0
        val n_N = n_kN * 1000.0
        val m_Nmm = m_kNm * 1e6
        val t_N = abs(t_kN) * 1000.0

        val area_mm2 = props.area_cm2 * 100.0
        val iz_mm4 = props.inertiaIz_cm4 * 10000.0
        val sMax_mm3 = props.staticMomentMax_cm3 * 1000.0
        val b_na_mm = props.widthAtNeutralAxis_mm

        // Axial stress: sigma_N = N / A (MPa)
        val sigmaAxial = if (area_mm2 > EPS) n_N / area_mm2 else 0.0

        // Bending stress (Navier):
        // M > 0 = Sagging => Fibres inférieures tendues (y = -y_inf gives positive sigma)
        // sigma(y) = sigma_N + M * y_inf / Iz for bottom fibers
        // sigma(y) = sigma_N - M * y_sup / Iz for top fibers
        val sigmaBendingInf = if (iz_mm4 > EPS) (m_Nmm * props.centroidY_inf_mm) / iz_mm4 else 0.0
        val sigmaBendingSup = if (iz_mm4 > EPS) (-m_Nmm * props.centroidY_sup_mm) / iz_mm4 else 0.0

        val sigmaInf = sigmaAxial + sigmaBendingInf
        val sigmaSup = sigmaAxial + sigmaBendingSup
        val maxNormalStress = max(abs(sigmaInf), abs(sigmaSup))

        // Shear stress (Jourawski / Colignon):
        // tau_max = (T * S_max) / (Iz * b)
        val maxShear = if (iz_mm4 > EPS && b_na_mm > EPS) {
            (t_N * sMax_mm3) / (iz_mm4 * b_na_mm)
        } else {
            0.0
        }

        // Equivalent Von Mises stress:
        // At outer fibers: tau is zero, sigma is maxNormalStress => sigma_VM = maxNormalStress
        // At neutral axis: sigma is sigmaAxial, tau is maxShear => sigma_VM_center = sqrt(sigmaAxial^2 + 3 * tau^2)
        val vmOuter = maxNormalStress
        val vmCenter = sqrt(sigmaAxial.pow(2) + 3.0 * maxShear.pow(2))
        val vonMises = max(vmOuter, vmCenter)

        val fy = material.yieldStrengthFy_MPa
        val utilization = if (fy > EPS) vonMises / fy else 0.0
        val isSafe = utilization <= 1.0

        return StressResult(
            sectionX = sectionX,
            normalForce_kN = n_kN,
            shearForce_kN = t_kN,
            bendingMoment_kNm = m_kNm,
            normalStressAxial_MPa = sigmaAxial,
            normalStressSup_MPa = sigmaSup,
            normalStressInf_MPa = sigmaInf,
            maxNormalStress_MPa = maxNormalStress,
            maxShearStress_MPa = maxShear,
            vonMisesStress_MPa = vonMises,
            utilizationRatio = utilization,
            isSafe = isSafe
        )
    }

    /**
     * Generate step-by-step resolution cards for Section & Stresses (Phase 2).
     */
    fun generateSectionSteps(
        section: CrossSection,
        props: SectionProperties,
        material: Material,
        stress: StressResult,
        stepOffset: Int
    ): List<StepItem> {
        val steps = mutableListOf<StepItem>()

        // Step 1: Geometric properties and Huygens theorem
        steps.add(
            StepItem(
                stepNumber = stepOffset + 1,
                title = "Caractéristiques géométriques de la section (${section.shapeType.labelFr})",
                formula = "I_z = \\sum \\left( I_{zi} + A_i \\cdot d_i^2 \\right), \\quad W_{\\text{el}} = \\frac{I_z}{y_{\\max}}",
                substitution = "A = %.2f \\text{ cm}^2, \\quad y_G = %.1f \\text{ mm (depuis la base)}\nI_z = %.2f \\text{ cm}^4, \\quad W_{\\text{el,inf}} = %.2f \\text{ cm}^3, \\quad W_{\\text{el,sup}} = %.2f \\text{ cm}^3".format(
                    Locale.US, props.area_cm2, props.centroidY_inf_mm, props.inertiaIz_cm4, props.modulusWel_inf_cm3, props.modulusWel_sup_cm3
                ),
                result = "I_z = %.2f \\text{ cm}^4, \\quad S_{\\max} = %.2f \\text{ cm}^3".format(
                    Locale.US, props.inertiaIz_cm4, props.staticMomentMax_cm3
                ),
                unit = "cm⁴ / cm³",
                details = "Théorème de Huygens appliqué pour le calcul du moment quadratique par rapport à l'axe neutre passant par le centre de gravité G."
            )
        )

        // Step 2: Normal bending stress calculation (Navier)
        steps.add(
            StepItem(
                stepNumber = stepOffset + 2,
                title = "Calcul des contraintes normales de flexion (Formule de Navier)",
                formula = "\\sigma(y) = \\frac{N}{A} + \\frac{M \\cdot y}{I_z}",
                substitution = "M_{\\max} = %.2f \\text{ kN}\\cdot\\text{m} = %.2e \\text{ N}\\cdot\\text{mm}, \\quad I_z = %.2f \\text{ cm}^4 = %.2e \\text{ mm}^4\n\\sigma_{\\text{inf}} = \\frac{%.2e \\cdot %.1f}{%.2e} = %.2f \\text{ MPa (Traction)}\n\\sigma_{\\text{sup}} = -\\frac{%.2e \\cdot %.1f}{%.2e} = %.2f \\text{ MPa (Compression)}".format(
                    Locale.US, stress.bendingMoment_kNm, stress.bendingMoment_kNm * 1e6, props.inertiaIz_cm4, props.inertiaIz_cm4 * 10000.0,
                    stress.bendingMoment_kNm * 1e6, props.centroidY_inf_mm, props.inertiaIz_cm4 * 10000.0, stress.normalStressInf_MPa,
                    stress.bendingMoment_kNm * 1e6, props.centroidY_sup_mm, props.inertiaIz_cm4 * 10000.0, stress.normalStressSup_MPa
                ),
                result = "\\sigma_{\\max} = %.2f".format(Locale.US, stress.maxNormalStress_MPa),
                unit = "MPa (N/mm²)",
                details = "Convention RDM : pour un moment positif M > 0, les fibres inférieures sont tendues (σ > 0) et les fibres supérieures sont comprimées (σ < 0)."
            )
        )

        // Step 3: Shear stress (Jourawski / Colignon)
        steps.add(
            StepItem(
                stepNumber = stepOffset + 3,
                title = "Calcul des contraintes tangentielles de cisaillement (Jourawski / Colignon)",
                formula = "\\tau(y) = \\frac{T \\cdot S(y)}{I_z \\cdot b(y)}",
                substitution = "T = %.2f \\text{ kN} = %.0f \\text{ N}, \\quad S_{\\max} = %.2f \\text{ cm}^3 = %.0f \\text{ mm}^3\nI_z = %.2e \\text{ mm}^4, \\quad b = %.1f \\text{ mm}".format(
                    Locale.US, abs(stress.shearForce_kN), abs(stress.shearForce_kN) * 1000.0, props.staticMomentMax_cm3, props.staticMomentMax_cm3 * 1000.0,
                    props.inertiaIz_cm4 * 10000.0, props.widthAtNeutralAxis_mm
                ),
                result = "\\tau_{\\max} = %.2f".format(Locale.US, stress.maxShearStress_MPa),
                unit = "MPa (N/mm²)",
                details = "L'effort tranchant engendre un cisaillement maximal sur l'axe neutre là où le moment statique S(y) est maximal."
            )
        )

        // Step 4: Von Mises stress and Material verification check
        val statusLabel = if (stress.isSafe) "CONFORME (Taux de travail acceptable)" else "NON CONFORME (Risque de plastification / rupture)"
        steps.add(
            StepItem(
                stepNumber = stepOffset + 4,
                title = "Critère de résistance de Von Mises et vérification matière",
                formula = "\\sigma_{VM} = \\sqrt{\\sigma^2 + 3\\tau^2} \\le f_y, \\quad \\eta = \\frac{\\sigma_{VM}}{f_y} \\times 100\\%",
                substitution = "\\sigma = %.2f \\text{ MPa}, \\quad \\tau = %.2f \\text{ MPa}, \\quad f_y = %.1f \\text{ MPa (%s)}\n\\sigma_{VM} = \\sqrt{(%.2f)^2 + 3(%.2f)^2} = %.2f \\text{ MPa}".format(
                    Locale.US, stress.maxNormalStress_MPa, stress.maxShearStress_MPa, material.yieldStrengthFy_MPa, material.name,
                    stress.maxNormalStress_MPa, stress.maxShearStress_MPa, stress.vonMisesStress_MPa
                ),
                result = "\\eta = %.1f\\%% \\quad \\to \\quad %s".format(Locale.US, stress.utilizationRatio * 100.0, statusLabel),
                unit = "MPa / %",
                details = "Vérification selon l'état limite ultime (ELU). Résistance élastique garantie si le taux d'utilisation reste inférieur à 100%."
            )
        )

        return steps
    }
}
