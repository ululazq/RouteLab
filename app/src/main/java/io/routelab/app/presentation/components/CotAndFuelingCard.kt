package io.routelab.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.domain.model.CotPlan
import io.routelab.app.domain.model.RestFuelingRecommendation
import io.routelab.app.presentation.theme.*
import kotlin.math.roundToInt

@Composable
fun CotPlannerCard(
    cotPlan: CotPlan,
    targetHours: Double,
    onTargetHoursChange: (Double) -> Unit,
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
                text = "Perencanaan Target Waktu (COT)",
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
                    text = "${String.format("%.1f", targetHours)} Jam",
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Atur target durasi keseluruhan untuk menghitung distribusi pace realistis berdasarkan elevasi tiap sektor:",
            color = TextSecondaryDark,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = targetHours.toFloat(),
            onValueChange = { onTargetHoursChange(((it * 10f).roundToInt() / 10f).toDouble()) },
            valueRange = 1.5f..8.0f,
            steps = 25,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = SurfaceElevated
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Cepat (1.5 jam)", color = TextMutedDark, fontSize = 10.sp)
            Text(text = "Rata-rata Target: ${cotPlan.avgRequiredSpeedKmh} km/jam", color = TextPrimaryDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Santai (8.0 jam)", color = TextMutedDark, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Buffer info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Alokasi Stop & Fueling: ${cotPlan.stopsAllowanceMinutes.toInt()} mnt", color = TextSecondaryDark, fontSize = 11.sp)
            Text(text = "Waktu Bergerak Murni: ${(cotPlan.totalMovingTimeMinutes / 60.0 * 10.0).roundToInt() / 10.0} jam", color = AccentBlueBright, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Strategi Pace Masuk Akal per Sektor",
            color = TextPrimaryDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cotPlan.segmentTargets.forEach { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KM ${target.startKm}-${target.endKm} (${target.terrain.displayName})",
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = target.effortStrategy,
                            color = AccentCyan,
                            fontSize = 10.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${target.targetPaceKmh} km/jam",
                            color = ColorRolling,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "~${target.estimatedMinutes.toInt()} menit",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuelingScheduleCard(
    recommendations: List<RestFuelingRecommendation>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Rencana Hidrasi, Nutrisi & Istirahat",
            color = TextPrimaryDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recommendations.forEach { rec ->
                val typeColor = when (rec.type) {
                    "Fueling" -> ColorRolling
                    "Hydration" -> AccentCyan
                    else -> ColorFlat
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "KM ${rec.kmMark}",
                            color = typeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rec.title,
                            color = TextPrimaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rec.advice,
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
