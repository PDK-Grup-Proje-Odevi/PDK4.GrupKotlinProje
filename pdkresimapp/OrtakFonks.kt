package com.nazli.pdkresimapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

// Bu fonksiyonu "object" içine koyuyoruz ki her yerden FileUtils.resmiGaleriyeKaydet(...) diye çağırabilelim.
object FileUtils {
    fun kaydet(context: Context, bitmap: Bitmap) {
        val dosyaIsmi = "PDK_Art_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, dosyaIsmi)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PDKCizimler")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (bitmap.width > 0 && bitmap.height > 0) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "Galeriye Kaydedildi! ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Hata: Dosya oluşturulamadı.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Kaydetme hatası!", Toast.LENGTH_SHORT).show()
        }
    }
}