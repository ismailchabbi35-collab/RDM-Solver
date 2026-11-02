# RDM Solver - Solveur de Résistance des Matériaux

Application mobile Android dédiée aux étudiants et ingénieurs en génie civil pour la résolution analytique, visuelle et détaillée des problèmes de Résistance des Matériaux (RDM).

## Fonctionnalités Principales (Phase 1)
- **Calcul Exact & Déterministe des Réactions d'Appuis :**
  - Équations d'équilibre statique global ($\sum F_x = 0, \sum F_y = 0, \sum M = 0$).
  - Prise en charge des appuis simples (rouleau), articulations (fixes) et encastrements (console).
  - Prise en charge des charges ponctuelles (verticales ou inclinées), uniformément réparties ($q$), trapézoïdales/triangulaires et couples concentrés ($C$).
- **Diagrammes des Sollicitations Internes (N, T, M) :**
  - Diagramme de l'effort normal $N(x)$ (Traction $>0$, Compression $<0$).
  - Diagramme de l'effort tranchant $T(x)$ avec sauts sous forces concentrées.
  - Diagramme du moment fléchissant $M(x)$ selon la convention RDM des fibres inférieures tendues ($M > 0$).
  - Recherche automatique de l'extremum $M_{\max}$ à l'abscisse où $T(x) = 0$ ($\frac{dM}{dx} = T$).
- **Démonstration Pas-à-Pas Complète :**
  - Structure pédagogique rigoureuse : Formule $\to$ Substitution $\to$ Résultat $\to$ Unité normalisée (kN, m, kN·m).
  - Explications et interprétations physiques des discontinuités et conditions aux limites.
- **Vérifications Automatiques d'Ingénierie Civile :**
  - Contrôle du résidu d'équilibre statique global ($< 10^{-3}$ kN et kN·m).
  - Vérification des conditions aux limites ($M = 0$ sur appuis simples d'extrémité).
  - Vérification de la relation différentielle fondamentale $\frac{dM}{dx} = T(x)$.
  - Vérification du saut de l'effort tranchant $\Delta T = F_{\text{appliquée}}$.
  - Détection automatique d'instabilités (mécanismes) ou de systèmes hyperstatiques.
- **Interface Utilisateur Jetpack Compose Réactive :**
  - Canevas interactif de la poutre avec affichage des cotes et réactions calculées.
  - Curseur tactile d'inspection sur les diagrammes.
  - Cas types préchargés (Poutre bi-appuyée avec $q$, Force ponctuelle au centre, Console encastrée, Porte-à-faux, Charge triangulaire, Couple concentré).
  - Toggles multilingues : Français / Arabe / Anglais.

## Structure du Projet
```
docs/
├── ADR.md                 # Architecture Decision Records
├── DATA_MODEL.json        # Schéma JSON de la structure
└── FORMULAS_REFERENCE.md  # Référentiel des formules et conventions RDM
app/src/main/java/com/example/
├── data/
│   ├── BeamModel.kt       # Modèles de données (Poutre, Appui, Charge)
│   ├── SolutionModel.kt   # Modèles de solution (Réactions, Diagrammes, Étapes, Contrôles)
│   └── BeamPresets.kt     # Cas types d'école d'ingénieurs
├── solver/
│   ├── RdmSolver.kt       # Orchestrateur central
│   ├── ReactionsSolver.kt # Équilibre global et réactions
│   ├── InternalForcesSolver.kt # Équations N(x), T(x), M(x) par tronçon
│   └── VerificationEngine.kt # Contrôles et règles de cohérence
└── ui/
    ├── RdmViewModel.kt    # State holder et logique de présentation
    ├── RdmScreen.kt       # Écran Compose principal
    ├── components/
    │   ├── BeamCanvas.kt  # Dessin de la poutre et des charges
    │   ├── DiagramCanvas.kt # Tracé interactif des diagrammes
    │   ├── StepItemCard.kt # Carte de solution pas à pas
    │   ├── AddSupportDialog.kt
    │   └── AddLoadDialog.kt
    └── theme/
app/src/test/java/com/example/
└── RdmSolverTest.kt       # Tests unitaires validant les cas d'école
```
