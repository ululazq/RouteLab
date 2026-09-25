package io.routelab.app.domain.analyzer

import io.routelab.app.domain.model.*
import kotlin.math.*

class RouteAnalyzerEngine {

    fun analyze(
        routeName: String,
        rawPoints: List<GeoPoint>,
        targetCotHours: Double = 4.5,
        roadInfoOverride: RoadCharacteristic? = null
    ): RouteAnalysis {
        require(rawPoints.size >= 2) { "Route must contain at least 2 coordinate points" }

        val smoothedPoints = smoothElevation(rawPoints)
        val routePoints = computeCumulativeDistancesAndGradients(smoothedPoints)

        val totalDistanceMeters = routePoints.last().distanceMeters
        val totalDistanceKm = totalDistanceMeters / 1000.0

        var elevationGainM = 0.0
        var elevationLossM = 0.0
        var highestPointM = routePoints.first().elevation
        var lowestPointM = routePoints.first().elevation

        for (i in 1 until routePoints.size) {
            val prev = routePoints[i - 1]
            val curr = routePoints[i]
            val diff = curr.elevation - prev.elevation
            if (diff > 0.0) {
                elevationGainM += diff
            } else {
                elevationLossM += abs(diff)
            }
            if (curr.elevation > highestPointM) highestPointM = curr.elevation
            if (curr.elevation < lowestPointM) lowestPointM = curr.elevation
        }

        val avgGradientPct = if (totalDistanceMeters > 0) {
            (elevationGainM / totalDistanceMeters) * 100.0
        } else 0.0

        var maxGradientPct = 0.0
        routePoints.forEach { pt ->
            if (pt.gradientPct > maxGradientPct) {
                maxGradientPct = pt.gradientPct
            }
        }

        val climbs = detectClimbs(routePoints)
        val segments = segmentRoute(routePoints, totalDistanceKm)
        val difficulty = calculateDifficulty(totalDistanceKm, elevationGainM, climbs, maxGradientPct)
        val cotPlan = calculateCotPlan(segments, targetCotHours)
        val restFueling = generateRestFuelingPlan(routePoints, climbs, totalDistanceKm)
        val summary = generateSummary(totalDistanceKm, elevationGainM, climbs, segments, difficulty)

        val roadInfo = roadInfoOverride ?: RoadCharacteristic(
            roadType = "Campuran (Sekunder dan Tersier)",
            surface = "Mayoritas Aspal",
            accessibility = "Akses terbuka untuk pesepeda"
        )

        val pois = generatePois(climbs, totalDistanceKm)

        return RouteAnalysis(
            routeId = "route-${System.currentTimeMillis()}",
            routeName = routeName,
            points = routePoints,
            totalDistanceKm = roundToOneDecimal(totalDistanceKm),
            elevationGainM = roundToOneDecimal(elevationGainM),
            elevationLossM = roundToOneDecimal(elevationLossM),
            highestPointM = roundToOneDecimal(highestPointM),
            lowestPointM = roundToOneDecimal(lowestPointM),
            avgGradientPct = roundToOneDecimal(avgGradientPct),
            maxGradientPct = roundToOneDecimal(maxGradientPct),
            climbs = climbs,
            segments = segments,
            roadInfo = roadInfo,
            difficulty = difficulty,
            cotPlan = cotPlan,
            restFueling = restFueling,
            summary = summary,
            pois = pois
        )
    }

    private fun smoothElevation(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size < 5) return points
        val windowSize = 3
        val result = mutableListOf<GeoPoint>()
        for (i in points.indices) {
            val start = max(0, i - windowSize)
            val end = min(points.size - 1, i + windowSize)
            var sum = 0.0
            for (j in start..end) {
                sum += points[j].elevation
            }
            val avg = sum / (end - start + 1)
            result.add(points[i].copy(elevation = avg))
        }
        return result
    }

    private fun computeCumulativeDistancesAndGradients(points: List<GeoPoint>): List<RoutePoint> {
        val result = mutableListOf<RoutePoint>()
        var cumulativeDistance = 0.0

        result.add(
            RoutePoint(
                latitude = points[0].latitude,
                longitude = points[0].longitude,
                elevation = points[0].elevation,
                distanceMeters = 0.0,
                gradientPct = 0.0
            )
        )

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val stepDist = haversineDistanceMeters(prev.latitude, prev.longitude, curr.latitude, curr.longitude)
            cumulativeDistance += stepDist

            val elevDiff = curr.elevation - prev.elevation
            val gradient = if (stepDist > 5.0) {
                ((elevDiff / stepDist) * 100.0).coerceIn(-25.0, 25.0)
            } else 0.0

            result.add(
                RoutePoint(
                    latitude = curr.latitude,
                    longitude = curr.longitude,
                    elevation = curr.elevation,
                    distanceMeters = cumulativeDistance,
                    gradientPct = gradient
                )
            )
        }
        return result
    }

    private fun detectClimbs(points: List<RoutePoint>): List<ClimbSegment> {
        val climbs = mutableListOf<ClimbSegment>()
        var inClimb = false
        var climbStartIndex = 0
        var climbGain = 0.0
        var climbMaxGrad = 0.0

        val minClimbDistanceMeters = 800.0
        val minClimbElevationGainMeters = 25.0

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val elevDiff = curr.elevation - prev.elevation
            val stepDist = curr.distanceMeters - prev.distanceMeters

            if (stepDist <= 0.0) continue

            val stepGrad = (elevDiff / stepDist) * 100.0

            if (stepGrad >= 2.0) {
                if (!inClimb) {
                    inClimb = true
                    climbStartIndex = i - 1
                    climbGain = max(0.0, elevDiff)
                    climbMaxGrad = stepGrad
                } else {
                    if (elevDiff > 0) climbGain += elevDiff
                    if (stepGrad > climbMaxGrad) climbMaxGrad = stepGrad
                }
            } else if (inClimb) {
                // Check if descent or flat is prolonged (over 300m)
                val distFromStart = curr.distanceMeters - points[climbStartIndex].distanceMeters
                if (elevDiff < -3.0 || stepGrad < -1.5) {
                    // End climb candidate
                    val climbDist = points[i - 1].distanceMeters - points[climbStartIndex].distanceMeters
                    if (climbDist >= minClimbDistanceMeters && climbGain >= minClimbElevationGainMeters) {
                        val startKm = points[climbStartIndex].distanceMeters / 1000.0
                        val endKm = points[i - 1].distanceMeters / 1000.0
                        val avgGrad = (climbGain / climbDist) * 100.0
                        val score = climbDist * (avgGrad / 100.0).pow(2) * 1000.0
                        val cat = categorizeClimb(climbDist, avgGrad)

                        climbs.add(
                            ClimbSegment(
                                id = climbs.size + 1,
                                name = "Tanjakan #${climbs.size + 1}",
                                startKm = roundToOneDecimal(startKm),
                                endKm = roundToOneDecimal(endKm),
                                distanceKm = roundToOneDecimal(climbDist / 1000.0),
                                elevationGainM = roundToOneDecimal(climbGain),
                                avgGradientPct = roundToOneDecimal(avgGrad),
                                maxGradientPct = roundToOneDecimal(climbMaxGrad),
                                category = cat,
                                difficultyScore = roundToOneDecimal(score)
                            )
                        )
                    }
                    inClimb = false
                    climbGain = 0.0
                    climbMaxGrad = 0.0
                }
            }
        }

        // Flush active climb at the end of route
        if (inClimb) {
            val lastIdx = points.size - 1
            val climbDist = points[lastIdx].distanceMeters - points[climbStartIndex].distanceMeters
            if (climbDist >= minClimbDistanceMeters && climbGain >= minClimbElevationGainMeters) {
                val startKm = points[climbStartIndex].distanceMeters / 1000.0
                val endKm = points[lastIdx].distanceMeters / 1000.0
                val avgGrad = (climbGain / climbDist) * 100.0
                val score = climbDist * (avgGrad / 100.0).pow(2) * 1000.0
                val cat = categorizeClimb(climbDist, avgGrad)

                climbs.add(
                    ClimbSegment(
                        id = climbs.size + 1,
                        name = "Tanjakan #${climbs.size + 1}",
                        startKm = roundToOneDecimal(startKm),
                        endKm = roundToOneDecimal(endKm),
                        distanceKm = roundToOneDecimal(climbDist / 1000.0),
                        elevationGainM = roundToOneDecimal(climbGain),
                        avgGradientPct = roundToOneDecimal(avgGrad),
                        maxGradientPct = roundToOneDecimal(climbMaxGrad),
                        category = cat,
                        difficultyScore = roundToOneDecimal(score)
                    )
                )
            }
        }

        return climbs
    }

    private fun categorizeClimb(distanceMeters: Double, avgGradientPct: Double): String {
        // Standard UCI climb scoring factor: dist_m * (gradient)^2
        val factor = distanceMeters * (avgGradientPct / 100.0).pow(2)
        return when {
            factor >= 80.0 || (distanceMeters >= 10000 && avgGradientPct >= 6.0) -> "Kategori HC"
            factor >= 40.0 || (distanceMeters >= 7000 && avgGradientPct >= 5.5) -> "Kategori 1"
            factor >= 20.0 || (distanceMeters >= 4000 && avgGradientPct >= 5.0) -> "Kategori 2"
            factor >= 8.0 || (distanceMeters >= 2000 && avgGradientPct >= 4.0) -> "Kategori 3"
            else -> "Kategori 4"
        }
    }

    private fun segmentRoute(points: List<RoutePoint>, totalDistanceKm: Double): List<RouteSegment> {
        val segmentCount = when {
            totalDistanceKm < 20.0 -> 3
            totalDistanceKm < 50.0 -> 4
            totalDistanceKm < 90.0 -> 5
            else -> 6
        }

        val stepDistMeters = (totalDistanceKm * 1000.0) / segmentCount
        val segments = mutableListOf<RouteSegment>()

        for (s in 0 until segmentCount) {
            val startDist = s * stepDistMeters
            val endDist = if (s == segmentCount - 1) totalDistanceKm * 1000.0 else (s + 1) * stepDistMeters

            val segPoints = points.filter { it.distanceMeters in startDist..endDist }
            if (segPoints.isEmpty()) continue

            var segGain = 0.0
            var segLoss = 0.0
            for (i in 1 until segPoints.size) {
                val d = segPoints[i].elevation - segPoints[i - 1].elevation
                if (d > 0) segGain += d else segLoss += abs(d)
            }

            val segDistM = endDist - startDist
            val netGradient = if (segDistM > 0) ((segGain - segLoss) / segDistM) * 100.0 else 0.0

            val terrain = when {
                netGradient > 2.5 || (segGain > 150.0 && segGain > segLoss * 2.0) -> TerrainType.CLIMBING
                netGradient < -2.5 || (segLoss > 150.0 && segLoss > segGain * 2.0) -> TerrainType.DESCENDING
                segGain > 50.0 && segLoss > 50.0 -> TerrainType.ROLLING
                else -> TerrainType.FLAT
            }

            val (effort, desc) = when (terrain) {
                TerrainType.CLIMBING -> Pair("Hemat Tenaga (Zone 2 - 3)", "Sektor tanjakan, atur ritme kayuhan dan jaga detak jantung.")
                TerrainType.DESCENDING -> Pair("Pemulihan / Konsentrasi", "Turunan cepat, prioritaskan kendali rem dan posisi badan.")
                TerrainType.ROLLING -> Pair("Stabil Berkelanjutan", "Kontur naik-turun sedang, manfaatkan momentum saat menurun.")
                TerrainType.FLAT -> Pair("Pertahankan Pace (Zone 2)", "Permukaan datar, jaga cadence stabil dan efisiensi tenaga.")
            }

            segments.add(
                RouteSegment(
                    segmentNumber = s + 1,
                    startKm = roundToOneDecimal(startDist / 1000.0),
                    endKm = roundToOneDecimal(endDist / 1000.0),
                    terrain = terrain,
                    description = desc,
                    recommendedEffort = effort,
                    avgGradientPct = roundToOneDecimal(netGradient)
                )
            )
        }
        return segments
    }

    private fun calculateDifficulty(
        totalDistanceKm: Double,
        elevationGainM: Double,
        climbs: List<ClimbSegment>,
        maxGradientPct: Double
    ): DifficultyRating {
        val gainPerKm = if (totalDistanceKm > 0) elevationGainM / totalDistanceKm else 0.0
        val distFactor = totalDistanceKm * 0.35
        val gainFactor = elevationGainM / 20.0
        val climbCountFactor = climbs.size * 3.5
        val maxGradFactor = maxGradientPct * 1.5

        val rawScore = distFactor + gainFactor + climbCountFactor + maxGradFactor
        val score = rawScore.toInt().coerceIn(10, 100)

        val (level, reason) = when {
            score >= 80 -> Pair(
                "Sangat Menantang",
                "Tingkat kesulitan sangat tinggi: rute memiliki total elevasi +${elevationGainM.toInt()} m dengan ${climbs.size} sektor tanjakan signifikan (gradien maks ${roundToOneDecimal(maxGradientPct)}%). Membutuhkan persiapan fisik prima dan pacing disiplin."
            )
            score >= 55 -> Pair(
                "Menantang",
                "Didominasi oleh tantangan akumulasi elevasi +${elevationGainM.toInt()} m dan ${climbs.size} sektor tanjakan. Karakter rute menguji ketahanan terutama di paruh kedua perjalanan."
            )
            score >= 35 -> Pair(
                "Moderat",
                "Karakteristik rute seimbang dengan kontur rolling dan tanjakan terkendali (+${elevationGainM.toInt()} m). Cocok untuk endurance ride terstruktur."
            )
            else -> Pair(
                "Mudah",
                "Rute relatif datar dengan elevasi minim (+${elevationGainM.toInt()} m). Sangat ideal untuk recovery ride atau latihan kecepatan konstan."
            )
        }

        return DifficultyRating(
            score = score,
            level = level,
            reason = reason
        )
    }

    fun calculateCotPlan(segments: List<RouteSegment>, targetCotHours: Double): CotPlan {
        val totalDistanceKm = segments.lastOrNull()?.endKm ?: 50.0
        val stopsAllowanceMinutes = (totalDistanceKm / 25.0 * 5.0).coerceAtLeast(10.0)
        val totalTargetMinutes = targetCotHours * 60.0
        val movingMinutes = (totalTargetMinutes - stopsAllowanceMinutes).coerceAtLeast(30.0)

        // Initial speed weights based on terrain
        val terrainFactors = segments.map { seg ->
            when (seg.terrain) {
                TerrainType.FLAT -> 1.0
                TerrainType.ROLLING -> 0.88
                TerrainType.CLIMBING -> (1.0 - (max(2.0, seg.avgGradientPct) * 0.07)).coerceIn(0.35, 0.75)
                TerrainType.DESCENDING -> 1.35
            }
        }

        // Calculate baseline flat speed V_base such that sum(dist_i / (V_base * factor_i)) = movingHours
        val movingHours = movingMinutes / 60.0
        var weightedHoursSum = 0.0
        for (i in segments.indices) {
            val segDist = segments[i].endKm - segments[i].startKm
            weightedHoursSum += segDist / terrainFactors[i]
        }
        val baseSpeedKmh = weightedHoursSum / movingHours

        val paceTargets = segments.mapIndexed { i, seg ->
            val segDist = seg.endKm - seg.startKm
            val speed = (baseSpeedKmh * terrainFactors[i]).coerceIn(8.0, 48.0)
            val durationMin = (segDist / speed) * 60.0
            val strategy = when (seg.terrain) {
                TerrainType.CLIMBING -> "Hemat Tenaga: Jaga irama kayuhan"
                TerrainType.DESCENDING -> "Pemulihan Aktif: Fokus keamanan"
                TerrainType.ROLLING -> "Pace Menengah: Ikuti alur jalan"
                TerrainType.FLAT -> "Pertahankan Target: Aerodinamis konsisten"
            }
            CotPaceTarget(
                segmentId = seg.segmentNumber,
                startKm = seg.startKm,
                endKm = seg.endKm,
                terrain = seg.terrain,
                targetPaceKmh = roundToOneDecimal(speed),
                estimatedMinutes = roundToOneDecimal(durationMin),
                effortStrategy = strategy
            )
        }

        val avgRequiredSpeed = totalDistanceKm / targetCotHours

        return CotPlan(
            targetHours = roundToOneDecimal(targetCotHours),
            avgRequiredSpeedKmh = roundToOneDecimal(avgRequiredSpeed),
            segmentTargets = paceTargets,
            stopsAllowanceMinutes = roundToOneDecimal(stopsAllowanceMinutes),
            totalMovingTimeMinutes = roundToOneDecimal(movingMinutes)
        )
    }

    private fun generateRestFuelingPlan(
        points: List<RoutePoint>,
        climbs: List<ClimbSegment>,
        totalDistanceKm: Double
    ): List<RestFuelingRecommendation> {
        val recs = mutableListOf<RestFuelingRecommendation>()

        recs.add(
            RestFuelingRecommendation(
                kmMark = 0.0,
                type = "Hydration",
                title = "Hidrasi Pra-Start",
                advice = "Minum 300 - 500 ml cairan elektrolit sebelum memulai gowes."
            )
        )

        climbs.forEach { climb ->
            val fuelingKm = (climb.startKm - 3.0).coerceAtLeast(1.0)
            recs.add(
                RestFuelingRecommendation(
                    kmMark = roundToOneDecimal(fuelingKm),
                    type = "Fueling",
                    title = "Fueling Sebelum ${climb.name}",
                    advice = "Konsumsi gel energi atau 30-40g karbohidrat 15 menit sebelum masuk ${climb.name} (KM ${climb.startKm})."
                )
            )
        }

        var currentKm = 30.0
        while (currentKm < totalDistanceKm - 10.0) {
            val km = currentKm
            val isNearClimb = climbs.any { abs(it.startKm - km) < 5.0 }
            if (!isNearClimb) {
                recs.add(
                    RestFuelingRecommendation(
                        kmMark = roundToOneDecimal(km),
                        type = "Rest",
                        title = "Pemberhentian Terjadwal KM ${roundToOneDecimal(km)}",
                        advice = "Isi ulang botol bidon, istirahat 5-8 menit, dan peregangan punggung/bahu."
                    )
                )
            }
            currentKm += 35.0
        }

        return recs.sortedBy { it.kmMark }
    }

    private fun generateSummary(
        totalDistanceKm: Double,
        elevationGainM: Double,
        climbs: List<ClimbSegment>,
        segments: List<RouteSegment>,
        difficulty: DifficultyRating
    ): RouteIntelligenceSummary {
        val headline = "${roundToOneDecimal(totalDistanceKm)} km : +${elevationGainM.toInt()} m elevasi : Status ${difficulty.level}"

        val steepestClimb = climbs.maxByOrNull { it.maxGradientPct }
        val longestClimb = climbs.maxByOrNull { it.distanceKm }

        val criticalSection = if (steepestClimb != null) {
            "Sektor kritis berada pada KM ${steepestClimb.startKm} - ${steepestClimb.endKm} (${steepestClimb.name}), dengan gradien mencapai ${steepestClimb.maxGradientPct}% dan jarak ${steepestClimb.distanceKm} km."
        } else {
            "Tidak ditemukan tanjakan terjal ekstrem. Perhatikan konsistensi tenaga menghadapi angin dan jarak tempuh."
        }

        val strategy = if (climbs.isNotEmpty()) {
            val firstClimb = climbs.first()
            "Hindari membuang tenaga di ${firstClimb.startKm.toInt()} km awal rute. Simpan energi untuk menghadapi ${climbs.size} sektor tanjakan."
        } else {
            "Jaga kecepatan stabil (Zone 2) dan pertahankan formasi kayuhan efisien."
        }

        val fuelingAdvice = if (climbs.isNotEmpty()) {
            "Pastikan asupan karbohidrat terpenuhi sebelum KM ${climbs.first().startKm.toInt()} agar simpanan glikogen optimal saat menanjak."
        } else {
            "Disiplin minum cairan elektrolit setiap 15-20 menit sekali untuk mencegah dehidrasi."
        }

        return RouteIntelligenceSummary(
            headline = headline,
            criticalSection = criticalSection,
            strategy = strategy,
            fuelingAdvice = fuelingAdvice
        )
    }

    private fun generatePois(climbs: List<ClimbSegment>, totalDistanceKm: Double): List<RoutePoi> {
        val pois = mutableListOf<RoutePoi>()
        pois.add(RoutePoi("Titik Start", 0.0, "Start"))
        climbs.forEach { climb ->
            pois.add(RoutePoi("Puncak ${climb.name}", climb.endKm, "Summit"))
        }
        if (totalDistanceKm > 40.0) {
            pois.add(RoutePoi("Rekomendasi Rest Point", roundToOneDecimal(totalDistanceKm * 0.5), "Rest"))
        }
        pois.add(RoutePoi("Garis Finish", roundToOneDecimal(totalDistanceKm), "Finish"))
        return pois
    }

    private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    private fun roundToOneDecimal(value: Double): Double {
        return (value * 10.0).roundToInt() / 10.0
    }
}
