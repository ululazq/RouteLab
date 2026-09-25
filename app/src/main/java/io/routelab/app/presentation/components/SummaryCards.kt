package io.routelab.app.presentation.components

import androidx.compose.foundation.background
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
import io.routelab.app.domain.model.DifficultyRating
import io.routelab.app.domain.model.RoadCharacteristic
import io.routelab.app.domain.model.RouteAnalysis
import io.routelab.app.domain.model.RouteIntelligenceSummary
import io.routelab.app.presentation.theme.*

@Composable
fun RouteQuickStatsRow(
    route: RouteAnalysis,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox(
            label = "Jarak",
            value = "${route.totalDistanceKm} km",
            subtext = "Total jarak tempuh",
            color = AccentCyan,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            label = "Elevasi",
            value = "+${route.elevationGainM.toInt()} m",
            subtext = "-${route.elevationLossM.toInt()} m turun",
            color = ColorRolling,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            label = "Titik Tertinggi",
            value = "${route.highestPointM.toInt()} m",
            subtext = "Titik terendah ${route.lowestPointM.toInt()} m",
            color = AccentBlueBright,
            modifier = Modifier.weight(1f)
        )
        StatBox(
            label = "Gradien Maks",
            value = "${route.maxGradientPct}%",
            subtext = "Rata-rata ${route.avgGradientPct}%",
            color = ColorClimb,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtext, color = TextMutedDark, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
fun RouteIntelligenceCard(
    summary: RouteIntelligenceSummary,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Route Intelligence Summary",
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .background(AccentBlue.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "TERANALISIS",
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = summary.headline,
            color = AccentBlueBright,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        InsightItem(
            title = "Sektor Kritis",
            content = summary.criticalSection,
            accentColor = ColorClimb
        )

        Spacer(modifier = Modifier.height(8.dp))

        InsightItem(
            title = "Strategi Pacing",
            content = summary.strategy,
            accentColor = ColorRolling
        )

        Spacer(modifier = Modifier.height(8.dp))

        InsightItem(
            title = "Rekomendasi Nutrisi",
            content = summary.fuelingAdvice,
            accentColor = ColorFlat
        )
    }
}

@Composable
private fun InsightItem(
    title: String,
    content: String,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = TextPrimaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun DifficultyCard(
    difficulty: DifficultyRating,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (difficulty.level) {
        "Sangat Menantang" -> ColorExtreme
        "Menantang" -> ColorClimb
        "Moderat" -> ColorRolling
        else -> ColorFlat
    }

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
                text = "Tingkat Kesulitan Objektif",
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${difficulty.level} (${difficulty.score}/100)",
                    color = badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Difficulty Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(SurfaceElevated, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (difficulty.score / 100f).coerceIn(0.1f, 1f))
                    .background(badgeColor, RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = difficulty.reason,
            color = TextSecondaryDark,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
fun RoadCharacteristicsCard(
    roadInfo: RoadCharacteristic,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Karakteristik Jalan & Permukaan",
            color = TextPrimaryDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RoadDetailItem(
                label = "Tipe Jalan",
                value = roadInfo.roadType,
                modifier = Modifier.weight(1f)
            )
            RoadDetailItem(
                label = "Permukaan",
                value = roadInfo.surface,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        RoadDetailItem(
            label = "Aksesibilitas & Lalu Lintas",
            value = roadInfo.accessibility,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoadDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(text = label, color = TextMutedDark, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
