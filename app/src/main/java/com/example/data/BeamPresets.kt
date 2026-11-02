package com.example.data

object BeamPresets {

    val simplySupportedUdl = BeamStructure(
        id = "preset_simply_supported_udl",
        name = "Bi-appuyée avec charge uniforme q",
        length = 6.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.PINNED, 0.0),
            Support("sup_B", "B", SupportType.ROLLER, 6.0)
        ),
        loads = listOf(
            Load(
                id = "load_q",
                type = LoadType.DISTRIBUTED_UNIFORM,
                positionStart = 0.0,
                positionEnd = 6.0,
                magnitude = 12.0
            )
        )
    )

    val simplySupportedCentralPoint = BeamStructure(
        id = "preset_central_point",
        name = "Bi-appuyée avec force ponctuelle F au centre",
        length = 6.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.PINNED, 0.0),
            Support("sup_B", "B", SupportType.ROLLER, 6.0)
        ),
        loads = listOf(
            Load(
                id = "load_f",
                type = LoadType.POINT_FORCE,
                positionStart = 3.0,
                magnitude = 30.0
            )
        )
    )

    val cantileverPoint = BeamStructure(
        id = "preset_cantilever_point",
        name = "Console encastrée avec charge ponctuelle",
        length = 4.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.FIXED, 0.0)
        ),
        loads = listOf(
            Load(
                id = "load_f_end",
                type = LoadType.POINT_FORCE,
                positionStart = 4.0,
                magnitude = 20.0
            )
        )
    )

    val cantileverUdl = BeamStructure(
        id = "preset_cantilever_udl",
        name = "Console encastrée avec charge répartie",
        length = 3.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.FIXED, 0.0)
        ),
        loads = listOf(
            Load(
                id = "load_q_cantilever",
                type = LoadType.DISTRIBUTED_UNIFORM,
                positionStart = 0.0,
                positionEnd = 3.0,
                magnitude = 10.0
            )
        )
    )

    val overhangingBeam = BeamStructure(
        id = "preset_overhanging",
        name = "Poutre avec porte-à-faux (travée + console)",
        length = 8.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.PINNED, 0.0),
            Support("sup_B", "B", SupportType.ROLLER, 6.0)
        ),
        loads = listOf(
            Load(
                id = "load_q_span",
                type = LoadType.DISTRIBUTED_UNIFORM,
                positionStart = 0.0,
                positionEnd = 6.0,
                magnitude = 8.0
            ),
            Load(
                id = "load_f_overhang",
                type = LoadType.POINT_FORCE,
                positionStart = 8.0,
                magnitude = 24.0
            )
        )
    )

    val triangularLoad = BeamStructure(
        id = "preset_triangular",
        name = "Bi-appuyée avec charge triangulaire",
        length = 6.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.PINNED, 0.0),
            Support("sup_B", "B", SupportType.ROLLER, 6.0)
        ),
        loads = listOf(
            Load(
                id = "load_tri",
                type = LoadType.DISTRIBUTED_TRIANGULAR,
                positionStart = 0.0,
                positionEnd = 6.0,
                magnitude = 0.0,
                magnitudeEnd = 18.0
            )
        )
    )

    val concentratedMoment = BeamStructure(
        id = "preset_moment",
        name = "Bi-appuyée avec couple ponctuel C",
        length = 6.0,
        supports = listOf(
            Support("sup_A", "A", SupportType.PINNED, 0.0),
            Support("sup_B", "B", SupportType.ROLLER, 6.0)
        ),
        loads = listOf(
            Load(
                id = "load_moment",
                type = LoadType.MOMENT,
                positionStart = 3.0,
                magnitude = 24.0 // 24 kN·m clockwise
            )
        )
    )

    val allPresets = listOf(
        simplySupportedUdl,
        simplySupportedCentralPoint,
        cantileverPoint,
        cantileverUdl,
        overhangingBeam,
        triangularLoad,
        concentratedMoment
    )
}
