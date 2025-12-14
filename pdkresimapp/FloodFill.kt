package com.nazli.pdkresimapp

import android.graphics.Bitmap
import android.graphics.Point
import java.util.Stack

object FloodFill {
    fun fill(sourceBitmap: Bitmap, startPoint: Point, targetColor: Int, newColor: Int): Bitmap? {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        // Hedef renk ile yeni renk aynıysa işlem yapma
        if (targetColor == newColor) return null
        // Pikselleri diziye al (Bitmap üzerinde işlem yapmaktan çok daha hızlıdır)
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val startIdx = startPoint.y * width + startPoint.x
        // Güvenlik kontrolü
        if (startIdx < 0 || startIdx >= pixels.size || pixels[startIdx] != targetColor) return null
        // Sonuç dizisi (Başlangıçta şeffaf)
        val resultPixels = IntArray(width * height) { 0 }
        // --- SCANLINE ALGORİTMASI (HIZLI BOYAMA) ---
        val stack = Stack<Point>()
        stack.push(startPoint)
        while (stack.isNotEmpty()) {
            val p = stack.pop()
            var x = p.x
            val y = p.y
            // Dizi indeksi hesapla
            var idx = y * width + x
            // Yukarı ve aşağı satırları kontrol etmek gerekip gerekmediği
            var spanAbove = false
            var spanBelow = false
            // Mevcut satırda SOLA doğru git
            while (x >= 0 && pixels[idx] == targetColor && resultPixels[idx] != newColor) {
                x--
                idx--
            }
            // Döngüden çıkınca bir adım geri gel (sınıra çarptık)
            x++
            idx++
            // Şimdi SAĞA doğru boyayarak git
            while (x < width && pixels[idx] == targetColor && resultPixels[idx] != newColor) {
                // Rengi boya
                resultPixels[idx] = newColor
                // Üst satırı kontrol et
                if (y > 0) {
                    val upIdx = idx - width
                    val upPixelMatch = (pixels[upIdx] == targetColor && resultPixels[upIdx] != newColor)
                    if (!spanAbove && upPixelMatch) {
                        stack.push(Point(x, y - 1))
                        spanAbove = true
                    } else if (spanAbove && !upPixelMatch) {
                        spanAbove = false
                    }
                }
                // Alt satırı kontrol et
                if (y < height - 1) {
                    val downIdx = idx + width
                    val downPixelMatch = (pixels[downIdx] == targetColor && resultPixels[downIdx] != newColor)

                    if (!spanBelow && downPixelMatch) {
                        stack.push(Point(x, y + 1))
                        spanBelow = true
                    } else if (spanBelow && !downPixelMatch) {
                        spanBelow = false
                    }
                }
                x++
                idx++
            }
        }
        return Bitmap.createBitmap(resultPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}