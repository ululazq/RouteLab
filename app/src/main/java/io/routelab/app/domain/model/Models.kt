package io.routelab.app.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double = 0.0,
    val time: String? = null
)

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val distanceMeters: Double,
    val gradientPct: Double = 0.0
)

enum class TerrainType(val displayName: String) {
    FLAT("Datar"),
    ROLLING("Bergelombang"),
    CLIMBING("Tanjakan"),
    DESCENDING("Turunan")
}

data class ClimbSegment(
    val id: Int,
    val name: String,
    val startKm: Double,
    val endKm: Double,
    val distanceKm: Double,
    val elevationGainM: Double,
    val avgGradientPct: Double,
    val maxGradientPct: Double,
    val category: String, // Cat 4, Cat 3, Cat 2, Cat 1, HC
    val difficultyScore: Double
)

data class RouteSegment(
    val segmentNumber: Int,
    val startKm: Double,
    val endKm: Double,
    val terrain: TerrainType,
    val description: String,
    val recommendedEffort: String,
    val avgGradientPct: Double
)

data class RoadCharacteristic(
    val roadType: String,
    val surface: String,
    val accessibility: String
)

data class DifficultyRating(
    val score: Int,
    val level: String, // Easy, Moderate, Challenging, Very Challenging
    val reason: String
)

data class CotPaceTarget(
    val segmentId: Int,
    val startKm: Double,
    val endKm: Double,
    val terrain: TerrainType,
    val targetPaceKmh: Double,
    val estimatedMinutes: Double,
    val effortStrategy: String
)

data class CotPlan(
    val targetHours: Double,
    val avgRequiredSpeedKmh: Double,
    val segmentTargets: List<CotPaceTarget>,
    val stopsAllowanceMinutes: Double,
    val totalMovingTimeMinutes: Double
)

data class RestFuelingRecommendation(
    val kmMark: Double,
    val type: String, // Rest, Fueling, Hydration
    val title: String,
    val advice: String
)

data class RouteIntelligenceSummary(
    val headline: String,
    val criticalSection: String,
    val strategy: String,
    val fuelingAdvice: String
)

data class RoutePoi(
    val name: String,
    val kmMark: Double,
    val type: String // Summit, Water, Caution, Landmark
)

data class RouteAnalysis(
    val routeId: String,
    val routeName: String,
    val points: List<RoutePoint>,
    val totalDistanceKm: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val highestPointM: Double,
    val lowestPointM: Double,
    val avgGradientPct: Double,
    val maxGradientPct: Double,
    val climbs: List<ClimbSegment>,
    val segments: List<RouteSegment>,
    val roadInfo: RoadCharacteristic,
    val difficulty: DifficultyRating,
    val cotPlan: CotPlan,
    val restFueling: List<RestFuelingRecommendation>,
    val summary: RouteIntelligenceSummary,
    val pois: List<RoutePoi> = emptyList()
)
