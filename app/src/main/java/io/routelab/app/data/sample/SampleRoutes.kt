package io.routelab.app.data.sample

import io.routelab.app.domain.model.GeoPoint
import io.routelab.app.domain.model.RoadCharacteristic
import kotlin.math.sin

object SampleRoutes {

    data class SampleRouteDef(
        val id: String,
        val name: String,
        val location: String,
        val description: String,
        val roadInfo: RoadCharacteristic,
        val defaultCotHours: Double,
        val points: List<GeoPoint>
    )

    fun getAvailableRoutes(): List<SampleRouteDef> {
        return listOf(
            sentulKm0Challenge(),
            puncakPassEpic(),
            pikCoastalPaceline()
        )
    }

    private fun sentulKm0Challenge(): SampleRouteDef {
        val points = mutableListOf<GeoPoint>()
        val startLat = -6.5583
        val startLon = 106.8741
        val numPoints = 120

        for (i in 0 until numPoints) {
            val progress = i.toDouble() / (numPoints - 1)
            val lat = startLat - (progress * 0.12) + (sin(progress * Math.PI * 4) * 0.015)
            val lon = startLon + (progress * 0.08) + (sin(progress * Math.PI * 2) * 0.01)

            // Elevation profile: start 180m -> rolling to 240m -> climb Rainbow Hills (420m) -> dip to 310m -> KM 0 summit (680m) -> descent
            val ele = when {
                progress < 0.25 -> 180.0 + (progress / 0.25) * 60.0 + (sin(progress * 40) * 10.0)
                progress < 0.45 -> 240.0 + ((progress - 0.25) / 0.20) * 180.0 + (sin(progress * 30) * 8.0)
                progress < 0.55 -> 420.0 - ((progress - 0.45) / 0.10) * 110.0
                progress < 0.80 -> 310.0 + ((progress - 0.55) / 0.25) * 370.0 + (sin(progress * 20) * 5.0)
                else -> 680.0 - ((progress - 0.80) / 0.20) * 480.0
            }

            points.add(GeoPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        return SampleRouteDef(
            id = "sentul-km0",
            name = "Sentul Rainbow Hills : KM 0",
            location = "Bogor, Jawa Barat",
            description = "Rute ikonik pesepeda Jabodetabek. Menghadirkan kombinasi jalan bergelombang dan dua tanjakan utama menuju KM 0 Bojong Koneng.",
            roadInfo = RoadCharacteristic(
                roadType = "Tersier & Jalan Pedesaan",
                surface = "Mayoritas Aspal Halus, Beton Sebagian",
                accessibility = "Akses terbuka, lalu lintas sedang pada akhir pekan"
            ),
            defaultCotHours = 3.2,
            points = points
        )
    }

    private fun puncakPassEpic(): SampleRouteDef {
        val points = mutableListOf<GeoPoint>()
        val startLat = -6.6000
        val startLon = 106.8000
        val numPoints = 160

        for (i in 0 until numPoints) {
            val progress = i.toDouble() / (numPoints - 1)
            val lat = startLat - (progress * 0.22) + (sin(progress * Math.PI * 3) * 0.02)
            val lon = startLon + (progress * 0.19) + (sin(progress * Math.PI * 2) * 0.015)

            // Puncak pass profile: start 250m -> gradual climb past Ciawi (450m) -> steep Cisarua (900m) -> Puncak Pass Summit (1,450m) -> rolling descent
            val ele = when {
                progress < 0.20 -> 250.0 + (progress / 0.20) * 200.0
                progress < 0.45 -> 450.0 + ((progress - 0.20) / 0.25) * 450.0
                progress < 0.70 -> 900.0 + ((progress - 0.45) / 0.25) * 550.0
                progress < 0.85 -> 1450.0 - ((progress - 0.70) / 0.15) * 350.0
                else -> 1100.0 - ((progress - 0.85) / 0.15) * 400.0
            }

            points.add(GeoPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        return SampleRouteDef(
            id = "puncak-pass",
            name = "Puncak Pass Epic Challenge",
            location = "Bogor - Cianjur, Jawa Barat",
            description = "Ujian elevasi legendaris dengan akumulasi tanjakan panjang bergradien konstan hingga mencapai Puncak Pass di ketinggian 1.450 mdpl.",
            roadInfo = RoadCharacteristic(
                roadType = "Jalan Nasional / Provinsi",
                surface = "Aspal Mulus Beraspal Panas",
                accessibility = "Jalur utama antarkota, ramai kendaraan di siang hari"
            ),
            defaultCotHours = 5.5,
            points = points
        )
    }

    private fun pikCoastalPaceline(): SampleRouteDef {
        val points = mutableListOf<GeoPoint>()
        val startLat = -6.1100
        val startLon = 106.7400
        val numPoints = 100

        for (i in 0 until numPoints) {
            val progress = i.toDouble() / (numPoints - 1)
            val lat = startLat + (sin(progress * Math.PI * 2) * 0.04)
            val lon = startLon + (progress * 0.06) + (sin(progress * Math.PI * 4) * 0.01)

            // Flat coastal route with small bridge elevation humps (4m - 18m)
            val ele = 6.0 + (sin(progress * Math.PI * 8) * 6.0).coerceAtLeast(0.0)

            points.add(GeoPoint(latitude = lat, longitude = lon, elevation = ele))
        }

        return SampleRouteDef(
            id = "pik-coastal",
            name = "PIK 2 Coastal Fast Paceline",
            location = "Jakarta Utara & Tangerang",
            description = "Rute datar tanpa hambatan tanjakan berarti, ideal untuk latihan endurance kelompok, time trial, dan menjaga cadence stabil melawan terpaan angin pesisir.",
            roadInfo = RoadCharacteristic(
                roadType = "Arterial Kawasan Khusus & Jalur Sepeda",
                surface = "Aspal Mulus Khusus Sepeda & Beton Rapi",
                accessibility = "Sangat ramah pesepeda, lebar jalan memadai"
            ),
            defaultCotHours = 2.4,
            points = points
        )
    }
}
