package io.routelab.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.domain.model.ClimbSegment
import io.routelab.app.domain.model.RouteSegment
import io.routelab.app.domain.model.TerrainType
import io.routelab.app.presentation.theme.*

@Composable
fun ClimbListCard(
    climbs: List<ClimbSegment>,
    onSelectClimb: (ClimbSegment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Sektor Tanjakan (${climbs.size})",
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ketuk untuk sorot di peta",
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (climbs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak terdeteksi tanjakan terjal signifikan pada rute ini.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                climbs.forEach { climb ->
                    ClimbItemRow(climb = climb, onClick = { onSelectClimb(climb) })
                }
            }
        }
    }
}

@Composable
private fun ClimbItemRow(
    climb: ClimbSegment,
    onClick: () -> Unit
) {
    val categoryColor = when (climb.category) {
        "Kategori HC" -> ColorExtreme
        "Kategori 1" -> ColorClimb
        "Kategori 2" -> ColorRolling
        "Kategori 3" -> ColorRolling
        else -> AccentBlueBright
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(categoryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = climb.category,
                        color = categoryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${climb.name} (KM ${climb.startKm} - ${climb.endKm})",
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Jarak: ${climb.distanceKm} km | Elevasi: +${climb.elevationGainM.toInt()} m",
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Maks ${climb.maxGradientPct}%",
                color = ColorClimb,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Rata-rata ${climb.avgGradientPct}%",
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun RouteSegmentsCard(
    segments: List<RouteSegment>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Segmentasi Karakteristik Rute",
            color = TextPrimaryDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEach { seg ->
                SegmentItemRow(segment = seg)
            }
        }
    }
}

@Composable
private fun SegmentItemRow(segment: RouteSegment) {
    val (terrainColor, terrainLabel) = when (segment.terrain) {
        TerrainType.CLIMBING -> Pair(ColorClimb, "Tanjakan")
        TerrainType.DESCENDING -> Pair(AccentCyan, "Turunan")
        TerrainType.ROLLING -> Pair(ColorRolling, "Rolling")
        TerrainType.FLAT -> Pair(ColorFlat, "Datar")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Segmen ${segment.segmentNumber}: KM ${segment.startKm} - ${segment.endKm}",
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(terrainColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = terrainLabel,
                    color = terrainColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = segment.description,
            color = TextSecondaryDark,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Saran Efek: ${segment.recommendedEffort}",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
