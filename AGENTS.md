# AGENTS.md - Instructions for Future AI Engineering Sessions

## Conventions & Rules for RDM Solver
1. **Civil Engineering Terminology:** Always preserve standard French civil engineering terminology (Poutre, Appui simple, Articulation, Encastrement, Effort normal N, Effort tranchant T, Moment fléchissant M, Flèche, Contrainte σ et τ, Flambement).
2. **Mathematical Determinism:** Never approximate or hallucinate reaction forces or moments using an LLM. All calculations MUST be performed strictly by the deterministic solver engine (`com.example.solver`).
3. **Sign Convention:**
   - $N > 0$ = Traction
   - $T(x) = \sum F_{y,\text{gauche}}$
   - $M(x) > 0$ = Fibres inférieures tendues (sagging)
   - $\frac{dM}{dx} = T(x)$
4. **Verification Step:** Always run the automated engineering verification checks (`VerificationEngine.kt`) after computing any solution. If unstable or a mechanism, report the exact missing degree of freedom clearly without crashing.
5. **Phase Progression:**
   - Phase 1: Statically determinate beams (Reactions, N/T/M diagrams, step-by-step resolution).
   - Phase 2: Section geometry and stress distribution ($\sigma = My/I$, $\tau = TS/(Ib)$).
   - Phase 3: Deflections (Double integration, superposition).
   - Phase 4: Trusses (Method of joints & sections).
   - Phase 5: Hyperstatic structures (Clapeyron, Force method).
   - Phase 6: Buckling (Euler, slenderness).
