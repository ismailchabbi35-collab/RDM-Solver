package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.solver.RdmSolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RdmMode(val labelFr: String, val labelAr: String, val labelEn: String) {
    BEAM("Poutre Isostatique", "جائز بـ RDM", "Isostatic Beam"),
    TRUSS("Treillis Plan", "جمالون / شبكة", "2D Planar Truss")
}

data class RdmUiState(
    val mode: RdmMode = RdmMode.BEAM,
    val structure: BeamStructure = BeamPresets.simplySupportedUdl,
    val solution: RdmSolution = RdmSolver.solve(BeamPresets.simplySupportedUdl),
    val trussStructure: TrussStructure = TrussPresets.warrenTruss,
    val trussSolution: TrussSolution = com.example.solver.TrussSolver.solve(TrussPresets.warrenTruss),
    val selectedTabIndex: Int = 0, // 0: Structure, 1: Diagrammes, 2: Section & Stresses, 3: Pas-à-Pas, 4: Contrôles
    val selectedDiagramIndex: Int = 1, // 0: T(x), 1: M(x), 2: f(x), 3: N(x)
    val selectedLanguage: String = "FR", // "FR", "AR", "EN"
    val showAddSupportDialog: Boolean = false,
    val showAddLoadDialog: Boolean = false,
    val isSolving: Boolean = false
)

class RdmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RdmUiState())
    val uiState: StateFlow<RdmUiState> = _uiState.asStateFlow()

    init {
        recalculate()
    }

    fun setMode(mode: RdmMode) {
        _uiState.value = _uiState.value.copy(mode = mode, selectedTabIndex = 0)
        recalculate()
    }

    fun loadTrussPreset(preset: TrussStructure) {
        _uiState.value = _uiState.value.copy(
            trussStructure = preset,
            mode = RdmMode.TRUSS
        )
        recalculate()
    }

    fun loadPreset(preset: BeamStructure) {
        _uiState.value = _uiState.value.copy(
            structure = preset,
            mode = RdmMode.BEAM
        )
        recalculate()
    }

    fun updateBeamLength(newLength: Double) {
        val clampedLength = newLength.coerceIn(1.0, 30.0)
        // Adjust any supports or loads exceeding new length
        val updatedSupports = _uiState.value.structure.supports.map {
            if (it.position > clampedLength) it.copy(position = clampedLength) else it
        }
        val updatedLoads = _uiState.value.structure.loads.map {
            val start = it.positionStart.coerceAtMost(clampedLength)
            val end = it.positionEnd.coerceAtMost(clampedLength)
            it.copy(positionStart = start, positionEnd = end)
        }

        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(
                length = clampedLength,
                supports = updatedSupports,
                loads = updatedLoads
            )
        )
        recalculate()
    }

    fun addSupport(support: Support) {
        val updated = _uiState.value.structure.supports + support
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(supports = updated),
            showAddSupportDialog = false
        )
        recalculate()
    }

    fun removeSupport(supportId: String) {
        val updated = _uiState.value.structure.supports.filterNot { it.id == supportId }
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(supports = updated)
        )
        recalculate()
    }

    fun addLoad(load: Load) {
        val updated = _uiState.value.structure.loads + load
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(loads = updated),
            showAddLoadDialog = false
        )
        recalculate()
    }

    fun removeLoad(loadId: String) {
        val updated = _uiState.value.structure.loads.filterNot { it.id == loadId }
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(loads = updated)
        )
        recalculate()
    }

    fun setTabIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = index)
    }

    fun setDiagramIndex(index: Int) {
        _uiState.value = _uiState.value.copy(selectedDiagramIndex = index)
    }

    fun setLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun setShowAddSupportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddSupportDialog = show)
    }

    fun setShowAddLoadDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddLoadDialog = show)
    }

    fun updateSection(section: CrossSection) {
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(section = section)
        )
        recalculate()
    }

    fun updateMaterial(material: Material) {
        _uiState.value = _uiState.value.copy(
            structure = _uiState.value.structure.copy(material = material)
        )
        recalculate()
    }

    fun recalculate() {
        val beamSol = RdmSolver.solve(_uiState.value.structure)
        val trussSol = com.example.solver.TrussSolver.solve(_uiState.value.trussStructure)
        _uiState.value = _uiState.value.copy(
            solution = beamSol,
            trussSolution = trussSol
        )
    }
}
