package com.nazli.pdkresimapp
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
class RenkSeciciDialog(private val context: Context) {
    fun goster(adapter: RenkAdapter, onRenkSecildi: (Int) -> Unit) {
        // Ana Düzen
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.WHITE)
        }
        // Renk Önizleme Kutusu
        val previewCard = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200).apply {
                setMargins(0, 0, 0, 40)
            }
            radius = 30f
            cardElevation = 0f
        }
        val previewView = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
        }
        previewCard.addView(previewView)
        layout.addView(previewCard)
        // Renk Çemberini Oluştur
        val wheelSize = 600
        val bitmap = Bitmap.createBitmap(wheelSize, wheelSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val centerX = wheelSize / 2f
        val centerY = wheelSize / 2f
        val radius = wheelSize / 2f
        // Gradyanlar
        val rainbowColors = intArrayOf(Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED)
        val sweepGradient = SweepGradient(centerX, centerY, rainbowColors, null)
        val saturationGradient = RadialGradient(centerX, centerY, radius, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        val paint = Paint().apply {
            shader = ComposeShader(sweepGradient, saturationGradient, PorterDuff.Mode.SRC_OVER)
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, paint)
        val wheelImage = ImageView(context).apply {
            setImageBitmap(bitmap)
            layoutParams = LinearLayout.LayoutParams(800, 800)
        }
        layout.addView(wheelImage)
        //Parlaklık Slider'ı
        val brightnessLabel = TextView(context).apply {
            text = "Parlaklık Ayarı"
            setPadding(0, 30, 0, 10)
        }
        layout.addView(brightnessLabel)
        val brightnessSlider = SeekBar(context).apply {
            max = 255
            progress = 255
        }
        layout.addView(brightnessSlider)
        // --- Mantık ---
        var secilenHamRenk = Color.WHITE
        var sonRenk = Color.WHITE
        fun rengiGuncelle() {
            val hsv = FloatArray(3)
            Color.colorToHSV(secilenHamRenk, hsv)
            hsv[2] = brightnessSlider.progress / 255f
            sonRenk = Color.HSVToColor(hsv)

            previewView.setBackgroundColor(sonRenk)
            brightnessSlider.thumb.setTint(sonRenk)
            brightnessSlider.progressDrawable.setTint(sonRenk)
        }
        wheelImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                val scaleX = bitmap.width.toFloat() / v.width
                val scaleY = bitmap.height.toFloat() / v.height
                val x = (event.x * scaleX).toInt()
                val y = (event.y * scaleY).toInt()

                val dx = x - centerX
                val dy = y - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())

                if (distance <= radius && x >= 0 && y >= 0 && x < bitmap.width && y < bitmap.height) {
                    secilenHamRenk = bitmap.getPixel(x, y)
                    rengiGuncelle()
                }
            }
            true
        }
        brightnessSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { rengiGuncelle() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        AlertDialog.Builder(context)
            .setTitle("Renk Seç ")
            .setView(layout)
            .setPositiveButton("SEÇ") { _, _ ->
                adapter.yeniRenkEkle(sonRenk)
                onRenkSecildi(sonRenk)
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
