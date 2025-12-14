package com.nazli.pdkresimapp

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path

// --- ENUMLAR ---
enum class Mod {
    KALEM, SILGI, METIN, SEKIL, DAMLALIK, TASIMA, KOVA //
}
enum class ShapeType {
    RECTANGLE, OVAL, LINE
}
// Eylem sınıfları
sealed class DrawAction {
    data class PathAction(
        val path: Path,
        val paint: Paint
    ) : DrawAction()
    data class TextAction(
        val text: String,
        var x: Float,
        var y: Float,
        val paint: Paint
    ) : DrawAction()
    data class ShapeAction(
        val type: ShapeType,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val paint: Paint
    ) : DrawAction()
    data class BitmapAction(val bitmap: Bitmap) : DrawAction()
}