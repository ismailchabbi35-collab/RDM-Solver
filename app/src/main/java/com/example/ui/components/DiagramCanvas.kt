package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.util.Locale
import kotlin.math.*

enum class DiagramType(val titleFr: String, val titleAr: String, val titleEn: String, val unit: String) {
    SHEAR("Effort Tranchant T(x)", "جهد القص T(x)", "Shear Force T(x)", "kN"),
    MOMENT("Moment Fléchissant M(x)", "عزم الانحناء M(x)", "Bending Moment M(x)", "kN·m"),
    NORMAL("Effort Normal N(x)", "الجهد الناظمي N(x)", "Normal Force N(x)", "kN"),
    DEFLECTION("Flèche / Déformée f(x)", "انحناء السهم f(x)", "Deflection f(x)", "mm"),
    ROTATION("Rotation de Section θ(x)", "دوران المقطع θ(x)", "Section Rotation θ(x)", "mrad")
}

@Composable
fun DiagramCanvas(
    type: DiagramType,
    points: List<DiagramPoint>,
    remarkablePoints: List<RemarkablePoint>,
    beamLength: Double,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    var cursorXFraction by remember { mutableStateOf<Float?>(null) }

    val maxVal = points.maxOfOrNull { it.value } ?: 0.0
    val minVal = points.minOfOrNull { it.value } ?: 0.0
    val absMax = max(abs(maxVal), abs(minVal)).coerceAtLeast(1.0)

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Diagram Header & Extremum Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = type.titleFr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = when (type) {
                        DiagramType.MOMENT -> "Convention RDM : M > 0 = Fibres tendues en bas"
                        DiagramType.SHEAR -> "Relation : dM/dx = T(x)"
                        DiagramType.NORMAL -> "N > 0 : Traction (+) | N < 0 : Compression (-)"
                        DiagramType.DEFLECTION -> "Déformée élastique : f(x) = -v(x) en mm"
                        DiagramType.ROTATION -> "Rotation des sections : θ(x) = dv/dx en mrad"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceColor
                )
            }

            Surface(
                color = primaryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Extr: %.2f %s".format(Locale.US, if (abs(maxVal) >= abs(minVal)) maxVal else minVal, type.unit),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Canvas with interactive touch scrubber
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                cursorXFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDrag = { change, _ ->
                                cursorXFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = { cursorXFraction = null },
                            onDragCancel = { cursorXFraction = null }
                        )
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val paddingX = 30.dp.toPx()
                val drawWidth = canvasWidth - 2 * paddingX
                val zeroY = canvasHeight / 2.0f

                // Scale factor: values mapped to +/- (canvasHeight / 2 - 16dp)
                val maxDrawHeight = (canvasHeight / 2.0f) - 20.dp.toPx()
                val valScale = (maxDrawHeight / absMax).toFloat()

                fun xToPx(x: Double): Float = (paddingX + (x / beamLength) * drawWidth).toFloat()

                // In RDM: For Moment M(x), traditionally M > 0 (fibres tendues en bas) is often drawn
                // downwards in civil engineering plans, or upwards in standard Cartesian.
                // We draw positive values according to civil convention with zero line in center!
                fun valToPx(v: Double): Float = (zeroY - v * valScale).toFloat()

                // Draw Zero Baseline
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(paddingX, zeroY),
                    end = Offset(paddingX + drawWidth, zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Fill polygon area
                if (points.isNotEmpty()) {
                    val fillPath = Path().apply {
                        moveTo(xToPx(points.first().x), zeroY)
                        for (p in points) {
                            lineTo(xToPx(p.x), valToPx(p.value))
                        }
                        lineTo(xToPx(points.last().x), zeroY)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.05f)),
                            startY = 0f,
                            endY = canvasHeight
                        )
                    )

                    // Stroke line
                    val strokePath = Path().apply {
                        moveTo(xToPx(points.first().x), valToPx(points.first().value))
                        for (p in points.drop(1)) {
                            lineTo(xToPx(p.x), valToPx(p.value))
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = primaryColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Draw Remarkable Points (dots at discontinuities / extrema)
                for (rp in remarkablePoints) {
                    val px = xToPx(rp.x)
                    val v = when (type) {
                        DiagramType.SHEAR -> rp.valueT
                        DiagramType.MOMENT -> rp.valueM
                        DiagramType.NORMAL -> rp.valueN
                        DiagramType.DEFLECTION -> points.minByOrNull { abs(it.x - rp.x) }?.value ?: 0.0
                        DiagramType.ROTATION -> points.minByOrNull { abs(it.x - rp.x) }?.value ?: 0.0
                    }
                    val py = valToPx(v)

                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Draw Cursor Line if user is scrubbing
                cursorXFraction?.let { frac ->
                    val curX = (paddingX + frac * drawWidth)
                    val curM = frac * beamLength
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(curX, 0f),
                        end = Offset(curX, canvasHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }
            }
        }

        // Inspection footer info (or scrub values)
        cursorXFraction?.let { frac ->
            val inspectX = frac * beamLength
            val nearestPoint = points.minByOrNull { abs(it.x - inspectX) }
            Text(
                text = "x = %.2f m ➔ %s = %.2f %s".format(
                    Locale.US, inspectX, when (type) {
                        DiagramType.SHEAR -> "T(x)"
                        DiagramType.MOMENT -> "M(x)"
                        DiagramType.NORMAL -> "N(x)"
                        DiagramType.DEFLECTION -> "f(x)"
                        DiagramType.ROTATION -> "θ(x)"
                    }, nearestPoint?.value ?: 0.0, type.unit
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
