package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.solver.SectionSolver
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RdmScreen(
    viewModel: RdmViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val structure = uiState.structure
    val solution = uiState.solution

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Architecture,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RDM Solver",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Résistance des Matériaux • Solutions pas à pas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Language Switcher Chips
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("FR", "AR", "EN").forEach { lang ->
                            FilterChip(
                                selected = (uiState.selectedLanguage == lang),
                                onClick = { viewModel.setLanguage(lang) },
                                label = { Text(lang, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector: Beam (Poutre) vs Truss (Treillis)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                SegmentedButton(
                    selected = (uiState.mode == RdmMode.BEAM),
                    onClick = { viewModel.setMode(RdmMode.BEAM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Poutres (RDM)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = (uiState.mode == RdmMode.TRUSS),
                    onClick = { viewModel.setMode(RdmMode.TRUSS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Treillis Plans (Fermes)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 1. Presets Quick Bar (Beams or Trusses)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.mode == RdmMode.BEAM) "Cas types Poutres :" else "Cas types Treillis :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (uiState.mode == RdmMode.BEAM) {
                    SuggestionChip(
                        onClick = { viewModel.loadPreset(BeamPresets.simplySupportedUdl) },
                        label = { Text("Bi-appuyée (q)") },
                        icon = { Icon(Icons.Outlined.Straighten, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("preset_udl_chip")
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadPreset(BeamPresets.simplySupportedCentralPoint) },
                        label = { Text("Bi-appuyée (F)") },
                        icon = { Icon(Icons.Outlined.South, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("preset_point_chip")
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadPreset(BeamPresets.cantileverPoint) },
                        label = { Text("Console (F)") },
                        icon = { Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadPreset(BeamPresets.cantileverUdl) },
                        label = { Text("Console (q)") },
                        icon = { Icon(Icons.Outlined.DensityMedium, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadPreset(BeamPresets.overhangingBeam) },
                        label = { Text("Porte-à-faux") },
                        icon = { Icon(Icons.AutoMirrored.Outlined.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                } else {
                    SuggestionChip(
                        onClick = { viewModel.loadTrussPreset(TrussPresets.warrenTruss) },
                        label = { Text("Treillis Warren") },
                        icon = { Icon(Icons.Outlined.GridOn, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadTrussPreset(TrussPresets.triangularRoofTruss) },
                        label = { Text("Ferme en A (Toiture)") },
                        icon = { Icon(Icons.Outlined.ChangeHistory, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.loadTrussPreset(TrussPresets.prattTruss) },
                        label = { Text("Treillis Pratt") },
                        icon = { Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // 2. Interactive Header (Beam Canvas or Truss Canvas)
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                if (uiState.mode == RdmMode.BEAM) {
                    BeamCanvas(
                        structure = structure,
                        reactions = solution.reactions,
                        modifier = Modifier.testTag("beam_canvas")
                    )
                } else {
                    TrussCanvas(
                        structure = uiState.trussStructure,
                        solution = uiState.trussSolution,
                        modifier = Modifier.testTag("truss_canvas")
                    )
                }
            }

            // 3. Navigation Tabs
            if (uiState.mode == RdmMode.BEAM) {
                TabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = (uiState.selectedTabIndex == 0),
                        onClick = { viewModel.setTabIndex(0) },
                        text = { Text("Structure", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_structure")
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 1),
                        onClick = { viewModel.setTabIndex(1) },
                        text = { Text("Diagrammes", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_diagrams")
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 2),
                        onClick = { viewModel.setTabIndex(2) },
                        text = { Text("Section & σ, τ", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Outlined.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_section")
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 3),
                        onClick = { viewModel.setTabIndex(3) },
                        text = { Text("Pas-à-Pas", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_steps")
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 4),
                        onClick = { viewModel.setTabIndex(4) },
                        text = { Text("Contrôles", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Outlined.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_checks")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    when (uiState.selectedTabIndex) {
                        0 -> StructureTabContent(
                            structure = structure,
                            solution = solution,
                            onUpdateLength = { viewModel.updateBeamLength(it) },
                            onShowAddSupport = { viewModel.setShowAddSupportDialog(true) },
                            onShowAddLoad = { viewModel.setShowAddLoadDialog(true) },
                            onRemoveSupport = { viewModel.removeSupport(it) },
                            onRemoveLoad = { viewModel.removeLoad(it) }
                        )
                        1 -> DiagramsTabContent(
                            solution = solution,
                            selectedDiagram = uiState.selectedDiagramIndex,
                            onSelectDiagram = { viewModel.setDiagramIndex(it) }
                        )
                        2 -> SectionTabContent(
                            structure = structure,
                            solution = solution,
                            onUpdateSection = { viewModel.updateSection(it) },
                            onUpdateMaterial = { viewModel.updateMaterial(it) }
                        )
                        3 -> StepsTabContent(solution = solution)
                        4 -> ChecksTabContent(solution = solution)
                    }
                }
            } else {
                // Truss Navigation Tabs (Efforts, Pas-à-Pas, Contrôles)
                TabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = (uiState.selectedTabIndex == 0),
                        onClick = { viewModel.setTabIndex(0) },
                        text = { Text("Efforts Axiaux", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Outlined.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 1),
                        onClick = { viewModel.setTabIndex(1) },
                        text = { Text("Méthode des Nœuds", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = (uiState.selectedTabIndex == 2),
                        onClick = { viewModel.setTabIndex(2) },
                        text = { Text("Contrôles", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(Icons.Outlined.Verified, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    when (uiState.selectedTabIndex) {
                        0 -> TrussForcesTabContent(uiState.trussSolution)
                        1 -> StepsTabContent(solution = RdmSolution(
                            structure = structure,
                            reactions = uiState.trussSolution.reactions,
                            segments = emptyList(),
                            diagramN = emptyList(), diagramT = emptyList(), diagramM = emptyList(),
                            remarkablePoints = emptyList(), maxMoment = null, minMoment = null, maxShear = null,
                            steps = uiState.trussSolution.steps,
                            checks = uiState.trussSolution.checks,
                            isDeterminate = uiState.trussSolution.isIsostatic,
                            isMechanicallyStable = uiState.trussSolution.isStable,
                            statusMessage = uiState.trussSolution.statusMessage
                        ))
                        2 -> ChecksTabContent(solution = RdmSolution(
                            structure = structure,
                            reactions = uiState.trussSolution.reactions,
                            segments = emptyList(),
                            diagramN = emptyList(), diagramT = emptyList(), diagramM = emptyList(),
                            remarkablePoints = emptyList(), maxMoment = null, minMoment = null, maxShear = null,
                            steps = uiState.trussSolution.steps,
                            checks = uiState.trussSolution.checks,
                            isDeterminate = uiState.trussSolution.isIsostatic,
                            isMechanicallyStable = uiState.trussSolution.isStable,
                            statusMessage = uiState.trussSolution.statusMessage
                        ))
                    }
                }
            }
        }
    }

    // Support & Load Modal Dialogs
    if (uiState.showAddSupportDialog) {
        AddSupportDialog(
            beamLength = structure.length,
            onDismiss = { viewModel.setShowAddSupportDialog(false) },
            onAddSupport = { viewModel.addSupport(it) }
        )
    }

    if (uiState.showAddLoadDialog) {
        AddLoadDialog(
            beamLength = structure.length,
            onDismiss = { viewModel.setShowAddLoadDialog(false) },
            onAddLoad = { viewModel.addLoad(it) }
        )
    }
}

@Composable
private fun StructureTabContent(
    structure: BeamStructure,
    solution: RdmSolution,
    onUpdateLength: (Double) -> Unit,
    onShowAddSupport: () -> Unit,
    onShowAddLoad: () -> Unit,
    onRemoveSupport: (String) -> Unit,
    onRemoveLoad: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status & Stability Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (solution.isMechanicallyStable && solution.isDeterminate)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (solution.isMechanicallyStable && solution.isDeterminate)
                            Icons.Filled.CheckCircle
                        else
                            Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (solution.isMechanicallyStable && solution.isDeterminate)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (solution.isMechanicallyStable && solution.isDeterminate)
                                "Poutre Isostatique Stable (h = 0)"
                            else
                                "Vérification requise",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = solution.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Length Slider
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Longueur de la poutre (L)", fontWeight = FontWeight.SemiBold)
                        Text("%.2f m".format(Locale.US, structure.length), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = structure.length.toFloat(),
                        onValueChange = { onUpdateLength(it.toDouble()) },
                        valueRange = 2f..20f,
                        steps = 35
                    )
                }
            }
        }

        // Support Management
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Appuis (${structure.supports.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = onShowAddSupport,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_support_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter appui")
                }
            }
        }

        items(structure.supports) { sup ->
            val reaction = solution.reactions.firstOrNull { it.supportId == sup.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Appui ${sup.name} (${sup.type.labelFr})",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Position x = %.2f m".format(Locale.US, sup.position),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (reaction != null) {
                            Text(
                                text = "Réaction Ry = %.2f kN %s".format(
                                    Locale.US, reaction.ry,
                                    if (abs(reaction.m) > 0.01) "• Moment M = %.2f kN·m".format(Locale.US, reaction.m) else ""
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ReactionGreen
                            )
                        }
                    }

                    IconButton(onClick = { onRemoveSupport(sup.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer appui", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Load Management
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Charges appliquées (${structure.loads.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = onShowAddLoad,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_load_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter charge")
                }
            }
        }

        items(structure.loads) { load ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = load.type.labelFr, fontWeight = FontWeight.Bold)
                        val posDesc = when (load.type) {
                            LoadType.POINT_FORCE -> "x = %.2f m • F = %.2f kN".format(Locale.US, load.positionStart, load.magnitude)
                            LoadType.DISTRIBUTED_UNIFORM -> "x ∈ [%.2f, %.2f] m • q = %.2f kN/m".format(Locale.US, load.positionStart, load.positionEnd, load.magnitude)
                            LoadType.DISTRIBUTED_TRIANGULAR -> "x ∈ [%.2f, %.2f] m • q1 = %.2f ➔ q2 = %.2f kN/m".format(Locale.US, load.positionStart, load.positionEnd, load.magnitude, load.magnitudeEnd)
                            LoadType.MOMENT -> "x = %.2f m • C = %.2f kN·m".format(Locale.US, load.positionStart, load.magnitude)
                        }
                        Text(
                            text = posDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onRemoveLoad(load.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer charge", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagramsTabContent(
    solution: RdmSolution,
    selectedDiagram: Int,
    onSelectDiagram: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Segmented Switcher
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = (selectedDiagram == 0),
                onClick = { onSelectDiagram(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
            ) {
                Text("Effort T", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = (selectedDiagram == 1),
                onClick = { onSelectDiagram(1) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
            ) {
                Text("Moment M", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = (selectedDiagram == 2),
                onClick = { onSelectDiagram(2) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
            ) {
                Text("Flèche f", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = (selectedDiagram == 3),
                onClick = { onSelectDiagram(3) },
                shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
            ) {
                Text("Normal N", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Diagram Canvas
        val diagType = when (selectedDiagram) {
            0 -> DiagramType.SHEAR
            1 -> DiagramType.MOMENT
            2 -> DiagramType.DEFLECTION
            else -> DiagramType.NORMAL
        }
        val points = when (selectedDiagram) {
            0 -> solution.diagramT
            1 -> solution.diagramM
            2 -> solution.deflectionResult?.deflectionPoints ?: emptyList()
            else -> solution.diagramN
        }
        val col = when (selectedDiagram) {
            0 -> ShearColor
            1 -> MomentColor
            2 -> Color(0xFF0284C7)
            else -> NormalColor
        }

        DiagramCanvas(
            type = diagType,
            points = points,
            remarkablePoints = solution.remarkablePoints,
            beamLength = solution.structure.length,
            primaryColor = col
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Remarkable Extrema Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Valeurs remarquables & Extrema", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))

                solution.maxMoment?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Moment Max (M_max) :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f kN·m à x = %.2f m".format(Locale.US, it.valueM, it.x), fontWeight = FontWeight.Bold, color = MomentColor)
                    }
                }

                solution.maxShear?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Effort Tranchant Max :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f kN à x = %.2f m".format(Locale.US, it.valueT, it.x), fontWeight = FontWeight.Bold, color = ShearColor)
                    }
                }

                solution.deflectionResult?.let { defl ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Flèche Max (f_max) :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f mm à x = %.2f m (Lim: %.2f mm)".format(Locale.US, kotlin.math.abs(defl.maxDeflection_mm), defl.maxDeflectionX_m, defl.deflectionLimit_mm), fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsTabContent(solution: RdmSolution) {
    if (solution.steps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune étape disponible. Vérifiez la stabilité de la structure.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Démonstration complète pas à pas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Chaque étape présente la formule théorique, la substitution numérique, le résultat et les unités normalisées.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(solution.steps) { step ->
                StepItemCard(step = step)
            }
        }
    }
}

@Composable
private fun ChecksTabContent(solution: RdmSolution) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Vérifications automatiques d'ingénierie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Contrôles de cohérence physique : équilibre statique global, conditions aux limites, sauts de charges et relation différentielle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(solution.checks) { check ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (check.isPassed)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (check.isPassed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (check.isPassed) ReactionGreen else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = check.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = if (check.isPassed) ReactionGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (check.isPassed) "CONFORME" else "NON CONFORME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (check.isPassed) ReactionGreen else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTabContent(
    structure: BeamStructure,
    solution: RdmSolution,
    onUpdateSection: (CrossSection) -> Unit,
    onUpdateMaterial: (Material) -> Unit
) {
    val section = structure.section
    val material = structure.material
    val props = solution.sectionProperties ?: SectionSolver.computeProperties(section)
    val stress = solution.maxStressResult

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Visual Section & Stress Distribution Canvas
        item {
            SectionCanvas(
                section = section,
                props = props,
                stress = stress,
                material = material,
                modifier = Modifier.testTag("section_canvas")
            )
        }

        // 2. Shape Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Forme de la section :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SectionShapeType.entries.forEach { shape ->
                            FilterChip(
                                selected = (section.shapeType == shape),
                                onClick = { onUpdateSection(section.copy(shapeType = shape)) },
                                label = { Text(shape.labelFr, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Dimensional Controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Dimensions géométriques (mm) :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    when (section.shapeType) {
                        SectionShapeType.CIRCLE -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diamètre D :")
                                Text("%.0f mm".format(Locale.US, section.diameter_mm), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = section.diameter_mm.toFloat(),
                                onValueChange = { onUpdateSection(section.copy(diameter_mm = it.toDouble())) },
                                valueRange = 50f..500f
                            )
                        }
                        else -> {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Largeur b :")
                                Text("%.0f mm".format(Locale.US, section.width_mm), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = section.width_mm.toFloat(),
                                onValueChange = { onUpdateSection(section.copy(width_mm = it.toDouble())) },
                                valueRange = 40f..400f
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hauteur totale h :")
                                Text("%.0f mm".format(Locale.US, section.height_mm), fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = section.height_mm.toFloat(),
                                onValueChange = { onUpdateSection(section.copy(height_mm = it.toDouble())) },
                                valueRange = 60f..600f
                            )

                            if (section.shapeType == SectionShapeType.I_BEAM || section.shapeType == SectionShapeType.T_BEAM) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Épaisseur semelle tf :")
                                    Text("%.1f mm".format(Locale.US, section.flangeThickness_mm), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = section.flangeThickness_mm.toFloat(),
                                    onValueChange = { onUpdateSection(section.copy(flangeThickness_mm = it.toDouble())) },
                                    valueRange = 4f..30f
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Épaisseur âme tw :")
                                    Text("%.1f mm".format(Locale.US, section.webThickness_mm), fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = section.webThickness_mm.toFloat(),
                                    onValueChange = { onUpdateSection(section.copy(webThickness_mm = it.toDouble())) },
                                    valueRange = 3f..20f
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Material Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Matériau & Limite élastique (fy) :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Material.ALL_MATERIALS.forEach { mat ->
                            FilterChip(
                                selected = (material.name == mat.name),
                                onClick = { onUpdateMaterial(mat) },
                                label = { Text("${mat.name} (${mat.yieldStrengthFy_MPa.toInt()} MPa)", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Section Characteristics Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Caractéristiques géométriques calculées", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Aire de la section A :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f cm²".format(Locale.US, props.area_cm2), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Position de l'axe neutre (y_inf) :", style = MaterialTheme.typography.bodySmall)
                        Text("%.1f mm".format(Locale.US, props.centroidY_inf_mm), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Moment d'inertie Iz :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f cm⁴".format(Locale.US, props.inertiaIz_cm4), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Module de flexion Wel,inf :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f cm³".format(Locale.US, props.modulusWel_inf_cm3), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Moment statique maximal S_max :", style = MaterialTheme.typography.bodySmall)
                        Text("%.2f cm³".format(Locale.US, props.staticMomentMax_cm3), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. Stress Evaluation Card
        stress?.let { st ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (st.isSafe)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bilan des Contraintes à la Section Critique",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (st.isSafe) ReactionGreen else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (st.isSafe) "SÉCURITAIRE" else "SURCHARGE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Contrainte normale traction (σ_inf) :", style = MaterialTheme.typography.bodySmall)
                            Text("%.2f MPa".format(Locale.US, st.normalStressInf_MPa), fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Contrainte normale compression (σ_sup) :", style = MaterialTheme.typography.bodySmall)
                            Text("%.2f MPa".format(Locale.US, st.normalStressSup_MPa), fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cisaillement maximal (τ_max) :", style = MaterialTheme.typography.bodySmall)
                            Text("%.2f MPa".format(Locale.US, st.maxShearStress_MPa), fontWeight = FontWeight.Bold, color = ShearColor)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Contrainte équivalente Von Mises :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("%.2f MPa".format(Locale.US, st.vonMisesStress_MPa), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taux de travail (η = σ_VM / fy) :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("%.1f %%".format(Locale.US, st.utilizationRatio * 100.0), fontWeight = FontWeight.Bold, color = if (st.isSafe) ReactionGreen else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrussForcesTabContent(solution: TrussSolution) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Tableau des efforts axiaux dans les barres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Effort axial N_i (kN), état de sollicitation (Traction, Compression, Barre Nulle) et contrainte normale σ_i (MPa).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(solution.barResults) { res ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Barre ${res.barName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("(${res.nodeStartName} ➔ ${res.nodeEndName})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Longueur L = %.2f m • σ = %.2f MPa".format(Locale.US, res.length_m, res.stress_MPa), style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val stateColor = when (res.state) {
                            TrussBarState.TRACTION -> Color(0xFF2563EB)
                            TrussBarState.COMPRESSION -> Color(0xFFDC2626)
                            TrussBarState.ZERO_FORCE -> Color(0xFF64748B)
                        }

                        Text(
                            text = "%.2f kN".format(Locale.US, res.force_kN),
                            fontWeight = FontWeight.Bold,
                            color = stateColor,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Surface(
                            color = stateColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = res.state.labelFr,
                                color = stateColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
