package io.routelab.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.routelab.app.domain.model.ClimbSegment
import io.routelab.app.domain.model.RoutePoi
import io.routelab.app.domain.model.RoutePoint
import io.routelab.app.presentation.theme.*
import kotlin.math.*

@Composable
fun InteractiveRouteMap(
    points: List<RoutePoint>,
    climbs: List<ClimbSegment>,
    pois: List<RoutePoi>,
    selectedDistanceMeters: Double,
    onSelectDistance: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    val avgLatRad = Math.toRadians((minLat + maxLat) / 2.0)
    val lonCorrection = cos(avgLatRad).coerceAtLeast(0.1)

    val latSpan = max(0.002, maxLat - minLat)
    val lonSpan = max(0.002, (maxLon - minLon) * lonCorrection)

    val currentPoint = points.minByOrNull { abs(it.distanceMeters - selectedDistanceMeters) } ?: points.first()

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
                text = "Peta Lintasan Rute",
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${points.size} titik GPS",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            // Find nearest route point to tap
                            var bestDistance = Double.MAX_VALUE
                            var bestMeters = 0.0

                            val padding = 40f
                            val mapWidth = size.width - (padding * 2)
                            val mapHeight = size.height - (padding * 2)
                            val scale = min(mapWidth / lonSpan.toFloat(), mapHeight / latSpan.toFloat())

                            val centerLon = (minLon + maxLon) / 2.0
                            val centerLat = (minLat + maxLat) / 2.0
                            val canvasCenterX = size.width / 2f
                            val canvasCenterY = size.height / 2f

                            points.forEach { pt ->
                                val x = canvasCenterX + ((pt.longitude - centerLon) * lonCorrection * scale).toFloat()
                                val y = canvasCenterY - ((pt.latitude - centerLat) * scale).toFloat()
                                val dx = tapOffset.x - x
                                val dy = tapOffset.y - y
                                val d2 = (dx * dx + dy * dy).toDouble()
                                if (d2 < bestDistance) {
                                    bestDistance = d2
                                    bestMeters = pt.distanceMeters
                                }
                            }
                            onSelectDistance(bestMeters)
                        }
                    }
            ) {
                val padding = 40f
                val mapWidth = size.width - (padding * 2)
                val mapHeight = size.height - (padding * 2)
                val scale = min(mapWidth / lonSpan.toFloat(), mapHeight / latSpan.toFloat())

                val centerLon = (minLon + maxLon) / 2.0
                val centerLat = (minLat + maxLat) / 2.0
                val canvasCenterX = size.width / 2f
                val canvasCenterY = size.height / 2f

                fun mapX(lon: Double): Float {
                    return canvasCenterX + ((lon - centerLon) * lonCorrection * scale).toFloat()
                }

                fun mapY(lat: Double): Float {
                    return canvasCenterY - ((lat - centerLat) * scale).toFloat()
                }

                // 1. Subtle grid lines
                val gridLines = 4
                for (g in 1..gridLines) {
                    val gy = (size.height / (gridLines + 1)) * g
                    drawLine(
                        color = Color(0x10FFFFFF),
                        start = Offset(0f, gy),
                        end = Offset(size.width, gy),
                        strokeWidth = 1f
                    )
                    val gx = (size.width / (gridLines + 1)) * g
                    drawLine(
                        color = Color(0x10FFFFFF),
                        start = Offset(gx, 0f),
                        end = Offset(gx, size.height),
                        strokeWidth = 1f
                    )
                }

                for (i in 1 until points.size) {
                    val p1 = points[i - 1]
                    val p2 = points[i]
                    val x1 = mapX(p1.longitude)
                    val y1 = mapY(p1.latitude)
                    val x2 = mapX(p2.longitude)
                    val y2 = mapY(p2.latitude)

                    val segColor = when {
                        p2.gradientPct > 7.0 -> ColorClimb
                        p2.gradientPct > 3.0 -> ColorRolling
                        p2.gradientPct < -3.0 -> AccentCyan
                        else -> AccentBlueBright
                    }

                    drawLine(
                        color = segColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                climbs.forEach { climb ->
                    val summitPt = points.minByOrNull { abs((it.distanceMeters / 1000.0) - climb.endKm) }
                    if (summitPt != null) {
                        val sx = mapX(summitPt.longitude)
                        val sy = mapY(summitPt.latitude)
                        drawCircle(
                            color = ColorClimb,
                            radius = 6f,
                            center = Offset(sx, sy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(sx, sy)
                        )
                    }
                }

                val startPt = points.first()
                val startX = mapX(startPt.longitude)
                val startY = mapY(startPt.latitude)
                drawCircle(
                    color = ColorFlat,
                    radius = 8f,
                    center = Offset(startX, startY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(startX, startY)
                )

                val finishPt = points.last()
                val finishX = mapX(finishPt.longitude)
                val finishY = mapY(finishPt.latitude)
                drawCircle(
                    color = ColorClimb,
                    radius = 8f,
                    center = Offset(finishX, finishY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(finishX, finishY)
                )

                val currX = mapX(currentPoint.longitude)
                val currY = mapY(currentPoint.latitude)

                drawCircle(
                    color = AccentCyan.copy(alpha = 0.3f),
                    radius = 16f,
                    center = Offset(currX, currY)
                )
                drawCircle(
                    color = AccentCyan,
                    radius = 7f,
                    center = Offset(currX, currY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(currX, currY)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = ColorFlat, label = "Start")
            LegendItem(color = AccentBlueBright, label = "Datar/Normal")
            LegendItem(color = ColorRolling, label = "Rolling (>3%)")
            LegendItem(color = ColorClimb, label = "Tanjakan (>7%)")
            LegendItem(color = AccentCyan, label = "Posisi Terpilih")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp
        )
    }
}
