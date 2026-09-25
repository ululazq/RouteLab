package io.routelab.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.domain.model.ClimbSegment
import io.routelab.app.domain.model.RoutePoint
import io.routelab.app.presentation.theme.*
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun InteractiveElevationProfile(
    points: List<RoutePoint>,
    climbs: List<ClimbSegment>,
    selectedDistanceMeters: Double,
    onScrub: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val totalDistanceMeters = points.last().distanceMeters
    val minEle = points.minOf { it.elevation }
    val maxEle = points.maxOf { it.elevation }
    val eleRange = max(20.0, maxEle - minEle)

    // Current scrubbed point details
    val currentPoint = points.minByOrNull { kotlin.math.abs(it.distanceMeters - selectedDistanceMeters) } ?: points.first()

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
                text = "Profil Elevasi Interaktif",
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Geser grafik untuk inspeksi",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrubber Information Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val kmText = String.format("%.1f", currentPoint.distanceMeters / 1000.0)
            val eleText = "${currentPoint.elevation.roundToInt()} m"
            val gradText = String.format("%.1f%%", currentPoint.gradientPct)

            Text(text = "KM: $kmText", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Elevasi: $eleText", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Gradien: $gradText",
                color = when {
                    currentPoint.gradientPct > 7.0 -> ColorClimb
                    currentPoint.gradientPct > 3.0 -> ColorRolling
                    else -> ColorFlat
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(totalDistanceMeters) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            onScrub(ratio * totalDistanceMeters)
                        }
                    }
                    .pointerInput(totalDistanceMeters) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onScrub(ratio * totalDistanceMeters)
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingBottom = 24f
                val paddingTop = 16f
                val usableHeight = canvasHeight - paddingBottom - paddingTop

                fun getX(distanceMeters: Double): Float {
                    return if (totalDistanceMeters > 0) {
                        ((distanceMeters / totalDistanceMeters) * canvasWidth).toFloat()
                    } else 0f
                }

                fun getY(elevation: Double): Float {
                    val normalized = (elevation - minEle) / eleRange
                    return (canvasHeight - paddingBottom - (normalized * usableHeight)).toFloat()
                }

                val gridSteps = 3
                for (s in 0..gridSteps) {
                    val gridY = canvasHeight - paddingBottom - (s.toFloat() / gridSteps * usableHeight)
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(0f, gridY),
                        end = Offset(canvasWidth, gridY),
                        strokeWidth = 1f
                    )
                }

                climbs.forEach { climb ->
                    val startX = getX(climb.startKm * 1000.0)
                    val endX = getX(climb.endKm * 1000.0)
                    val climbWidth = max(endX - startX, 4f)
                    drawRect(
                        color = ColorClimb.copy(alpha = 0.12f),
                        topLeft = Offset(startX, paddingTop),
                        size = Size(climbWidth, usableHeight)
                    )
                }

                val path = Path()
                val fillPath = Path()

                fillPath.moveTo(0f, canvasHeight - paddingBottom)

                points.forEachIndexed { index, pt ->
                    val x = getX(pt.distanceMeters)
                    val y = getY(pt.elevation)
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(canvasWidth, canvasHeight - paddingBottom)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.45f),
                            AccentBlue.copy(alpha = 0.05f)
                        ),
                        startY = paddingTop,
                        endY = canvasHeight - paddingBottom
                    )
                )

                drawPath(
                    path = path,
                    color = AccentBlueBright,
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val scrubX = getX(selectedDistanceMeters)
                val scrubY = getY(currentPoint.elevation)

                // Vertical guideline
                drawLine(
                    color = AccentCyan.copy(alpha = 0.7f),
                    start = Offset(scrubX, paddingTop),
                    end = Offset(scrubX, canvasHeight - paddingBottom),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                // Outer halo
                drawCircle(
                    color = AccentCyan.copy(alpha = 0.35f),
                    radius = 9f,
                    center = Offset(scrubX, scrubY)
                )
                // Center point
                drawCircle(
                    color = AccentCyan,
                    radius = 5f,
                    center = Offset(scrubX, scrubY)
                )
            }
        }
    }
}
