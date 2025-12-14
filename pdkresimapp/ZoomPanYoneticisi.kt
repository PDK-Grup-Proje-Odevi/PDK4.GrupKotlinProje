package com.nazli.pdkresimapp
import android.graphics.*
import kotlin.math.max
import kotlin.math.min
class ZoomPanYoneticisi {
    val drawMatrix = Matrix()
    var scaleFactor = 1.0f
    // Sınırlar
    private val MIN_ZOOM = 1.0f
    private val MAX_ZOOM = 5.0f
    // Minimap Boyaları
    private val minimapPaint = Paint().apply { color = Color.argb(150, 200, 200, 200); style = Paint.Style.FILL }
    private val minimapBorderPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val viewportPaint = Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 3f }
    private val minimapWidth = 200f
    private val minimapMargin = 30f
    fun reset() {
        drawMatrix.reset()
        scaleFactor = 1.0f
    }
    fun applyZoom(scale: Float, focusX: Float, focusY: Float) {
        val oldScale = scaleFactor
        scaleFactor *= scale
        // Sınırla
        if (scaleFactor < MIN_ZOOM) scaleFactor = MIN_ZOOM
        else if (scaleFactor > MAX_ZOOM) scaleFactor = MAX_ZOOM
        // Eğer değişiklik varsa uygula
        if (scaleFactor != oldScale) {
            val correctedScale = scaleFactor / oldScale
            drawMatrix.postScale(correctedScale, correctedScale, focusX, focusY)
        } }
    fun checkBounds(viewW: Float, viewH: Float) {
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        val scale = values[Matrix.MSCALE_X]
        val imageW = viewW * scale
        val imageH = viewH * scale
        var deltaX = 0f; var deltaY = 0f
        if (imageW <= viewW) deltaX = (viewW - imageW) / 2 - transX
        else if (transX > 0) deltaX = -transX
        else if (transX < viewW - imageW) deltaX = viewW - imageW - transX
        if (imageH <= viewH) deltaY = (viewH - imageH) / 2 - transY
        else if (transY > 0) deltaY = -transY
        else if (transY < viewH - imageH) deltaY = viewH - imageH - transY
        drawMatrix.postTranslate(deltaX, deltaY)
    }
    fun drawMinimap(canvas: Canvas, viewW: Float, viewH: Float) {
        if (viewW == 0f || scaleFactor <= 1.0f) return
        val ratio = viewH / viewW
        val mapH = minimapWidth * ratio
        val mapX = viewW - minimapWidth - minimapMargin
        val mapY = minimapMargin
        val mapRect = RectF(mapX, mapY, mapX + minimapWidth, mapY + mapH)
        canvas.drawRect(mapRect, minimapPaint)
        canvas.drawRect(mapRect, minimapBorderPaint)
        val visibleRect = RectF(0f, 0f, viewW, viewH)
        val mInverse = Matrix()
        drawMatrix.invert(mInverse)
        mInverse.mapRect(visibleRect)
        val scaleX = minimapWidth / viewW
        val scaleY = mapH / viewH
        val indLeft = mapX + (visibleRect.left * scaleX)
        val indTop = mapY + (visibleRect.top * scaleY)
        val indRight = mapX + (visibleRect.right * scaleX)
        val indBottom = mapY + (visibleRect.bottom * scaleY)
        val indicatorRect = RectF(indLeft, indTop, indRight, indBottom)
        if (indicatorRect.intersect(mapRect)) canvas.drawRect(indicatorRect, viewportPaint)
        else canvas.drawRect(mapRect, viewportPaint)
    }
    // Koordinat Dönüşümü (Ekran -> Tuval)
    fun toCanvasCoordinates(touchX: Float, touchY: Float): PointF {
        val src = floatArrayOf(touchX, touchY)
        val dst = floatArrayOf(0f, 0f)
        val mInverse = Matrix()
        drawMatrix.invert(mInverse)
        mInverse.mapPoints(dst, src)
        return PointF(dst[0], dst[1])
    }
}