package com.nazli.pdkresimapp

import android.graphics.*
import kotlin.math.max
import kotlin.math.min
import android.graphics.Bitmap

class CizimYoneticisi {
    // Listeler
    val actions = ArrayList<DrawAction>()
    val undoActions = ArrayList<DrawAction>()
    // Çizim Araçları
    var currentPath: Path = Path()
    var currentPaint: Paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 20f
        isAntiAlias = true
    }
    // Şekil Geçici Değişkenleri
    var shapeStartX = 0f; var shapeStartY = 0f
    var shapeCurrentX = 0f; var shapeCurrentY = 0f
    var currentShapeType = ShapeType.RECTANGLE
    // Seçim
    var seciliMetin: DrawAction.TextAction? = null
    val selectionPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }
    fun addAction(action: DrawAction) {
        actions.add(action)
        undoActions.clear() // Yeni işlem gelince ileri al (Redo) listesi temizlenir
    }
    fun clear() {
        actions.clear()
        undoActions.clear()
        currentPath.reset()
    }
    fun undo() {
        if (actions.isNotEmpty()) {
            undoActions.add(actions.removeAt(actions.size - 1))
        }
    }
    fun redo() {
        if (undoActions.isNotEmpty()) {
            actions.add(undoActions.removeAt(undoActions.size - 1))
        }
    }
    // Çizim Fonksiyonu (Canvas üzerine her şeyi çizer)
    fun drawContent(canvas: Canvas, scaleFactor: Float) {
        for (action in actions) {
            when (action) {
                is DrawAction.BitmapAction -> {
                    canvas.drawBitmap(action.bitmap, 0f, 0f, null)
                }

                is DrawAction.PathAction -> canvas.drawPath(action.path, action.paint)

                is DrawAction.TextAction -> {
                    canvas.drawText(action.text, action.x, action.y, action.paint)
                    // Seçili metin çerçevesi
                    if (action == seciliMetin) {
                        val bounds = Rect()
                        action.paint.getTextBounds(action.text, 0, action.text.length, bounds)
                        selectionPaint.strokeWidth = 3f / scaleFactor // Zoom'a göre kalınlık ayarla
                        val rect = RectF(
                            action.x + bounds.left - 15, action.y + bounds.top - 15,
                            action.x + bounds.right + 15, action.y + bounds.bottom + 15
                        )
                        canvas.drawRect(rect, selectionPaint)
                    }
                }
                is DrawAction.ShapeAction -> drawShape(canvas, action.type, action.startX, action.startY, action.endX, action.endY, action.paint)
            }
        }
    }
    fun drawPreview(canvas: Canvas, mod: Mod) {
        if (mod == Mod.KALEM || mod == Mod.SILGI) {
            if (!currentPath.isEmpty) canvas.drawPath(currentPath, currentPaint)
        } else if (mod == Mod.SEKIL) {
            drawShape(canvas, currentShapeType, shapeStartX, shapeStartY, shapeCurrentX, shapeCurrentY, currentPaint)
        }
    }
    private fun drawShape(c: Canvas, t: ShapeType, sx: Float, sy: Float, ex: Float, ey: Float, p: Paint) {
        val l = min(sx, ex); val tp = min(sy, ey); val r = max(sx, ex); val b = max(sy, ey)
        when (t) {
            ShapeType.RECTANGLE -> c.drawRect(l, tp, r, b, p)
            ShapeType.OVAL -> c.drawOval(l, tp, r, b, p)
            ShapeType.LINE -> c.drawLine(sx, sy, ex, ey, p)
        }
    }
}