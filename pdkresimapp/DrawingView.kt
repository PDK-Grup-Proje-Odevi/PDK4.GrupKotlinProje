package com.nazli.pdkresimapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    // --- YÖNETİCİLER ---
    private val cizimYoneticisi = CizimYoneticisi()
    private val zoomPanYoneticisi = ZoomPanYoneticisi()
    // --- DURUM DEĞİŞKENLERİ ---
    private var suankiMod = Mod.KALEM
    private var oncekiMod = Mod.KALEM
    private var backgroundBitmap: Bitmap? = null
    // --- DAMLALIK OPTİMİZASYONU ---
    private val singlePixelBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    private val singlePixelCanvas = Canvas(singlePixelBitmap)
    // Dokunmatik Durumları
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isZoomingOrPanning = false
    private var isLongPressPanning = false
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    // --- SEÇİM VE BOYUTLANDIRMA ---
    private var isResizingText = false
    private val resizeHandleRadius = 25f
    // Seçim Görselleri
    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#009688")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
    }
    private val handlePaint = Paint().apply {
        color = Color.parseColor("#009688")
        style = Paint.Style.FILL
    }
    // Callbackler
    var onCanvasClick: ((Float, Float) -> Unit)? = null
    var onColorPicked: ((Int) -> Unit)? = null
    var onTextSelected: ((Boolean) -> Unit)? = null
    // --- DEDEKTÖRLER ---
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isResizingText) return false
            zoomPanYoneticisi.applyZoom(detector.scaleFactor, detector.focusX, detector.focusY)
            zoomPanYoneticisi.checkBounds(width.toFloat(), height.toFloat())
            invalidate()
            return true
        }
    })
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val p = zoomPanYoneticisi.toCanvasCoordinates(e.x, e.y)
            if (suankiMod == Mod.TASIMA) {
                var tiklanan: DrawAction.TextAction? = null
                // Tutamaç kontrolü
                if (cizimYoneticisi.seciliMetin != null) {
                    val rect = getSelectedTextRect(cizimYoneticisi.seciliMetin!!)
                    val touchRadius = (resizeHandleRadius / zoomPanYoneticisi.scaleFactor) * 2
                    val dx = p.x - rect.right
                    val dy = p.y - rect.bottom
                    if (dx * dx + dy * dy < touchRadius * touchRadius) return true
                }
                // Metne tıklandı mı?
                for (i in cizimYoneticisi.actions.indices.reversed()) {
                    val action = cizimYoneticisi.actions[i]
                    if (action is DrawAction.TextAction && isTouchingText(action, p.x, p.y)) {
                        tiklanan = action
                        break
                    }
                }
                if (tiklanan != null) {
                    cizimYoneticisi.seciliMetin = tiklanan
                    onTextSelected?.invoke(true)
                } else {
                    secimiKaldir()
                }
                invalidate()
                return true
            }
            if (suankiMod == Mod.METIN) { onCanvasClick?.invoke(p.x, p.y); return true }
            return false
        }
        override fun onLongPress(e: MotionEvent) {
            val p = zoomPanYoneticisi.toCanvasCoordinates(e.x, e.y)
            var metinBulundu = false

            for (i in cizimYoneticisi.actions.indices.reversed()) {
                val action = cizimYoneticisi.actions[i]
                if (action is DrawAction.TextAction && isTouchingText(action, p.x, p.y)) {
                    metinBulundu = true
                    if (suankiMod != Mod.TASIMA) oncekiMod = suankiMod
                    suankiMod = Mod.TASIMA
                    cizimYoneticisi.seciliMetin = action
                    dragOffsetX = p.x - action.x
                    dragOffsetY = p.y - action.y
                    cizimYoneticisi.currentPath.reset()
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onTextSelected?.invoke(true)
                    invalidate()
                    break
                }
            }
            if (!metinBulundu) {
                isLongPressPanning = true
                lastTouchX = e.x; lastTouchY = e.y
                cizimYoneticisi.currentPath.reset()
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    })
    // --- ÇİZİM ---
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.concat(zoomPanYoneticisi.drawMatrix)

        backgroundBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) } ?: canvas.drawColor(Color.WHITE)
        cizimYoneticisi.drawContent(canvas, zoomPanYoneticisi.scaleFactor)
        cizimYoneticisi.drawPreview(canvas, suankiMod)
        if (cizimYoneticisi.seciliMetin != null) {
            val textAction = cizimYoneticisi.seciliMetin!!
            val rect = getSelectedTextRect(textAction)
            selectionPaint.strokeWidth = 3f / zoomPanYoneticisi.scaleFactor
            canvas.drawRect(rect, selectionPaint)
            val handleRadiusAdjusted = resizeHandleRadius / zoomPanYoneticisi.scaleFactor
            canvas.drawCircle(rect.right, rect.bottom, handleRadiusAdjusted, handlePaint)
        }
        canvas.restore()
        zoomPanYoneticisi.drawMinimap(canvas, width.toFloat(), height.toFloat())
    }
    // --- DOKUNMATİK OLAYLAR ---
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        val p = zoomPanYoneticisi.toCanvasCoordinates(event.x, event.y)
        val cx = p.x; val cy = p.y
        if (event.pointerCount > 1) { isZoomingOrPanning = true; return true }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isZoomingOrPanning = false; isLongPressPanning = false
                isResizingText = false
                lastTouchX = event.x; lastTouchY = event.y
                // Hızlı Damlalık
                if (suankiMod == Mod.DAMLALIK) {
                    val pixelColor = getPixelAt(cx, cy)
                    setPenColor(pixelColor)
                    onColorPicked?.invoke(pixelColor)
                    return true
                }
                // Seçim / Boyutlandırma / Taşıma
                if (cizimYoneticisi.seciliMetin != null) {
                    val rect = getSelectedTextRect(cizimYoneticisi.seciliMetin!!)
                    val dx = cx - rect.right
                    val dy = cy - rect.bottom
                    val touchRadius = (resizeHandleRadius / zoomPanYoneticisi.scaleFactor) * 2.5f

                    if (dx * dx + dy * dy < touchRadius * touchRadius) {
                        isResizingText = true
                        return true
                    } else if (isTouchingText(cizimYoneticisi.seciliMetin!!, cx, cy)) {
                        suankiMod = Mod.TASIMA
                        dragOffsetX = cx - cizimYoneticisi.seciliMetin!!.x
                        dragOffsetY = cy - cizimYoneticisi.seciliMetin!!.y
                    } else if (suankiMod == Mod.TASIMA) {
                        secimiKaldir()
                    }
                }
                if (suankiMod == Mod.SEKIL) {
                    cizimYoneticisi.shapeStartX = cx; cizimYoneticisi.shapeStartY = cy
                    cizimYoneticisi.shapeCurrentX = cx; cizimYoneticisi.shapeCurrentY = cy
                    cizimYoneticisi.undoActions.clear()

                } else if (suankiMod == Mod.KALEM || suankiMod == Mod.SILGI) {
                    cizimYoneticisi.undoActions.clear()
                    cizimYoneticisi.currentPath.reset()
                    cizimYoneticisi.currentPath.moveTo(cx, cy)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (suankiMod == Mod.DAMLALIK) {
                    val pixelColor = getPixelAt(cx, cy)
                    setPenColor(pixelColor)
                    onColorPicked?.invoke(pixelColor)
                    return true
                }
                if (isResizingText && cizimYoneticisi.seciliMetin != null) {
                    val metin = cizimYoneticisi.seciliMetin!!
                    val yeniGenislik = (cx - metin.x).coerceAtLeast(10f)
                    val mevcutGenislik = metin.paint.measureText(metin.text)
                    if (mevcutGenislik > 0) {
                        val oran = yeniGenislik / mevcutGenislik
                        metin.paint.textSize *= oran
                    }
                    invalidate()
                    return true
                }
                if ((zoomPanYoneticisi.scaleFactor > 1.0f && !scaleDetector.isInProgress && isZoomingOrPanning) || isLongPressPanning) {
                    zoomPanYoneticisi.drawMatrix.postTranslate(event.x - lastTouchX, event.y - lastTouchY)
                    zoomPanYoneticisi.checkBounds(width.toFloat(), height.toFloat())
                    invalidate(); lastTouchX = event.x; lastTouchY = event.y
                    return true
                }
                if (suankiMod == Mod.SEKIL) {
                    cizimYoneticisi.shapeCurrentX = cx; cizimYoneticisi.shapeCurrentY = cy
                } else if (suankiMod == Mod.TASIMA && cizimYoneticisi.seciliMetin != null) {
                    cizimYoneticisi.seciliMetin!!.x = cx - dragOffsetX
                    cizimYoneticisi.seciliMetin!!.y = cy - dragOffsetY
                } else if (suankiMod == Mod.KALEM || suankiMod == Mod.SILGI) {
                    cizimYoneticisi.currentPath.lineTo(cx, cy)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                isResizingText = false
                // --- BOYA KOVASI ---
                if (suankiMod == Mod.KOVA) {
                    // Coroutine Başlat (Main Thread'de başla)
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // Ekranın fotoğrafını çek (UI Thread gerekir)
                            val currentBitmap = getBitmap()
                            val x = cx.toInt()
                            val y = cy.toInt()
                            if (x >= 0 && x < currentBitmap.width && y >= 0 && y < currentBitmap.height) {
                                val targetColor = currentBitmap.getPixel(x, y)
                                val newColor = cizimYoneticisi.currentPaint.color

                                if (targetColor != newColor) {
                                    // AĞIR İŞLEMİ ARKA PLANA AT (Default Dispatcher)
                                    val filledBitmap = withContext(Dispatchers.Default) {
                                        FloodFill.fill(currentBitmap, Point(x, y), targetColor, newColor)
                                    }
                                    // Sonucu UI'a bas
                                    if (filledBitmap != null) {
                                        cizimYoneticisi.addAction(DrawAction.BitmapAction(filledBitmap))
                                        invalidate()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    return true
                }
                if (suankiMod == Mod.DAMLALIK) {
                    val finalColor = getPixelAt(cx, cy)
                    onColorPicked?.invoke(finalColor)
                    setPenColor(finalColor)
                    kalem()
                    invalidate()
                    return true
                }
                if (isZoomingOrPanning || isLongPressPanning) {
                    isZoomingOrPanning = false; isLongPressPanning = false; return true
                }
                if (suankiMod == Mod.SEKIL) {
                    cizimYoneticisi.currentPath.lineTo(cx, cy)
                    cizimYoneticisi.addAction(DrawAction.ShapeAction(cizimYoneticisi.currentShapeType, cizimYoneticisi.shapeStartX, cizimYoneticisi.shapeStartY, cx, cy, Paint(cizimYoneticisi.currentPaint)))
                } else if (suankiMod == Mod.KALEM || suankiMod == Mod.SILGI) {
                    cizimYoneticisi.currentPath.lineTo(cx, cy)
                    cizimYoneticisi.addAction(DrawAction.PathAction(cizimYoneticisi.currentPath, Paint(cizimYoneticisi.currentPaint)))
                    cizimYoneticisi.currentPath = Path()
                }
                invalidate()
            }
        }
        return true
    }
    // --- YARDIMCILAR ---
    // 1x1 Piksel Hızlı Okuma
    private fun getPixelAt(x: Float, y: Float): Int {
        singlePixelCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        singlePixelCanvas.save()
        singlePixelCanvas.translate(-x, -y)
        backgroundBitmap?.let {
            singlePixelCanvas.drawBitmap(it, 0f, 0f, null)
        } ?: singlePixelCanvas.drawColor(Color.WHITE)
        cizimYoneticisi.drawContent(singlePixelCanvas, 1.0f)
        singlePixelCanvas.restore()
        return singlePixelBitmap.getPixel(0, 0)
    }
    private fun getSelectedTextRect(textAction: DrawAction.TextAction): RectF {
        val w = textAction.paint.measureText(textAction.text)
        val textSize = textAction.paint.textSize
        val padding = 10f
        return RectF(
            textAction.x - padding,
            textAction.y - textSize - padding / 2,
            textAction.x + w + padding,
            textAction.y + padding * 2
        )
    }
    fun resmiYukle(bmp: Bitmap) {
        if (width == 0 || height == 0) { post { resmiYukle(bmp) }; return }
        try {
            backgroundBitmap = Bitmap.createScaledBitmap(bmp, width, height, true)
            zoomPanYoneticisi.reset()
            cizimYoneticisi.clear()
            invalidate()
        } catch (e: Exception) { e.printStackTrace() }
    }
    fun getBitmap(): Bitmap {
        val w = if (width > 0) width else 1; val h = if (height > 0) height else 1
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        backgroundBitmap?.let { c.drawBitmap(it, null, Rect(0, 0, w, h), null) } ?: c.drawColor(Color.WHITE)
        cizimYoneticisi.drawContent(c, 1.0f)
        return bmp
    }
    private fun isTouchingText(t: DrawAction.TextAction, x: Float, y: Float): Boolean {
        val w = t.paint.measureText(t.text); val s = t.paint.textSize
        return (x in t.x..t.x + w) && (y in t.y - s..t.y + (s * 0.2f))
    }
    // API METOTLARI
    fun setPenColor(c: Int) {
        if (suankiMod != Mod.SILGI) cizimYoneticisi.currentPaint.color = c
    }
    fun setBrushSize(s: Float) {
        cizimYoneticisi.currentPaint.strokeWidth = s
        cizimYoneticisi.currentPaint.textSize = s * 2
    }
    fun getCurrentSize() = cizimYoneticisi.currentPaint.strokeWidth
    fun updateSelectedTextSize(s: Float) {
        cizimYoneticisi.seciliMetin?.let { it.paint.textSize = s; invalidate() }
    }
    fun isTextSelected() = cizimYoneticisi.seciliMetin != null
    fun secimiKaldir() {
        cizimYoneticisi.seciliMetin = null; suankiMod = oncekiMod; onTextSelected?.invoke(false); invalidate()
    }
    fun resetZoom() { zoomPanYoneticisi.reset(); invalidate() }
    // Mod Değiştiriciler
    fun kalem() { secimiKaldir(); suankiMod = Mod.KALEM; cizimYoneticisi.currentPaint.style = Paint.Style.STROKE }
    fun silme() { secimiKaldir(); suankiMod = Mod.SILGI; cizimYoneticisi.currentPaint.color = Color.WHITE; cizimYoneticisi.currentPaint.style = Paint.Style.STROKE }
    fun metinModu() { secimiKaldir(); suankiMod = Mod.METIN; cizimYoneticisi.currentPaint.style = Paint.Style.FILL }
    fun damlalikModu() { secimiKaldir(); suankiMod = Mod.DAMLALIK }
    fun sekilModu(t: ShapeType) { secimiKaldir(); suankiMod = Mod.SEKIL; cizimYoneticisi.currentShapeType = t; cizimYoneticisi.currentPaint.style = Paint.Style.STROKE }
    fun tasimaModu() { suankiMod = Mod.TASIMA }
    fun kovaModu() { secimiKaldir(); suankiMod = Mod.KOVA }
    // Aksiyonlar
    fun addText(t: String, x: Float, y: Float) {
        cizimYoneticisi.undoActions.clear()
        // Kalemin özelliklerini kopyala (Renk vb. için)
        val p = Paint(cizimYoneticisi.currentPaint)
        p.style = Paint.Style.FILL
        // --- DEĞİŞİKLİK BURADA ---
        // Yazı boyutunu fırça kalınlığından bağımsız hale getiriyoruz.
        // 80f, 100f gibi değerler vererek ne kadar büyük olacağını ayarlayabilirsin.
        p.textSize = 100f
        cizimYoneticisi.actions.add(DrawAction.TextAction(t, x, y, p))
        invalidate()
    }
    fun geriAl() { cizimYoneticisi.undo(); invalidate() }
    fun ileriAl() { cizimYoneticisi.redo(); invalidate() }
}