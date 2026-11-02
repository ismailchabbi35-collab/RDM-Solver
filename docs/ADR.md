# Architectural Decision Records (ADR) - RDM Solver

## ADR 001: Pure Deterministic Solver Engine Layer
- **Status:** Accepted
- **Context:** Civil engineering students require 100% deterministic, mathematically exact calculations with step-by-step derivations (formula $\to$ numerical substitution $\to$ result $\to$ units). Floating-point inaccuracies or heuristic non-explainable methods fail educational standards.
- **Decision:** The solver engine (`com.example.solver.*`) is implemented as pure, decoupled Kotlin functions with zero Android UI dependencies. It takes an immutable `BeamStructure` input, validates stability and determinacy, performs static equilibrium calculations ($\sum F_x = 0, \sum F_y = 0, \sum M = 0$), derives piecewise continuous polynomial equations for $N(x), T(x), M(x)$, and produces a structured `RdmSolution` object.
- **Consequences:** 
  - Easily unit-tested via local JVM tests (`RdmSolverTest.kt`).
  - Capable of running 100% offline on-device without network latency.
  - Portable to backend or other targets if needed.

---

## ADR 002: Civil Engineering Sign Convention (Convention Française RDM)
- **Status:** Accepted
- **Context:** Across Francophone civil engineering faculties (Polytechnique, ESTP, INSA, ENPC, etc.) and Eurocode practices, specific conventions apply to internal forces.
- **Decision:**
  - **Coordinate system:** Beam axis $x$ oriented from left ($x=0$) to right ($x=L$). Upward vertical axis $y > 0$.
  - **Normal force $N(x)$:** Traction (tension) is positive ($N > 0$), compression is negative ($N < 0$).
  - **Effort tranchant $T(x)$ (or $V(x)$):** Left-cut convention: $T(x) = \sum F_{y,\text{gauche}}$. An upward external load to the left causes positive shear.
  - **Moment fléchissant $M(x)$:** Tension on bottom fibers ("fibres tendues en bas") is positive ($M(x) > 0$, sagging). Consequently:
    $$\frac{dM(x)}{dx} = T(x), \quad \frac{dT(x)}{dx} = -q(x)$$
  - **Equilibrium equation for moment:** $\sum M_{\text{pivot}} = 0$ taken counter-clockwise as positive ($+ \circlearrowleft$).
- **Consequences:** Consistency across all steps, diagrams, and textbook examples ($M_{\max} = \frac{qL^2}{8}$ positive at midspan for simply supported beam).

---

## ADR 003: Three-Tier Architecture
- **Status:** Accepted
- **Context:** The app must support interactive editing, diagram visualization, multi-language UI, and future phases (Phase 2 sections/stresses, Phase 3 deflection, Phase 4 trusses, Phase 5 hyperstatic, Phase 6 buckling).
- **Decision:** Maintain strict separation into 3 distinct layers:
  1. **UI Layer:** Jetpack Compose, Canvas rendering, localized strings (FR / AR / EN), M3 components.
  2. **Solver Engine (Core):** Pure engineering calculations, matrix/equilibrium solvers, root finding for $T(x)=0$, interval segmentation.
  3. **Step & Verification Renderer:** Generates human-readable, educational solution steps (`StepItem`: title, formula, substitution, result, unit) and automated verification tests.
- **Consequences:** High cohesion, easy maintenance, progressive rollout of Phases 1 to 6 without refactoring the UI.

---

## ADR 004: Offline-First Operation with Optional AI Pedagogical Assistance
- **Status:** Accepted
- **Context:** Calculations must never rely on cloud APIs or LLMs for computation. However, students often need natural-language explanations of specific steps in French or Arabic.
- **Decision:** All structural calculations, diagrams, and verification checks run strictly on-device in real-time. An optional "Explain this step" feature can utilize Gemini/LLM to provide pedagogical guidance, but all numerical inputs and outputs are deterministically supplied by the solver.
