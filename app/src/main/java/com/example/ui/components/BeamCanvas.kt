package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun BeamCanvas(
    structure: BeamStructure,
    reactions: List<SupportReaction>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val paddingX = 40.dp.toPx()
            val beamY = canvasHeight * 0.52f
            val beamLengthPx = canvasWidth - 2 * paddingX
            val beamLengthM = max(0.1, structure.length)
            val scale = beamLengthPx / beamLengthM

            fun xToPx(x: Double): Float = (paddingX + x * scale).toFloat()

            // 1. Draw Beam Main Bar
            val beamThickness = 12.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
                ),
                topLeft = Offset(paddingX, beamY - beamThickness / 2),
                size = Size(beamLengthPx, beamThickness),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // 2. Draw Supports
            structure.supports.forEach { support ->
                val supX = xToPx(support.position)
                drawSupport(support, supX, beamY + beamThickness / 2)
            }

            // 3. Draw Loads
            structure.loads.forEach { load ->
                when (load.type) {
                    LoadType.POINT_FORCE -> {
                        val px = xToPx(load.positionStart)
                        drawPointForce(px, beamY - beamThickness / 2, load)
                    }
                    LoadType.DISTRIBUTED_UNIFORM -> {
                        val x1 = xToPx(load.positionStart)
                        val x2 = xToPx(load.positionEnd)
                        drawDistributedUniform(x1, x2, beamY - beamThickness / 2, load)
                    }
                    LoadType.DISTRIBUTED_TRIANGULAR -> {
                        val x1 = xToPx(load.positionStart)
                        val x2 = xToPx(load.positionEnd)
                        drawDistributedTriangular(x1, x2, beamY - beamThickness / 2, load)
                    }
                    LoadType.MOMENT -> {
                        val px = xToPx(load.positionStart)
                        drawMoment(px, beamY, load)
                    }
                }
            }

            // 4. Draw Support Reactions (Upward green arrows)
            reactions.forEach { reaction ->
                val rx = xToPx(reaction.position)
                if (abs(reaction.ry) > 0.01) {
                    drawReactionArrow(rx, beamY + beamThickness / 2 + 32.dp.toPx(), reaction)
                }
            }

            // 5. Draw Dimension Line along bottom
            val dimY = canvasHeight - 16.dp.toPx()
            drawLine(
                color = DimensionLineColor,
                start = Offset(paddingX, dimY),
                end = Offset(paddingX + beamLengthPx, dimY),
                strokeWidth = 1.5.dp.toPx()
            )
            // Ticks at ends
            drawLine(
                color = DimensionLineColor,
                start = Offset(paddingX, dimY - 4.dp.toPx()),
                end = Offset(paddingX, dimY + 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = DimensionLineColor,
                start = Offset(paddingX + beamLengthPx, dimY - 4.dp.toPx()),
                end = Offset(paddingX + beamLengthPx, dimY + 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawSupport(support: Support, x: Float, bottomOfBeamY: Float) {
    val triangleHeight = 22.dp.toPx()
    val triangleWidth = 22.dp.toPx()

    when (support.type) {
        SupportType.PINNED -> {
            // Triangle with fixed hatched ground
            val path = Path().apply {
                moveTo(x, bottomOfBeamY)
                lineTo(x - triangleWidth / 2, bottomOfBeamY + triangleHeight)
                lineTo(x + triangleWidth / 2, bottomOfBeamY + triangleHeight)
                close()
            }
            drawPath(path, color = Color(0xFF3B82F6))
            drawPath(path, color = Color(0xFF1E3A8A), style = Stroke(width = 2.dp.toPx()))

            // Ground line
            val groundY = bottomOfBeamY + triangleHeight
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x - triangleWidth * 0.7f, groundY),
                end = Offset(x + triangleWidth * 0.7f, groundY),
                strokeWidth = 2.dp.toPx()
            )
            // Hatching
            for (i in -2..2) {
                val hx = x + i * 5.dp.toPx()
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(hx, groundY),
                    end = Offset(hx - 4.dp.toPx(), groundY + 5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        SupportType.ROLLER -> {
            // Triangle + roller wheels
            val path = Path().apply {
                moveTo(x, bottomOfBeamY)
                lineTo(x - triangleWidth / 2, bottomOfBeamY + triangleHeight - 6.dp.toPx())
                lineTo(x + triangleWidth / 2, bottomOfBeamY + triangleHeight - 6.dp.toPx())
                close()
            }
            drawPath(path, color = Color(0xFF0EA5E9))
            drawPath(path, color = Color(0xFF0369A1), style = Stroke(width = 2.dp.toPx()))

            // Two roller wheels
            val wheelY = bottomOfBeamY + triangleHeight - 3.dp.toPx()
            val r = 2.5.dp.toPx()
            drawCircle(color = Color(0xFF0284C7), radius = r, center = Offset(x - 5.dp.toPx(), wheelY))
            drawCircle(color = Color(0xFF0284C7), radius = r, center = Offset(x + 5.dp.toPx(), wheelY))

            // Ground line
            val groundY = bottomOfBeamY + triangleHeight
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x - triangleWidth * 0.7f, groundY),
                end = Offset(x + triangleWidth * 0.7f, groundY),
                strokeWidth = 2.dp.toPx()
            )
        }
        SupportType.FIXED -> {
            // Fixed wall with diagonal hatching
            val wallHeight = 44.dp.toPx()
            val wallYTop = bottomOfBeamY - wallHeight / 2
            val wallYBottom = bottomOfBeamY + wallHeight / 2
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x, wallYTop),
                end = Offset(x, wallYBottom),
                strokeWidth = 4.dp.toPx()
            )
            // Hatching lines
            for (y in wallYTop.toInt()..wallYBottom.toInt() step 8) {
                val dir = if (x <= 60.dp.toPx()) -1 else 1
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(x, y.toFloat()),
                    end = Offset(x + dir * 8.dp.toPx(), y.toFloat() + 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    }
}

private fun DrawScope.drawPointForce(x: Float, topOfBeamY: Float, load: Load) {
    val arrowLen = 38.dp.toPx()
    val startY = topOfBeamY - arrowLen
    val arrowColor = Color(0xFFDC2626) // Red load arrow

    // Arrow shaft
    drawLine(
        color = arrowColor,
        start = Offset(x, startY),
        end = Offset(x, topOfBeamY),
        strokeWidth = 2.5.dp.toPx()
    )
    // Arrowhead
    val headSize = 7.dp.toPx()
    val headPath = Path().apply {
        moveTo(x, topOfBeamY)
        lineTo(x - headSize, topOfBeamY - headSize * 1.5f)
        lineTo(x + headSize, topOfBeamY - headSize * 1.5f)
        close()
    }
    drawPath(headPath, color = arrowColor)
}

private fun DrawScope.drawDistributedUniform(x1: Float, x2: Float, topOfBeamY: Float, load: Load) {
    val h = 26.dp.toPx()
    val loadTopY = topOfBeamY - h
    val loadColor = Color(0xFFEA580C)

    // Shaded box
    drawRect(
        color = loadColor.copy(alpha = 0.15f),
        topLeft = Offset(x1, loadTopY),
        size = Size(x2 - x1, h)
    )
    // Top boundary line
    drawLine(
        color = loadColor,
        start = Offset(x1, loadTopY),
        end = Offset(x2, loadTopY),
        strokeWidth = 2.dp.toPx()
    )

    // Downward arrows
    val stepPx = 16.dp.toPx()
    var cx = x1 + 4.dp.toPx()
    while (cx <= x2 - 4.dp.toPx()) {
        drawLine(
            color = loadColor,
            start = Offset(cx, loadTopY),
            end = Offset(cx, topOfBeamY),
            strokeWidth = 1.5.dp.toPx()
        )
        val headPath = Path().apply {
            moveTo(cx, topOfBeamY)
            lineTo(cx - 3.dp.toPx(), topOfBeamY - 5.dp.toPx())
            lineTo(cx + 3.dp.toPx(), topOfBeamY - 5.dp.toPx())
            close()
        }
        drawPath(headPath, color = loadColor)
        cx += stepPx
    }
}

private fun DrawScope.drawDistributedTriangular(x1: Float, x2: Float, topOfBeamY: Float, load: Load) {
    val maxH = 30.dp.toPx()
    val loadColor = Color(0xFFD97706)

    val path = Path().apply {
        moveTo(x1, topOfBeamY)
        lineTo(x2, topOfBeamY - maxH)
        lineTo(x2, topOfBeamY)
        close()
    }
    drawPath(path, color = loadColor.copy(alpha = 0.2f))
    drawLine(
        color = loadColor,
        start = Offset(x1, topOfBeamY),
        end = Offset(x2, topOfBeamY - maxH),
        strokeWidth = 2.dp.toPx()
    )

    // Distributed small arrows
    val stepPx = 18.dp.toPx()
    var cx = x1 + stepPx
    while (cx < x2) {
        val frac = (cx - x1) / (x2 - x1)
        val arrH = maxH * frac
        drawLine(
            color = loadColor,
            start = Offset(cx, topOfBeamY - arrH),
            end = Offset(cx, topOfBeamY),
            strokeWidth = 1.5.dp.toPx()
        )
        val headPath = Path().apply {
            moveTo(cx, topOfBeamY)
            lineTo(cx - 3.dp.toPx(), topOfBeamY - 5.dp.toPx())
            lineTo(cx + 3.dp.toPx(), topOfBeamY - 5.dp.toPx())
            close()
        }
        drawPath(headPath, color = loadColor)
        cx += stepPx
    }
}

private fun DrawScope.drawMoment(x: Float, beamCenterY: Float, load: Load) {
    val r = 16.dp.toPx()
    val momentColor = Color(0xFF7C3AED)
    drawArc(
        color = momentColor,
        startAngle = 30f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(x - r, beamCenterY - r),
        size = Size(2 * r, 2 * r),
        style = Stroke(width = 2.5.dp.toPx())
    )
}

private fun DrawScope.drawReactionArrow(x: Float, startY: Float, reaction: SupportReaction) {
    val arrowLen = 28.dp.toPx()
    val endY = startY - arrowLen
    val arrowColor = ReactionGreen

    drawLine(
        color = arrowColor,
        start = Offset(x, startY),
        end = Offset(x, endY),
        strokeWidth = 2.5.dp.toPx()
    )
    val headPath = Path().apply {
        moveTo(x, endY)
        lineTo(x - 5.dp.toPx(), endY + 7.dp.toPx())
        lineTo(x + 5.dp.toPx(), endY + 7.dp.toPx())
        close()
    }
    drawPath(headPath, color = arrowColor)
}
