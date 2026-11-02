package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun TrussCanvas(
    structure: TrussStructure,
    solution: TrussSolution,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    val nodes = structure.nodes
    val bars = structure.bars

    // Determine bounding box of nodes for scaling
    val minX = nodes.minOfOrNull { it.x } ?: 0.0
    val maxX = nodes.maxOfOrNull { it.x } ?: 6.0
    val minY = nodes.minOfOrNull { it.y } ?: 0.0
    val maxY = nodes.maxOfOrNull { it.y } ?: 3.0

    val rangeX = max(1.0, maxX - minX)
    val rangeY = max(1.0, maxY - minY)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = structure.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = "Treillis Plan • ${nodes.size} Nœuds, ${bars.size} Barres",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = if (solution.isStable) ReactionGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (solution.isIsostatic) "Isostatique (h=0)" else if (solution.isStable) "Hyperstatique" else "Instable",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (solution.isStable) ReactionGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val padX = 40.dp.toPx()
                val padY = 35.dp.toPx()

                val drawW = canvasWidth - 2 * padX
                val drawH = canvasHeight - 2 * padY

                // Coordinate mapper: Y is inverted in screen space
                val scale = min(drawW / rangeX, drawH / rangeY).toFloat()

                fun nodeToPx(node: TrussNode): Offset {
                    val px = padX + (node.x - minX).toFloat() * scale
                    val py = (canvasHeight - padY) - (node.y - minY).toFloat() * scale
                    return Offset(px, py)
                }

                // 1. Draw Bars (Colored by force state)
                bars.forEach { bar ->
                    val sNode = nodes.firstOrNull { it.id == bar.nodeStartId }
                    val eNode = nodes.firstOrNull { it.id == bar.nodeEndId }
                    val forceRes = solution.barResults.firstOrNull { it.barId == bar.id }

                    if (sNode != null && eNode != null) {
                        val p1 = nodeToPx(sNode)
                        val p2 = nodeToPx(eNode)

                        val barColor = when (forceRes?.state) {
                            TrussBarState.TRACTION -> Color(0xFF2563EB) // Blue for tension (+)
                            TrussBarState.COMPRESSION -> Color(0xFFDC2626) // Red for compression (-)
                            TrussBarState.ZERO_FORCE, null -> Color(0xFF94A3B8) // Gray for zero-force
                        }

                        val strokeW = if (forceRes?.state == TrussBarState.ZERO_FORCE) 1.8.dp.toPx() else 3.5.dp.toPx()
                        val pathEffect = if (forceRes?.state == TrussBarState.ZERO_FORCE) {
                            PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        } else null

                        drawLine(
                            color = barColor,
                            start = p1,
                            end = p2,
                            strokeWidth = strokeW,
                            pathEffect = pathEffect
                        )
                    }
                }

                // 2. Draw Supports at Support Nodes
                nodes.filter { it.support != null }.forEach { node ->
                    val p = nodeToPx(node)
                    drawSupportIcon(node.support!!, p.x, p.y)
                }

                // 3. Draw External Load Arrows
                nodes.forEach { node ->
                    val p = nodeToPx(node)
                    if (abs(node.fy_kN) > 0.01) {
                        drawNodalForceY(p.x, p.y, node.fy_kN)
                    }
                    if (abs(node.fx_kN) > 0.01) {
                        drawNodalForceX(p.x, p.y, node.fx_kN)
                    }
                }

                // 4. Draw Nodes (Joints)
                nodes.forEach { node ->
                    val p = nodeToPx(node)
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 6.dp.toPx(),
                        center = p,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Color Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF2563EB), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Traction (+)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFDC2626), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compression (-)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF94A3B8), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Barre Nulle (0)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            }
        }
    }
}

private fun DrawScope.drawSupportIcon(type: SupportType, x: Float, y: Float) {
    val h = 18.dp.toPx()
    val w = 18.dp.toPx()

    val path = Path().apply {
        moveTo(x, y)
        lineTo(x - w / 2, y + h)
        lineTo(x + w / 2, y + h)
        close()
    }
    val col = if (type == SupportType.PINNED) Color(0xFF1E3A8A) else Color(0xFF0284C7)
    drawPath(path, color = col.copy(alpha = 0.25f))
    drawPath(path, color = col, style = Stroke(width = 2.dp.toPx()))

    // Ground line
    drawLine(
        color = Color(0xFF0F172A),
        start = Offset(x - w * 0.7f, y + h),
        end = Offset(x + w * 0.7f, y + h),
        strokeWidth = 2.dp.toPx()
    )
}

private fun DrawScope.drawNodalForceY(x: Float, y: Float, fy: Double) {
    val arrowLen = 28.dp.toPx()
    val dir = if (fy < 0) 1f else -1f // Negative fy = downward force
    val startY = y - dir * arrowLen
    val endY = y
    val arrowColor = Color(0xFFDC2626)

    drawLine(
        color = arrowColor,
        start = Offset(x, startY),
        end = Offset(x, endY),
        strokeWidth = 2.5.dp.toPx()
    )
    val headPath = Path().apply {
        moveTo(x, endY)
        lineTo(x - 4.dp.toPx(), endY - dir * 6.dp.toPx())
        lineTo(x + 4.dp.toPx(), endY - dir * 6.dp.toPx())
        close()
    }
    drawPath(headPath, color = arrowColor)
}

private fun DrawScope.drawNodalForceX(x: Float, y: Float, fx: Double) {
    val arrowLen = 28.dp.toPx()
    val dir = if (fx > 0) 1f else -1f
    val startX = x - dir * arrowLen
    val endX = x
    val arrowColor = Color(0xFFDC2626)

    drawLine(
        color = arrowColor,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 2.5.dp.toPx()
    )
    val headPath = Path().apply {
        moveTo(endX, y)
        lineTo(endX - dir * 6.dp.toPx(), y - 4.dp.toPx())
        lineTo(endX - dir * 6.dp.toPx(), y + 4.dp.toPx())
        close()
    }
    drawPath(headPath, color = arrowColor)
}
