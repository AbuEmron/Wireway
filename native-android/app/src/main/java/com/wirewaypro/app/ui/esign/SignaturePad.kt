package com.wirewaypro.app.ui.esign

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * A freehand signature pad. Captures strokes with raw pointer input (smooth,
 * finger-friendly) and can flatten them to an Android [Bitmap] with a transparent
 * background, so the signature composites cleanly onto the sealed proposal PDF.
 *
 * Deliberately dependency-free (no signature library) — it's part of the
 * extractable e-signature module.
 */
class SignaturePadState {
    /** Each stroke is the list of points from pen-down to pen-up. */
    val strokes: SnapshotStateList<SnapshotStateList<Offset>> = mutableStateListOf()
    internal var size: IntSize = IntSize.Zero

    val isEmpty: Boolean get() = strokes.all { it.size < 2 }

    fun clear() = strokes.clear()

    /**
     * Render the strokes to a transparent ARGB bitmap sized to the pad. Returns
     * null if nothing was drawn or the pad hasn't been laid out yet.
     */
    fun toBitmap(strokeColor: Int = 0xFF0A0E14.toInt(), strokeWidthPx: Float = 5f): Bitmap? {
        if (isEmpty || size.width <= 0 || size.height <= 0) return null
        val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bmp)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = AndroidPaint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }
        for (stroke in strokes) {
            if (stroke.size < 2) {
                // A dot — draw a small filled point so single taps still register.
                if (stroke.size == 1) canvas.drawPoint(stroke[0].x, stroke[0].y, paint)
                continue
            }
            val path = AndroidPath().apply {
                moveTo(stroke[0].x, stroke[0].y)
                for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
            }
            canvas.drawPath(path, paint)
        }
        return bmp
    }
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

@Composable
fun SignaturePad(
    state: SignaturePadState,
    modifier: Modifier = Modifier,
) {
    val strokeWidth = with(LocalDensity.current) { 2.5.dp.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F7F9))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val stroke = mutableStateListOf(down.position)
                    state.strokes.add(stroke)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        stroke.add(change.position)
                        change.consume()
                    }
                }
            },
    ) {
        state.size = IntSize(size.width.toInt(), size.height.toInt())
        for (stroke in state.strokes) {
            if (stroke.size < 2) continue
            val path = Path().apply {
                moveTo(stroke[0].x, stroke[0].y)
                for (i in 1 until stroke.size) lineTo(stroke[i].x, stroke[i].y)
            }
            drawPath(
                path = path,
                color = Color(0xFF0A0E14),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
