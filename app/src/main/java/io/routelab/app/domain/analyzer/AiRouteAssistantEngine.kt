package io.routelab.app.domain.analyzer

import io.routelab.app.domain.model.RouteAnalysis
import io.routelab.app.domain.model.TerrainType
import kotlin.math.abs

class AiRouteAssistantEngine {

    data class AssistantMessage(
        val isUser: Boolean,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun answerQuestion(question: String, route: RouteAnalysis): String {
        val q = question.lowercase().trim()

        return when {
            q.contains("tanjakan") && (q.contains("berat") || q.contains("terjal") || q.contains("sulit") || q.contains("mana")) -> {
                answerSteepestClimb(route)
            }
            q.contains("hemat") || q.contains("tenaga") || q.contains("energi") -> {
                answerPacingConservation(route)
            }
            q.contains("target") || q.contains("cot") || q.contains("jam") || q.contains("pace") || q.contains("kecepatan") -> {
                answerCotStrategy(route)
            }
            q.contains("fueling") || q.contains("makan") || q.contains("minum") || q.contains("hidrasi") || q.contains("nutrisi") -> {
                answerFuelingPlan(route)
            }
            q.contains("akhir") || q.contains("awal") || q.contains("paruh") || q.contains("banding") -> {
                answerFirstVsSecondHalf(route)
            }
            q.contains("berhenti") || q.contains("istirahat") || q.contains("rest") || q.contains("stop") -> {
                answerStopImpact(route)
            }
            else -> {
                answerGeneralIntelligence(route, question)
            }
        }
    }

    private fun answerSteepestClimb(route: RouteAnalysis): String {
        if (route.climbs.isEmpty()) {
            return "Pada rute ${route.routeName} (${route.totalDistanceKm} km), tidak terdeteksi tanjakan terjal yang signifikan. Total elevasi rute ini hanya +${route.elevationGainM.toInt()} m dengan kontur mayoritas datar."
        }

        val steepest = route.climbs.maxByOrNull { it.maxGradientPct } ?: route.climbs.first()
        val longest = route.climbs.maxByOrNull { it.distanceKm } ?: route.climbs.first()

        val sb = StringBuilder()
        sb.append("Tanjakan terberat pada rute ${route.routeName} adalah ${steepest.name}:\n")
        sb.append("• Lokasi: KM ${steepest.startKm} sampai KM ${steepest.endKm} (panjang ${steepest.distanceKm} km)\n")
        sb.append("• Elevasi didapat: +${steepest.elevationGainM.toInt()} m\n")
        sb.append("• Rata-rata kemiringan: ${steepest.avgGradientPct}%\n")
        sb.append("• Kemiringan maksimum: ${steepest.maxGradientPct}%\n")
        sb.append("• Kategori: ${steepest.category}\n\n")

        if (longest.id != steepest.id) {
            sb.append("Perhatikan juga ${longest.name} sebagai tanjakan terpanjang (${longest.distanceKm} km) di KM ${longest.startKm}-${longest.endKm} dengan elevasi +${longest.elevationGainM.toInt()} m.")
        } else {
            sb.append("Tanjakan ini sekaligus merupakan tanjakan terpanjang di rute ini. Gunakan rasio gear ringan sejak awal tanjakan dan jaga cadence stabil di atas 75 RPM.")
        }
        return sb.toString()
    }

    private fun answerPacingConservation(route: RouteAnalysis): String {
        if (route.climbs.isEmpty()) {
            return "Karena kontur rute datar, titik krusial adalah mempertahankan efisiensi kayuhan konstan di Zone 2 dan memanfaatkan drafting jika bersepeda dalam grup."
        }

        val firstClimb = route.climbs.first()
        val majorClimbsInSecondHalf = route.climbs.filter { it.startKm > route.totalDistanceKm / 2.0 }

        val sb = StringBuilder()
        sb.append("Strategi penghematan tenaga untuk ${route.routeName}:\n\n")
        sb.append("1. Jangan terbawa nafsu di KM 0 - ${firstClimb.startKm.toInt()}. Banyak pesepeda membuang tenaga di sektor datar awal.\n")

        if (majorClimbsInSecondHalf.isNotEmpty()) {
            sb.append("2. Terdapat ${majorClimbsInSecondHalf.size} tanjakan signifikan di paruh kedua rute (setelah KM ${(route.totalDistanceKm / 2).toInt()}). Simpan minimal 40% kapasitas tenaga untuk sektor akhir ini.\n")
        }

        sb.append("3. Pada tanjakan dengan gradien di atas 7%, kurangi target kecepatan dan prioritaskan detak jantung agar tidak melebihi ambang batas laktat (Lactate Threshold).")
        return sb.toString()
    }

    private fun answerCotStrategy(route: RouteAnalysis): String {
        val cot = route.cotPlan
        val sb = StringBuilder()
        sb.append("Analisis strategi target waktu (${cot.targetHours} jam) untuk ${route.routeName} (${route.totalDistanceKm} km):\n\n")
        sb.append("• Rata-rata kecepatan yang dibutuhkan: ${cot.avgRequiredSpeedKmh} km/jam\n")
        sb.append("• Alokasi waktu berhenti / fueling: ${cot.stopsAllowanceMinutes.toInt()} menit\n")
        sb.append("• Total waktu bergerak murni: ${(cot.totalMovingTimeMinutes / 60.0 * 10.0).toInt() / 10.0} jam\n\n")
        sb.append("Target pace per sektor berdasarkan elevasi:\n")

        cot.segmentTargets.forEach { seg ->
            sb.append("• KM ${seg.startKm}-${seg.endKm} (${seg.terrain.displayName}): target ${seg.targetPaceKmh} km/jam (~${seg.estimatedMinutes.toInt()} mnt) : ${seg.effortStrategy}\n")
        }
        return sb.toString()
    }

    private fun answerFuelingPlan(route: RouteAnalysis): String {
        val recs = route.restFueling
        if (recs.isEmpty()) {
            return "Rekomendasi standar: Konsumsi 30-60g karbohidrat per jam dan 500ml cairan elektrolit."
        }

        val sb = StringBuilder()
        sb.append("Jadwal nutrisi dan hidrasi yang direkomendasikan untuk ${route.routeName}:\n\n")
        recs.forEach { rec ->
            sb.append("• KM ${rec.kmMark} [${rec.type}]: ${rec.title}\n  ${rec.advice}\n\n")
        }
        sb.append("Tips: Jangan menunggu rasa haus atau lapar muncul. Lakukan pengisian bahan bakar sebelum memasuki sektor tanjakan terberat.")
        return sb.toString()
    }

    private fun answerFirstVsSecondHalf(route: RouteAnalysis): String {
        val midKm = route.totalDistanceKm / 2.0
        val midMeters = midKm * 1000.0

        val firstHalfPoints = route.points.filter { it.distanceMeters <= midMeters }
        val secondHalfPoints = route.points.filter { it.distanceMeters > midMeters }

        var gain1 = 0.0
        for (i in 1 until firstHalfPoints.size) {
            val d = firstHalfPoints[i].elevation - firstHalfPoints[i - 1].elevation
            if (d > 0) gain1 += d
        }

        var gain2 = 0.0
        for (i in 1 until secondHalfPoints.size) {
            val d = secondHalfPoints[i].elevation - secondHalfPoints[i - 1].elevation
            if (d > 0) gain2 += d
        }

        val climbs1 = route.climbs.count { it.startKm < midKm }
        val climbs2 = route.climbs.count { it.startKm >= midKm }

        val sb = StringBuilder()
        sb.append("Perbandingan Paruh Pertama vs Paruh Kedua (${route.routeName}):\n\n")
        sb.append("• Paruh Pertama (KM 0 - ${midKm.toInt()}): Elevasi +${gain1.toInt()} m, ${climbs1} tanjakan.\n")
        sb.append("• Paruh Kedua (KM ${midKm.toInt()} - ${route.totalDistanceKm.toInt()}): Elevasi +${gain2.toInt()} m, ${climbs2} tanjakan.\n\n")

        if (gain2 > gain1 * 1.2) {
            sb.append("Kesimpulan: Paruh kedua JAUH LEBIH BERAT (+${gain2.toInt()} m vs +${gain1.toInt()} m). Waspadai kelelahan kumulatif dan hemat cadangan energi di awal perjalanan.")
        } else if (gain1 > gain2 * 1.2) {
            sb.append("Kesimpulan: Rintangan terberat berada di paruh pertama. Setelah KM ${midKm.toInt()}, profil rute cenderung menurun atau lebih bersahabat.")
        } else {
            sb.append("Kesimpulan: Beban elevasi tersebar merata sepanjang rute. Pertahankan ritme kayuhan konsisten dari start hingga finish.")
        }
        return sb.toString()
    }

    private fun answerStopImpact(route: RouteAnalysis): String {
        val cot = route.cotPlan
        val additionalStopMinutes = 15.0
        val newStopMinutes = cot.stopsAllowanceMinutes + additionalStopMinutes
        val totalTargetMinutes = cot.targetHours * 60.0
        val remainingMovingMinutes = totalTargetMinutes - newStopMinutes

        val sb = StringBuilder()
        sb.append("Simulasi Dampak Berhenti Tambahan 15 Menit:\n\n")
        if (remainingMovingMinutes <= 0) {
            sb.append("Perhatian: Total waktu berhenti melebihi target waktu keseluruhan. Anda perlu menambah target COT Anda.")
        } else {
            val newRequiredSpeed = (route.totalDistanceKm / (remainingMovingMinutes / 60.0) * 10.0).toInt() / 10.0
            val speedDelta = newRequiredSpeed - cot.avgRequiredSpeedKmh
            sb.append("• Total alokasi istirahat bertambah menjadi ${newStopMinutes.toInt()} menit.\n")
            sb.append("• Waktu bergerak murni berkurang menjadi ${(remainingMovingMinutes / 60.0 * 10.0).toInt() / 10.0} jam.\n")
            sb.append("• Kecepatan rata-rata bergerak harus NAIK sebesar +${(speedDelta * 10.0).toInt() / 10.0} km/jam (dari ${cot.avgRequiredSpeedKmh} km/jam menjadi ${newRequiredSpeed} km/jam) untuk tetap mencapai garis akhir tepat waktu.")
        }
        return sb.toString()
    }

    private fun answerGeneralIntelligence(route: RouteAnalysis, query: String): String {
        return "Ringkasan Rute ${route.routeName}:\n" +
                "• Jarak: ${route.totalDistanceKm} km\n" +
                "• Elevasi: +${route.elevationGainM.toInt()} m / -${route.elevationLossM.toInt()} m\n" +
                "• Titik Tertinggi: ${route.highestPointM.toInt()} m\n" +
                "• Sektor Tanjakan: ${route.climbs.size} tanjakan terdeteksi\n" +
                "• Status Kesulitan: ${route.difficulty.level} (Skor ${route.difficulty.score}/100)\n\n" +
                "Saran: ${route.summary.strategy}\n" +
                "Sektor Kritis: ${route.summary.criticalSection}"
    }
}
