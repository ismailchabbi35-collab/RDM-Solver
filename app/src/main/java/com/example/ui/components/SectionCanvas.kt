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
import androidx.compose.ui.geometry.Size
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
fun SectionCanvas(
    section: CrossSection,
    props: SectionProperties,
    stress: StressResult?,
    material: Material,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

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
                    text = "Section Transversale & Profils de Contraintes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Text(
                    text = "${section.shapeType.labelFr} • ${material.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            stress?.let { st ->
                val badgeColor = if (st.isSafe) ReactionGreen else MaterialTheme.colorScheme.error
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "η = %.1f%% (%s)".format(Locale.US, st.utilizationRatio * 100.0, if (st.isSafe) "Conforme" else "Surcharge"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Layout partitions:
                // Left 40%: Section cross-section shape
                // Middle 30%: Normal stress profile σ(y)
                // Right 30%: Shear stress profile τ(y)
                val secCenterX = canvasWidth * 0.22f
                val secCenterY = canvasHeight * 0.50f
                val secMaxHeightPx = canvasHeight * 0.72f

                val totalH = props.totalHeight_mm.toFloat()
                val scale = secMaxHeightPx / totalH

                // 1. Draw Section Shape
                drawCrossSectionShape(section, props, secCenterX, secCenterY, scale)

                // 2. Draw Neutral Axis (Axe Neutre passant par G) across Section and Stress Diagrams
                val neutralAxisY = secCenterY + (totalH / 2.0f - props.centroidY_inf_mm.toFloat()) * scale
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(secCenterX - 45.dp.toPx(), neutralAxisY),
                    end = Offset(canvasWidth - 10.dp.toPx(), neutralAxisY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )

                // 3. Draw Normal Stress Butterfly Diagram σ(y) (Navier)
                val sigmaCenterX = canvasWidth * 0.58f
                val topY = secCenterY - (totalH / 2.0f) * scale
                val bottomY = secCenterY + (totalH / 2.0f) * scale

                // Baseline for σ=0
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(sigmaCenterX, topY),
                    end = Offset(sigmaCenterX, bottomY),
                    strokeWidth = 1.5.dp.toPx()
                )

                if (stress != null) {
                    val maxS = max(abs(stress.normalStressSup_MPa), abs(stress.normalStressInf_MPa)).coerceAtLeast(1.0)
                    val sScale = (32.dp.toPx() / maxS.toFloat())

                    val sigTopPx = (stress.normalStressSup_MPa.toFloat() * sScale)
                    val sigBottomPx = (stress.normalStressInf_MPa.toFloat() * sScale)

                    // Top triangle (Compression: negative => to the left)
                    val topPath = Path().apply {
                        moveTo(sigmaCenterX, neutralAxisY)
                        lineTo(sigmaCenterX + sigTopPx, topY)
                        lineTo(sigmaCenterX, topY)
                        close()
                    }
                    drawPath(topPath, color = Color(0xFF3B82F6).copy(alpha = 0.25f))
                    drawLine(
                        color = Color(0xFF2563EB),
                        start = Offset(sigmaCenterX, neutralAxisY),
                        end = Offset(sigmaCenterX + sigTopPx, topY),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Bottom triangle (Tension: positive => to the right)
                    val bottomPath = Path().apply {
                        moveTo(sigmaCenterX, neutralAxisY)
                        lineTo(sigmaCenterX + sigBottomPx, bottomY)
                        lineTo(sigmaCenterX, bottomY)
                        close()
                    }
                    drawPath(bottomPath, color = Color(0xFFEF4444).copy(alpha = 0.25f))
                    drawLine(
                        color = Color(0xFFDC2626),
                        start = Offset(sigmaCenterX, neutralAxisY),
                        end = Offset(sigmaCenterX + sigBottomPx, bottomY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 4. Draw Shear Stress Parabolic Diagram τ(y) (Jourawski)
                val tauCenterX = canvasWidth * 0.86f
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(tauCenterX, topY),
                    end = Offset(tauCenterX, bottomY),
                    strokeWidth = 1.5.dp.toPx()
                )

                if (stress != null) {
                    val maxT = stress.maxShearStress_MPa.coerceAtLeast(0.5)
                    val tScale = (34.dp.toPx() / maxT.toFloat())
                    val tauMaxPx = (stress.maxShearStress_MPa.toFloat() * tScale)

                    // Parabolic curve: 0 at top, max at NA, 0 at bottom
                    val tauPath = Path().apply {
                        moveTo(tauCenterX, topY)
                        cubicTo(
                            tauCenterX + tauMaxPx, topY + (neutralAxisY - topY) * 0.5f,
                            tauCenterX + tauMaxPx, neutralAxisY,
                            tauCenterX + tauMaxPx, neutralAxisY
                        )
                        cubicTo(
                            tauCenterX + tauMaxPx, neutralAxisY + (bottomY - neutralAxisY) * 0.5f,
                            tauCenterX, bottomY,
                            tauCenterX, bottomY
                        )
                        close()
                    }
                    drawPath(tauPath, color = ShearColor.copy(alpha = 0.25f))
                    drawPath(tauPath, color = ShearColor, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }

        // Labels beneath diagrams
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Section G (Iz = %.1f cm⁴)".format(Locale.US, props.inertiaIz_cm4),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "σ(y) Flexion (Navier)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "τ(y) Cisaillement (Jourawski)",
                style = MaterialTheme.typography.labelSmall,
                color = ShearColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun DrawScope.drawCrossSectionShape(
    section: CrossSection,
    props: SectionProperties,
    cx: Float,
    cy: Float,
    scale: Float
) {
    val steelColor = Color(0xFF334155)
    val steelFill = Color(0xFF64748B).copy(alpha = 0.35f)

    when (section.shapeType) {
        SectionShapeType.RECTANGLE -> {
            val w = section.width_mm.toFloat() * scale
            val h = section.height_mm.toFloat() * scale
            drawRect(
                color = steelFill,
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h)
            )
            drawRect(
                color = steelColor,
                topLeft = Offset(cx - w / 2, cy - h / 2),
                size = Size(w, h),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        SectionShapeType.I_BEAM -> {
            val b = section.width_mm.toFloat() * scale
            val h = section.height_mm.toFloat() * scale
            val tf = section.flangeThickness_mm.toFloat() * scale
            val tw = section.webThickness_mm.toFloat() * scale

            val path = Path().apply {
                moveTo(cx - b / 2, cy - h / 2) // top flange top-left
                lineTo(cx + b / 2, cy - h / 2)
                lineTo(cx + b / 2, cy - h / 2 + tf)
                lineTo(cx + tw / 2, cy - h / 2 + tf)
                lineTo(cx + tw / 2, cy + h / 2 - tf)
                lineTo(cx + b / 2, cy + h / 2 - tf)
                lineTo(cx + b / 2, cy + h / 2)
                lineTo(cx - b / 2, cy + h / 2)
                lineTo(cx - b / 2, cy + h / 2 - tf)
                lineTo(cx - tw / 2, cy + h / 2 - tf)
                lineTo(cx - tw / 2, cy - h / 2 + tf)
                lineTo(cx - b / 2, cy - h / 2 + tf)
                close()
            }
            drawPath(path, color = steelFill)
            drawPath(path, color = steelColor, style = Stroke(width = 2.dp.toPx()))
        }
        SectionShapeType.T_BEAM -> {
            val b = section.width_mm.toFloat() * scale
            val h = section.height_mm.toFloat() * scale
            val tf = section.flangeThickness_mm.toFloat() * scale
            val tw = section.webThickness_mm.toFloat() * scale

            val path = Path().apply {
                moveTo(cx - b / 2, cy - h / 2)
                lineTo(cx + b / 2, cy - h / 2)
                lineTo(cx + b / 2, cy - h / 2 + tf)
                lineTo(cx + tw / 2, cy - h / 2 + tf)
                lineTo(cx + tw / 2, cy + h / 2)
                lineTo(cx - tw / 2, cy + h / 2)
                lineTo(cx - tw / 2, cy - h / 2 + tf)
                lineTo(cx - b / 2, cy - h / 2 + tf)
                close()
            }
            drawPath(path, color = steelFill)
            drawPath(path, color = steelColor, style = Stroke(width = 2.dp.toPx()))
        }
        SectionShapeType.CIRCLE -> {
            val r = (section.diameter_mm.toFloat() / 2.0f) * scale
            drawCircle(color = steelFill, radius = r, center = Offset(cx, cy))
            drawCircle(color = steelColor, radius = r, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
        }
        SectionShapeType.HOLLOW_BOX -> {
            val b = section.width_mm.toFloat() * scale
            val h = section.height_mm.toFloat() * scale
            val bIn = section.innerWidth_mm.toFloat() * scale
            val hIn = section.innerHeight_mm.toFloat() * scale

            val outerPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(cx - b / 2, cy - h / 2, cx + b / 2, cy + h / 2))
                addRect(androidx.compose.ui.geometry.Rect(cx - bIn / 2, cy - hIn / 2, cx + bIn / 2, cy + hIn / 2))
                fillType = PathFillType.EvenOdd
            }
            drawPath(outerPath, color = steelFill)
            drawRect(color = steelColor, topLeft = Offset(cx - b / 2, cy - h / 2), size = Size(b, h), style = Stroke(width = 2.dp.toPx()))
            drawRect(color = steelColor, topLeft = Offset(cx - bIn / 2, cy - hIn / 2), size = Size(bIn, hIn), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}
