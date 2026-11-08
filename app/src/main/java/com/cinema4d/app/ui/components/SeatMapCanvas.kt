package com.cinema4d.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinema4d.app.data.model.RowInfo
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.ui.theme.SeatAvailable
import com.cinema4d.app.ui.theme.SeatAvailableBorder
import com.cinema4d.app.ui.theme.SeatOccupied
import com.cinema4d.app.ui.theme.SeatOccupiedBorder
import com.cinema4d.app.ui.theme.SeatSelected
import com.cinema4d.app.ui.theme.TierClassic
import com.cinema4d.app.ui.theme.TierPrime
import com.cinema4d.app.ui.theme.TierRecliner
import kotlin.math.abs

private data class SeatCell(
    val seat: SeatInfo,
    val center: Offset,
    val w: Float,
    val h: Float,
)

private data class SeatMapLayout(
    val cells: List<SeatCell>,
    val labelWidth: Float,
    val screenArea: Float,
    val mapWidth: Float,
    val orderedRows: List<RowInfo>,
)

private fun tierColor(tier: String): Color = when (tier) {
    "prime" -> TierPrime
    "recliner" -> TierRecliner
    else -> TierClassic
}

@Composable
fun SeatMapCanvas(
    seatMap: SeatMapDocument,
    occupied: Set<String>,
    selected: Set<String>,
    previewSeatId: String?,
    interactive: Boolean,
    onSeatClick: (SeatInfo) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val layout = remember(wPx, hPx, seatMap, compact) {
            computeLayout(seatMap, wPx, hPx, compact, density)
        }

        val screenAccent = MaterialTheme.colorScheme.primary

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(interactive, layout) {
                    if (!interactive) return@pointerInput
                    detectTapGestures { tap ->
                        val hit = layout.cells.minByOrNull {
                            abs(it.center.x - tap.x) + abs(it.center.y - tap.y)
                        }
                        if (hit != null &&
                            abs(hit.center.x - tap.x) < hit.w * 0.9f &&
                            abs(hit.center.y - tap.y) < hit.h * 1.1f
                        ) {
                            onSeatClick(hit.seat)
                        }
                    }
                },
        ) {
            drawSeatMap(layout, occupied, selected, previewSeatId, textMeasurer, compact, screenAccent)
        }
    }
}

private fun computeLayout(
    seatMap: SeatMapDocument,
    wPx: Float,
    hPx: Float,
    compact: Boolean,
    density: androidx.compose.ui.unit.Density,
): SeatMapLayout {
    val labelWidth = with(density) { (if (compact) 14 else 30).dp.toPx() }
    val screenArea = with(density) { (if (compact) 16 else 40).dp.toPx() }
    val sidePad = with(density) { 8.dp.toPx() }
    val bottomPad = with(density) { 6.dp.toPx() }
    val mapW = wPx - labelWidth - sidePad * 2
    val mapH = hPx - screenArea - bottomPad

    val orderedRows = seatMap.rows.sortedBy { it.y }
    val byRow = seatMap.seats.groupBy { it.row }
    val maxAbsX = seatMap.seats.maxOfOrNull { abs(it.x) }?.plus(0.6f) ?: 6f
    val xScale = if (mapW > 0 && maxAbsX > 0) mapW / (maxAbsX * 2f) else 0f
    val rowGap = if (orderedRows.isNotEmpty() && mapH > 0) mapH / orderedRows.size else 0f

    val typicalPitch = seatMap.seats.map { it.x }
        .zipWithNext()
        .map { abs(it.second - it.first) }
        .filter { it in 0.2f..1.5f }
        .minOrNull() ?: 0.63f
    val seatW = typicalPitch * xScale * 0.78f
    val seatH = (rowGap * if (compact) 0.55f else 0.6f).coerceAtMost(seatW * 1.15f)

    val cells = mutableListOf<SeatCell>()
    orderedRows.forEachIndexed { rowIdx, row ->
        val seats = byRow[row.letter] ?: return@forEachIndexed
        val cy = screenArea + rowIdx * rowGap + rowGap / 2
        seats.forEach { seat ->
            val cx = labelWidth + sidePad + mapW / 2 + seat.x * xScale
            cells += SeatCell(seat, Offset(cx, cy), seatW, seatH)
        }
    }
    return SeatMapLayout(cells, labelWidth, screenArea, mapW, orderedRows)
}

private fun DrawScope.drawSeatMap(
    layout: SeatMapLayout,
    occupied: Set<String>,
    selected: Set<String>,
    previewSeatId: String?,
    textMeasurer: TextMeasurer,
    compact: Boolean,
    screenAccent: Color,
) {
    // screen bar
    drawRoundRect(
        color = screenAccent.copy(alpha = if (compact) 0.22f else 0.14f),
        topLeft = Offset(layout.labelWidth + layout.mapWidth * 0.08f, layout.screenArea * 0.22f),
        size = Size(layout.mapWidth * 0.84f, layout.screenArea * 0.5f),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    if (!compact) {
        val style = TextStyle(color = Color(0xFFB9C2D4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val t = textMeasurer.measure("SCREEN", style)
        drawText(
            textMeasurer, "SCREEN",
            Offset(
                layout.labelWidth + layout.mapWidth / 2 - t.size.width / 2,
                layout.screenArea * 0.47f - t.size.height / 2,
            ),
            style,
        )
    }

    // seats
    layout.cells.forEach { cell ->
        val seat = cell.seat
        val topLeft = Offset(cell.center.x - cell.w / 2, cell.center.y - cell.h / 2)
        val sz = Size(cell.w, cell.h)
        val radius = CornerRadius(minOf(cell.w, cell.h) * 0.28f)
        when {
            seat.id in selected -> drawRoundRect(SeatSelected, topLeft, sz, radius)
            seat.id in occupied -> {
                drawRoundRect(SeatOccupied, topLeft, sz, radius)
                drawRoundRect(SeatOccupiedBorder, topLeft, sz, radius, style = Stroke(1.dp.toPx()))
            }
            else -> {
                drawRoundRect(SeatAvailable, topLeft, sz, radius)
                drawRoundRect(tierColor(seat.tier), topLeft, sz, radius, style = Stroke(1.4.dp.toPx()))
            }
        }
        // row label
        if (!compact && cell == layout.cells.firstOrNull { it.seat.row == seat.row && it.center.x < cell.center.x }) {
            // drawn once per row below instead
        }
    }

    // row labels once per row
    if (!compact) {
        val style = TextStyle(color = Color(0xFF8A93A8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        layout.orderedRows.forEachIndexed { rowIdx, row ->
            val firstCell = layout.cells.firstOrNull { it.seat.row == row.letter } ?: return@forEachIndexed
            val t = textMeasurer.measure(row.letter, style)
            drawText(
                textMeasurer, row.letter,
                Offset(layout.labelWidth / 2 - t.size.width / 2, firstCell.center.y - t.size.height / 2),
                style,
            )
        }
    }

    // preview ring
    previewSeatId?.let { pid ->
        layout.cells.firstOrNull { it.seat.id == pid }?.let { cell ->
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(
                    cell.center.x - cell.w / 2 - 3.dp.toPx(),
                    cell.center.y - cell.h / 2 - 3.dp.toPx(),
                ),
                size = Size(cell.w + 6.dp.toPx(), cell.h + 6.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
