package io.routelab.app

import io.routelab.app.data.parser.GpxParser
import io.routelab.app.data.sample.SampleRoutes
import io.routelab.app.domain.analyzer.AiRouteAssistantEngine
import io.routelab.app.domain.analyzer.RouteAnalyzerEngine
import io.routelab.app.domain.model.GeoPoint
import io.routelab.app.domain.model.TerrainType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RouteAnalyzerEngineTest {

    private lateinit var analyzer: RouteAnalyzerEngine
    private lateinit var assistant: AiRouteAssistantEngine
    private lateinit var parser: GpxParser

    @Before
    fun setUp() {
        analyzer = RouteAnalyzerEngine()
        assistant = AiRouteAssistantEngine()
        parser = GpxParser()
    }

    @Test
    fun testHaversineDistanceAndElevationAnalysis() {
        val samplePoints = listOf(
            GeoPoint(-6.550, 106.850, 200.0),
            GeoPoint(-6.560, 106.850, 250.0),
            GeoPoint(-6.570, 106.850, 320.0),
            GeoPoint(-6.580, 106.850, 300.0)
        )

        val analysis = analyzer.analyze(
            routeName = "Test Mini Route",
            rawPoints = samplePoints,
            targetCotHours = 0.5
        )

        assertTrue("Total distance should be greater than 2 km", analysis.totalDistanceKm > 2.0)
        assertTrue("Elevation gain should be positive", analysis.elevationGainM > 0.0)
        assertTrue("Elevation loss should be positive", analysis.elevationLossM > 0.0)
        assertEquals("Highest point matches max smoothed elevation", 320.0, analysis.highestPointM, 5.0)
    }

    @Test
    fun testSentulSampleRouteClimbDetection() {
        val sentulRoute = SampleRoutes.getAvailableRoutes().first { it.id == "sentul-km0" }
        val analysis = analyzer.analyze(
            routeName = sentulRoute.name,
            rawPoints = sentulRoute.points,
            targetCotHours = sentulRoute.defaultCotHours,
            roadInfoOverride = sentulRoute.roadInfo
        )

        assertTrue("Distance should be around 10-50 km", analysis.totalDistanceKm in 10.0..60.0)
        assertTrue("Climbs should be detected", analysis.climbs.isNotEmpty())

        val firstClimb = analysis.climbs.first()
        assertTrue("Climb start KM should be >= 0", firstClimb.startKm >= 0.0)
        assertTrue("Climb end KM should be > start KM", firstClimb.endKm > firstClimb.startKm)
        assertTrue("Climb gradient should be positive", firstClimb.avgGradientPct > 0.0)
        assertTrue("Climb category must be assigned", firstClimb.category.isNotEmpty())
    }

    @Test
    fun testDifficultyRatingScoring() {
        val flatRoute = listOf(
            GeoPoint(-6.10, 106.70, 10.0),
            GeoPoint(-6.11, 106.71, 12.0),
            GeoPoint(-6.12, 106.72, 11.0),
            GeoPoint(-6.13, 106.73, 10.0)
        )
        val flatAnalysis = analyzer.analyze("Flat Route", flatRoute)
        assertEquals("Flat route should have Mudah level", "Mudah", flatAnalysis.difficulty.level)
        assertTrue("Score should be < 35 for flat short route", flatAnalysis.difficulty.score < 35)
        assertTrue("Reason should not be empty", flatAnalysis.difficulty.reason.isNotBlank())
        assertFalse("Reason must not contain em dash per R-02", flatAnalysis.difficulty.reason.contains("—"))
    }

    @Test
    fun testCotPlanningLogic() {
        val sentulRoute = SampleRoutes.getAvailableRoutes().first { it.id == "sentul-km0" }
        val analysis = analyzer.analyze(
            routeName = sentulRoute.name,
            rawPoints = sentulRoute.points,
            targetCotHours = 3.0
        )

        val cot = analysis.cotPlan
        assertEquals(3.0, cot.targetHours, 0.01)
        assertTrue("Average required speed should be positive", cot.avgRequiredSpeedKmh > 0.0)
        assertTrue("Moving minutes should be positive", cot.totalMovingTimeMinutes > 0.0)
        assertTrue("Segment targets must exist", cot.segmentTargets.isNotEmpty())

        // Check if climbing segment has lower speed target than flat or descending
        val climbSeg = cot.segmentTargets.find { it.terrain == TerrainType.CLIMBING }
        val flatSeg = cot.segmentTargets.find { it.terrain == TerrainType.FLAT || it.terrain == TerrainType.DESCENDING }
        if (climbSeg != null && flatSeg != null) {
            assertTrue(
                "Climbing speed (${climbSeg.targetPaceKmh}) should be <= flat speed (${flatSeg.targetPaceKmh})",
                climbSeg.targetPaceKmh <= flatSeg.targetPaceKmh
            )
        }
    }

    @Test
    fun testGpxParserWithTrackPoints() {
        val gpxXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="RouteLab">
                <trk>
                    <name>Test Ride</name>
                    <trkseg>
                        <trkpt lat="-6.5583" lon="106.8741">
                            <ele>182.5</ele>
                            <time>2026-09-25T06:00:00Z</time>
                        </trkpt>
                        <trkpt lat="-6.5650" lon="106.8780">
                            <ele>210.0</ele>
                            <time>2026-09-25T06:05:00Z</time>
                        </trkpt>
                        <trkpt lat="-6.5720" lon="106.8820">
                            <ele>245.0</ele>
                            <time>2026-09-25T06:10:00Z</time>
                        </trkpt>
                    </trkseg>
                </trk>
            </gpx>
        """.trimIndent()

        val parsed = parser.parse(gpxXml)
        assertEquals(3, parsed.size)
        assertEquals(-6.5583, parsed[0].latitude, 0.0001)
        assertEquals(106.8741, parsed[0].longitude, 0.0001)
        assertEquals(182.5, parsed[0].elevation, 0.1)
        assertEquals("2026-09-25T06:00:00Z", parsed[0].time)
    }

    @Test
    fun testAiAssistantContextualAnswers() {
        val puncakRoute = SampleRoutes.getAvailableRoutes().first { it.id == "puncak-pass" }
        val analysis = analyzer.analyze(
            routeName = puncakRoute.name,
            rawPoints = puncakRoute.points,
            targetCotHours = puncakRoute.defaultCotHours
        )

        val climbAnswer = assistant.answerQuestion("Tanjakan paling berat di mana?", analysis)
        assertTrue("Answer should mention climb details", climbAnswer.contains("Tanjakan") || climbAnswer.contains("KM"))
        assertFalse("Answer must not contain em dash per R-02", climbAnswer.contains("—"))

        val cotAnswer = assistant.answerQuestion("Bagaimana strategi target COT 5 jam?", analysis)
        assertTrue("Answer should mention COT details", cotAnswer.contains("km/jam") || cotAnswer.contains("Target"))
        assertFalse("Answer must not contain em dash per R-02", cotAnswer.contains("—"))

        val fuelingAnswer = assistant.answerQuestion("Di mana sebaiknya saya fueling?", analysis)
        assertTrue("Answer should contain fueling advice", fuelingAnswer.contains("KM") || fuelingAnswer.contains("nutrisi") || fuelingAnswer.contains("Fueling"))
        assertFalse("Answer must not contain em dash per R-02", fuelingAnswer.contains("—"))
    }
}
