package com.example

import com.example.data.*
import com.example.solver.RdmSolver
import org.junit.Assert.*
import org.junit.Test

class RdmSolverTest {

    private val eps = 1e-3

    @Test
    fun testSimplySupportedBeamWithUdl() {
        val beam = BeamPresets.simplySupportedUdl // L=6m, q=12 kN/m
        val solution = RdmSolver.solve(beam)

        assertTrue(solution.isMechanicallyStable)
        assertTrue(solution.isDeterminate)
        assertEquals(2, solution.reactions.size)

        val rA = solution.reactions.first { it.supportName == "A" }
        val rB = solution.reactions.first { it.supportName == "B" }

        // R_A = R_B = qL / 2 = 12 * 6 / 2 = 36 kN
        assertEquals(36.0, rA.ry, eps)
        assertEquals(36.0, rB.ry, eps)

        // M_max = qL^2 / 8 = 12 * 36 / 8 = 54 kN·m at x = 3 m
        assertNotNull(solution.maxMoment)
        assertEquals(3.0, solution.maxMoment!!.x, 0.05)
        assertEquals(54.0, solution.maxMoment!!.valueM, eps)

        // Shear at x=0 is +36, at x=6 is -36, at midspan is 0
        val tMid = solution.segments.first().tFunc(3.0)
        assertEquals(0.0, tMid, eps)

        // All engineering checks passed
        assertTrue(solution.checks.isNotEmpty())
        assertTrue("All engineering checks must pass", solution.checks.all { it.isPassed })
    }

    @Test
    fun testSimplySupportedBeamWithCentralPointLoad() {
        val beam = BeamPresets.simplySupportedCentralPoint // L=6m, F=30 kN at x=3m
        val solution = RdmSolver.solve(beam)

        assertTrue(solution.isMechanicallyStable)
        assertTrue(solution.isDeterminate)

        val rA = solution.reactions.first { it.supportName == "A" }
        val rB = solution.reactions.first { it.supportName == "B" }

        // R_A = R_B = F / 2 = 15 kN
        assertEquals(15.0, rA.ry, eps)
        assertEquals(15.0, rB.ry, eps)

        // M_max = F * L / 4 = 30 * 6 / 4 = 45 kN·m at x = 3m
        assertNotNull(solution.maxMoment)
        assertEquals(3.0, solution.maxMoment!!.x, 0.05)
        assertEquals(45.0, solution.maxMoment!!.valueM, eps)

        // Shear jump = 30 kN across x = 3m
        val tLeft = solution.segments[0].tFunc(2.99)
        val tRight = solution.segments[1].tFunc(3.01)
        assertEquals(15.0, tLeft, 0.1)
        assertEquals(-15.0, tRight, 0.1)
    }

    @Test
    fun testCantileverWithTipPointLoad() {
        val beam = BeamPresets.cantileverPoint // L=4m, Fixed at x=0, F=20 kN at x=4m
        val solution = RdmSolver.solve(beam)

        assertTrue(solution.isMechanicallyStable)
        assertTrue(solution.isDeterminate)
        assertEquals(1, solution.reactions.size)

        val rA = solution.reactions[0]
        assertEquals(20.0, rA.ry, eps)
        // M_enc = - F * L = - 20 * 4 = -80 kN·m
        assertEquals(-80.0, rA.m, eps)

        // M at tip x=4 must be 0
        val mTip = solution.segments.first().mFunc(4.0)
        assertEquals(0.0, mTip, eps)
    }

    @Test
    fun testCantileverWithUdl() {
        val beam = BeamPresets.cantileverUdl // L=3m, Fixed at x=0, q=10 kN/m
        val solution = RdmSolver.solve(beam)

        assertTrue(solution.isMechanicallyStable)
        val rA = solution.reactions[0]
        // R_Ay = q*L = 30 kN
        assertEquals(30.0, rA.ry, eps)
        // M_enc = - q*L^2 / 2 = - 10 * 9 / 2 = -45 kN·m
        assertEquals(-45.0, rA.m, eps)

        // M at free end x=3 must be 0
        val mEnd = solution.segments.first().mFunc(3.0)
        assertEquals(0.0, mEnd, eps)
    }

    @Test
    fun testUnstableMechanismDetection() {
        // Zero supports
        val freeBeam = BeamStructure(
            length = 5.0,
            supports = emptyList(),
            loads = listOf(Load("f1", LoadType.POINT_FORCE, 2.5, 2.5, 10.0))
        )
        val solFree = RdmSolver.solve(freeBeam)
        assertFalse("Beam without supports must be detected as unstable mechanism", solFree.isMechanicallyStable)

        // Only one roller support
        val singleRoller = BeamStructure(
            length = 5.0,
            supports = listOf(Support("s1", "A", SupportType.ROLLER, 0.0)),
            loads = listOf(Load("f1", LoadType.POINT_FORCE, 2.5, 2.5, 10.0))
        )
        val solRoller = RdmSolver.solve(singleRoller)
        assertFalse("Beam with single roller must be detected as unstable mechanism", solRoller.isMechanicallyStable)
    }

    @Test
    fun testOverhangingBeamReactions() {
        val beam = BeamPresets.overhangingBeam // L=8m, A at 0, B at 6, q=8 on [0,6], F=24 at 8
        val solution = RdmSolver.solve(beam)

        assertTrue(solution.isMechanicallyStable)
        // Total load Fy = 8*6 + 24 = 48 + 24 = 72 kN
        // Sum M/A = 0: R_By * 6 - (48 * 3 + 24 * 8) = 0
        // 48*3 = 144, 24*8 = 192, sum = 336
        // R_By = 336 / 6 = 56 kN
        // R_Ay = 72 - 56 = 16 kN
        val rA = solution.reactions.first { it.supportName == "A" }
        val rB = solution.reactions.first { it.supportName == "B" }

        assertEquals(16.0, rA.ry, eps)
        assertEquals(56.0, rB.ry, eps)

        // Moment at B (x=6): coming from overhang cut to the right: -24 * 2 = -48 kN·m
        val mB = solution.segments[0].mFunc(6.0)
        assertEquals(-48.0, mB, eps)

        // Moment at free end x=8 must be 0
        val mEnd = solution.segments.last().mFunc(8.0)
        assertEquals(0.0, mEnd, eps)
    }

    @Test
    fun testRectangularSectionProperties() {
        val rect = CrossSection(
            shapeType = SectionShapeType.RECTANGLE,
            width_mm = 150.0,
            height_mm = 300.0
        )
        val props = com.example.solver.SectionSolver.computeProperties(rect)

        // Area A = 150 * 300 = 45000 mm² = 450 cm²
        assertEquals(450.0, props.area_cm2, eps)
        // Centroid at half height = 150 mm
        assertEquals(150.0, props.centroidY_inf_mm, eps)
        assertEquals(150.0, props.centroidY_sup_mm, eps)
        // Iz = 150 * 300^3 / 12 = 337,500,000 mm⁴ = 33,750 cm⁴
        assertEquals(33750.0, props.inertiaIz_cm4, eps)
        // Wel = Iz / 150 = 2250 cm³
        assertEquals(2250.0, props.modulusWel_inf_cm3, eps)
        // S_max = 150 * 300^2 / 8 = 1,687,500 mm³ = 1687.5 cm³
        assertEquals(1687.5, props.staticMomentMax_cm3, eps)
    }

    @Test
    fun testTBeamHuygensTheorem() {
        // T-beam: flange 200 x 20 mm, web 10 x 180 mm => total height = 200 mm
        val tBeam = CrossSection(
            shapeType = SectionShapeType.T_BEAM,
            width_mm = 200.0,
            height_mm = 200.0,
            flangeThickness_mm = 20.0,
            webThickness_mm = 10.0
        )
        val props = com.example.solver.SectionSolver.computeProperties(tBeam)

        // A_flange = 200 * 20 = 4000 mm², A_web = 10 * 180 = 1800 mm² => Total = 5800 mm² = 58 cm²
        assertEquals(58.0, props.area_cm2, eps)
        // y_G = (1800 * 90 + 4000 * 190) / 5800 = (162000 + 760000) / 5800 = 922000 / 5800 ≈ 158.965 mm
        val expectedYg = (1800.0 * 90.0 + 4000.0 * 190.0) / 5800.0
        assertEquals(expectedYg, props.centroidY_inf_mm, 0.05)
        assertTrue("Iz must be positive and computed via Huygens theorem", props.inertiaIz_cm4 > 0.0)
    }

    @Test
    fun testBendingAndShearStressesNavierAndJourawski() {
        val rect = CrossSection(
            shapeType = SectionShapeType.RECTANGLE,
            width_mm = 150.0,
            height_mm = 300.0
        )
        val props = com.example.solver.SectionSolver.computeProperties(rect)
        val material = Material.STEEL_S235

        // Applied M = 54 kN·m, T = 36 kN, N = 0
        val stress = com.example.solver.SectionSolver.computeStresses(
            sectionX = 3.0,
            n_kN = 0.0,
            t_kN = 36.0,
            m_kNm = 54.0,
            props = props,
            material = material
        )

        // Navier: sigma = M / Wel = 54e6 / 2250e3 = 24.0 MPa
        assertEquals(24.0, stress.normalStressInf_MPa, 0.05)
        assertEquals(-24.0, stress.normalStressSup_MPa, 0.05)
        assertEquals(24.0, stress.maxNormalStress_MPa, 0.05)

        // Jourawski: tau_max = 1.5 * T / A = 1.5 * 36000 / 45000 = 1.2 MPa
        assertEquals(1.2, stress.maxShearStress_MPa, 0.05)

        // Von Mises: outer fiber has sigma=24, tau=0 => 24 MPa
        assertEquals(24.0, stress.vonMisesStress_MPa, 0.05)

        // Utilization: 24 / 235 ≈ 10.2%
        assertEquals(24.0 / 235.0, stress.utilizationRatio, 0.01)
        assertTrue("Section must be safe under 24 MPa vs 235 MPa", stress.isSafe)
    }

    @Test
    fun testSimplySupportedDeflectionUdl() {
        // L=6m, q=12 kN/m, Rect 150x300, E=210 GPa => EI = 70875 kN·m²
        val beam = BeamPresets.simplySupportedUdl
        val solution = RdmSolver.solve(beam)

        assertNotNull(solution.deflectionResult)
        val defl = solution.deflectionResult!!

        // Theoretical f_max = 5 * q * L^4 / (384 * EI)
        // = 5 * 12 * 1296 / (384 * 70875) = 77760 / 27216000 = 0.00285714 m = 2.857 mm
        val theoreticalFmaxMm = (5.0 * 12.0 * 6.0 * 6.0 * 6.0 * 6.0) / (384.0 * 70875.0) * 1000.0
        assertEquals(theoreticalFmaxMm, defl.maxDeflection_mm, 0.05)
        assertEquals(3.0, defl.maxDeflectionX_m, 0.05) // Max at midspan

        // ELS limit L/500 = 6000 / 500 = 12.0 mm
        assertEquals(12.0, defl.deflectionLimit_mm, eps)
        assertTrue("2.857 mm <= 12 mm => ELS compliant", defl.isElsCompliant)
    }

    @Test
    fun testSimplySupportedDeflectionCentralPointLoad() {
        // L=6m, F=30 kN at x=3m, EI = 70875 kN·m²
        val beam = BeamPresets.simplySupportedCentralPoint
        val solution = RdmSolver.solve(beam)

        assertNotNull(solution.deflectionResult)
        val defl = solution.deflectionResult!!

        // Theoretical f_max = F * L^3 / (48 * EI)
        // = 30 * 216 / (48 * 70875) = 6480 / 3402000 = 0.00190476 m = 1.905 mm
        val theoreticalFmaxMm = (30.0 * 216.0) / (48.0 * 70875.0) * 1000.0
        assertEquals(theoreticalFmaxMm, defl.maxDeflection_mm, 0.05)
        assertEquals(3.0, defl.maxDeflectionX_m, 0.05)
    }

    @Test
    fun testCantileverDeflectionTipLoad() {
        // L=4m, F=20 kN at x=4m, EI = 70875 kN·m²
        val beam = BeamPresets.cantileverPoint
        val solution = RdmSolver.solve(beam)

        assertNotNull(solution.deflectionResult)
        val defl = solution.deflectionResult!!

        // Theoretical f_max = F * L^3 / (3 * EI)
        // = 20 * 64 / (3 * 70875) = 1280 / 212625 = 0.00602 m = 6.02 mm
        val theoreticalFmaxMm = (20.0 * 64.0) / (3.0 * 70875.0) * 1000.0
        assertEquals(theoreticalFmaxMm, defl.maxDeflection_mm, 0.05)
        assertEquals(4.0, defl.maxDeflectionX_m, 0.05) // Tip
    }

    @Test
    fun testCantileverDeflectionUdl() {
        // L=3m, q=10 kN/m, EI = 70875 kN·m²
        val beam = BeamPresets.cantileverUdl
        val solution = RdmSolver.solve(beam)

        assertNotNull(solution.deflectionResult)
        val defl = solution.deflectionResult!!

        // Theoretical f_max = q * L^4 / (8 * EI)
        // = 10 * 81 / (8 * 70875) = 810 / 567000 = 0.00142857 m = 1.429 mm
        val theoreticalFmaxMm = (10.0 * 81.0) / (8.0 * 70875.0) * 1000.0
        assertEquals(theoreticalFmaxMm, defl.maxDeflection_mm, 0.05)
        assertEquals(3.0, defl.maxDeflectionX_m, 0.05)
    }

    @Test
    fun testWarrenTrussEquilibrium() {
        val truss = TrussPresets.warrenTruss
        val solution = com.example.solver.TrussSolver.solve(truss)

        assertTrue("Warren truss must be stable", solution.isStable)
        assertTrue("Warren truss must be isostatic (b+r=2j)", solution.isIsostatic)

        // Total vertical load = 20 + 40 + 20 = 80 kN
        // Symmetric truss => R_1y = R_4y = 40 kN
        val r1 = solution.reactions.first { it.supportId == "n1" }
        val r4 = solution.reactions.first { it.supportId == "n4" }

        assertEquals(40.0, r1.ry, 0.05)
        assertEquals(40.0, r4.ry, 0.05)
        assertEquals(0.0, r1.rx, 0.05)

        // All bars must have calculated forces
        assertEquals(truss.bars.size, solution.barResults.size)
        assertTrue("All engineering checks must pass for Warren truss", solution.checks.all { it.isPassed })
    }

    @Test
    fun testTriangularRoofTrussEquilibrium() {
        val truss = TrussPresets.triangularRoofTruss
        val solution = com.example.solver.TrussSolver.solve(truss)

        assertTrue("Roof truss must be stable", solution.isStable)
        assertTrue("Roof truss must be isostatic", solution.isIsostatic)

        // Total vertical load = 15 + 30 + 15 = 60 kN
        // Symmetry => R_Ay = R_Cy = 30 kN
        val rA = solution.reactions.first { it.supportId == "n1" }
        val rC = solution.reactions.first { it.supportId == "n3" }

        assertEquals(30.0, rA.ry, 0.05)
        assertEquals(30.0, rC.ry, 0.05)

        // Bottom tie-beam (Bar A-B, B-C) must be in tension (+)
        val barAB = solution.barResults.first { it.barName == "A-B" }
        assertEquals(TrussBarState.TRACTION, barAB.state)
        assertTrue("Tie beam must be in tension", barAB.force_kN > 0.0)

        // Rafter (Bar A-D, F-C) must be in compression (-)
        val barAD = solution.barResults.first { it.barName == "A-D" }
        assertEquals(TrussBarState.COMPRESSION, barAD.state)
        assertTrue("Rafter must be in compression", barAD.force_kN < 0.0)
    }
}
