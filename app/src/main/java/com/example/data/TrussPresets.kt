package com.example.data

object TrussPresets {

    /**
     * Warren Truss with vertical posts (Span = 6m, Height = 2m, 6 bays)
     */
    val warrenTruss = TrussStructure(
        id = "preset_warren",
        name = "Treillis Warren (6m × 2m)",
        nodes = listOf(
            TrussNode("n1", "1", 0.0, 0.0, support = SupportType.PINNED),
            TrussNode("n2", "2", 2.0, 0.0),
            TrussNode("n3", "3", 4.0, 0.0),
            TrussNode("n4", "4", 6.0, 0.0, support = SupportType.ROLLER),
            TrussNode("n5", "5", 1.0, 2.0, fy_kN = -20.0),
            TrussNode("n6", "6", 3.0, 2.0, fy_kN = -40.0),
            TrussNode("n7", "7", 5.0, 2.0, fy_kN = -20.0)
        ),
        bars = listOf(
            // Bottom chord
            TrussBar("b1", "1-2", "n1", "n2"),
            TrussBar("b2", "2-3", "n2", "n3"),
            TrussBar("b3", "3-4", "n3", "n4"),
            // Top chord
            TrussBar("b4", "5-6", "n5", "n6"),
            TrussBar("b5", "6-7", "n6", "n7"),
            // Diagonals
            TrussBar("b6", "1-5", "n1", "n5"),
            TrussBar("b7", "2-5", "n2", "n5"),
            TrussBar("b8", "2-6", "n2", "n6"),
            TrussBar("b9", "3-6", "n3", "n6"),
            TrussBar("b10", "3-7", "n3", "n7"),
            TrussBar("b11", "4-7", "n4", "n7")
        )
    )

    /**
     * Triangular Roof Truss / Ferme en A (Span = 8m, Height = 3m)
     */
    val triangularRoofTruss = TrussStructure(
        id = "preset_triangular_roof",
        name = "Ferme en A (Pignon 8m × 3m)",
        nodes = listOf(
            TrussNode("n1", "A", 0.0, 0.0, support = SupportType.PINNED),
            TrussNode("n2", "B", 4.0, 0.0),
            TrussNode("n3", "C", 8.0, 0.0, support = SupportType.ROLLER),
            TrussNode("n4", "D", 2.0, 1.5, fy_kN = -15.0),
            TrussNode("n5", "E", 4.0, 3.0, fy_kN = -30.0),
            TrussNode("n6", "F", 6.0, 1.5, fy_kN = -15.0)
        ),
        bars = listOf(
            // Tie beam (Entrait)
            TrussBar("b1", "A-B", "n1", "n2"),
            TrussBar("b2", "B-C", "n2", "n3"),
            // Rafters (Arbalétriers)
            TrussBar("b3", "A-D", "n1", "n4"),
            TrussBar("b4", "D-E", "n4", "n5"),
            TrussBar("b5", "E-F", "n5", "n6"),
            TrussBar("b6", "F-C", "n6", "n3"),
            // Internal web members (Poinçon et Fiches)
            TrussBar("b7", "B-E", "n2", "n5"),
            TrussBar("b8", "B-D", "n2", "n4"),
            TrussBar("b9", "B-F", "n2", "n6")
        )
    )

    /**
     * Pratt Truss (Span = 8m, Height = 2m)
     */
    val prattTruss = TrussStructure(
        id = "preset_pratt",
        name = "Treillis Pratt (8m × 2m)",
        nodes = listOf(
            TrussNode("n1", "1", 0.0, 0.0, support = SupportType.PINNED),
            TrussNode("n2", "2", 2.0, 0.0, fy_kN = -25.0),
            TrussNode("n3", "3", 4.0, 0.0, fy_kN = -50.0),
            TrussNode("n4", "4", 6.0, 0.0, fy_kN = -25.0),
            TrussNode("n5", "5", 8.0, 0.0, support = SupportType.ROLLER),
            TrussNode("n6", "6", 2.0, 2.0),
            TrussNode("n7", "7", 4.0, 2.0),
            TrussNode("n8", "8", 6.0, 2.0)
        ),
        bars = listOf(
            // Bottom chord
            TrussBar("b1", "1-2", "n1", "n2"),
            TrussBar("b2", "2-3", "n2", "n3"),
            TrussBar("b3", "3-4", "n3", "n4"),
            TrussBar("b4", "4-5", "n4", "n5"),
            // Top chord
            TrussBar("b5", "1-6", "n1", "n6"),
            TrussBar("b6", "6-7", "n6", "n7"),
            TrussBar("b7", "7-8", "n7", "n8"),
            TrussBar("b8", "8-5", "n8", "n5"),
            // Vertical posts
            TrussBar("b9", "2-6", "n2", "n6"),
            TrussBar("b10", "3-7", "n3", "n7"),
            TrussBar("b11", "4-8", "n4", "n8"),
            // Diagonals (Pratt: tension diagonals slope towards center)
            TrussBar("b12", "2-7", "n2", "n7"),
            TrussBar("b13", "4-7", "n4", "n7")
        )
    )

    val allPresets = listOf(
        warrenTruss,
        triangularRoofTruss,
        prattTruss
    )
}
