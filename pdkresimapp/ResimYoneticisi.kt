package com.nazli.pdkresimapp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
object ResimYoneticisi {
    fun tuvalBosMu(bitmap: Bitmap): Boolean {
        val config = bitmap.config ?: Bitmap.Config.ARGB_8888
        val bosBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
        val canvas = Canvas(bosBitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap.sameAs(bosBitmap)
    }
    fun resmiKaydet(context: Context, bitmap: Bitmap, mevcutDosyaYolu: String?): Boolean {
        val dosya: File
        if (mevcutDosyaYolu != null) {
            dosya = File(mevcutDosyaYolu)
        } else {
            val dosyaIsmi = "Cizim_${System.currentTimeMillis()}.png"
            dosya = File(context.filesDir, dosyaIsmi)
        }
        return try {
            val stream = FileOutputStream(dosya)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            Toast.makeText(context, "Kaydedildi! ", Toast.LENGTH_SHORT).show()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Hata oluştu! ", Toast.LENGTH_SHORT).show()
            false
        }
    }
}