package io.routelab.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.routelab.app.data.parser.GpxParser
import io.routelab.app.data.sample.SampleRoutes
import io.routelab.app.domain.analyzer.AiRouteAssistantEngine
import io.routelab.app.domain.analyzer.RouteAnalyzerEngine
import io.routelab.app.domain.model.RouteAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RouteLabUiState {
    object Loading : RouteLabUiState
    data class Content(
        val availableRoutes: List<SampleRoutes.SampleRouteDef>,
        val currentRouteDef: SampleRoutes.SampleRouteDef,
        val analysis: RouteAnalysis,
        val selectedDistanceMeters: Double,
        val targetCotHours: Double,
        val activeTabIndex: Int,
        val assistantMessages: List<AiRouteAssistantEngine.AssistantMessage>
    ) : RouteLabUiState
    data class Error(val message: String) : RouteLabUiState
}

class RouteLabViewModel(
    private val analyzerEngine: RouteAnalyzerEngine = RouteAnalyzerEngine(),
    private val assistantEngine: AiRouteAssistantEngine = AiRouteAssistantEngine(),
    private val gpxParser: GpxParser = GpxParser()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RouteLabUiState>(RouteLabUiState.Loading)
    val uiState: StateFlow<RouteLabUiState> = _uiState.asStateFlow()

    init {
        loadInitialRoute()
    }

    private fun loadInitialRoute() {
        val routes = SampleRoutes.getAvailableRoutes()
        val defaultRoute = routes.first()
        loadRoute(defaultRoute, routes)
    }

    fun selectRoute(routeDef: SampleRoutes.SampleRouteDef) {
        val currentState = _uiState.value
        val routes = if (currentState is RouteLabUiState.Content) currentState.availableRoutes else SampleRoutes.getAvailableRoutes()
        loadRoute(routeDef, routes)
    }

    private fun loadRoute(routeDef: SampleRoutes.SampleRouteDef, availableRoutes: List<SampleRoutes.SampleRouteDef>) {
        _uiState.value = RouteLabUiState.Loading
        viewModelScope.launch {
            try {
                val analysis = analyzerEngine.analyze(
                    routeName = routeDef.name,
                    rawPoints = routeDef.points,
                    targetCotHours = routeDef.defaultCotHours,
                    roadInfoOverride = routeDef.roadInfo
                )
                val initialQuestion = "Tanjakan paling berat di mana?"
                val initialAnswer = assistantEngine.answerQuestion(initialQuestion, analysis)

                _uiState.value = RouteLabUiState.Content(
                    availableRoutes = availableRoutes,
                    currentRouteDef = routeDef,
                    analysis = analysis,
                    selectedDistanceMeters = 0.0,
                    targetCotHours = routeDef.defaultCotHours,
                    activeTabIndex = 0,
                    assistantMessages = listOf(
                        AiRouteAssistantEngine.AssistantMessage(isUser = true, text = initialQuestion),
                        AiRouteAssistantEngine.AssistantMessage(isUser = false, text = initialAnswer)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = RouteLabUiState.Error("Gagal menganalisis rute: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun scrubDistance(distanceMeters: Double) {
        val state = _uiState.value
        if (state is RouteLabUiState.Content) {
            _uiState.value = state.copy(selectedDistanceMeters = distanceMeters)
        }
    }

    fun updateTargetCotHours(newHours: Double) {
        val state = _uiState.value
        if (state is RouteLabUiState.Content) {
            val updatedPlan = analyzerEngine.calculateCotPlan(state.analysis.segments, newHours)
            val updatedAnalysis = state.analysis.copy(cotPlan = updatedPlan)
            _uiState.value = state.copy(
                targetCotHours = newHours,
                analysis = updatedAnalysis
            )
        }
    }

    fun selectTab(index: Int) {
        val state = _uiState.value
        if (state is RouteLabUiState.Content) {
            _uiState.value = state.copy(activeTabIndex = index)
        }
    }

    fun askAssistant(question: String) {
        val state = _uiState.value
        if (state is RouteLabUiState.Content) {
            val answer = assistantEngine.answerQuestion(question, state.analysis)
            val updatedMessages = state.assistantMessages + listOf(
                AiRouteAssistantEngine.AssistantMessage(isUser = true, text = question),
                AiRouteAssistantEngine.AssistantMessage(isUser = false, text = answer)
            )
            _uiState.value = state.copy(assistantMessages = updatedMessages)
        }
    }

    fun importGpxContent(gpxXml: String, fileName: String) {
        _uiState.value = RouteLabUiState.Loading
        viewModelScope.launch {
            try {
                val points = gpxParser.parse(gpxXml)
                if (points.size < 2) {
                    _uiState.value = RouteLabUiState.Error("File GPX tidak memiliki titik koordinat valid yang cukup.")
                    return@launch
                }
                val customRoute = SampleRoutes.SampleRouteDef(
                    id = "custom-${System.currentTimeMillis()}",
                    name = fileName.removeSuffix(".gpx"),
                    location = "Rute Impor",
                    description = "Rute hasil impor dari berkas GPX pengguna.",
                    roadInfo = io.routelab.app.domain.model.RoadCharacteristic(
                        roadType = "Data GPX Pengguna",
                        surface = "Tergantung Lapangan",
                        accessibility = "Periksa regulasi lokal rute"
                    ),
                    defaultCotHours = 4.0,
                    points = points
                )
                val allRoutes = listOf(customRoute) + SampleRoutes.getAvailableRoutes()
                loadRoute(customRoute, allRoutes)
            } catch (e: Exception) {
                _uiState.value = RouteLabUiState.Error("Gagal membaca file GPX: ${e.message}")
            }
        }
    }
}
