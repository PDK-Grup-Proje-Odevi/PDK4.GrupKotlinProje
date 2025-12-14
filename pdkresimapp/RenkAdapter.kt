package com.nazli.pdkresimapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.nazli.pdkresimapp.R
import java.util.Collections

class RenkAdapter(
    private val renkListesi: ArrayList<Int>,
    private val onRenkSecildi: (Int) -> Unit,
    private val onRenkSil: (Int) -> Unit
) : RecyclerView.Adapter<RenkAdapter.RenkHolder>() {
    private var seciliPozisyon = 0
    private var sonTiklamaZamani: Long = 0 // Çift tıklama takibi için
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RenkHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_renk_kart, parent, false)
        return RenkHolder(view)
    }
    override fun onBindViewHolder(holder: RenkHolder, position: Int) {
        val renk = renkListesi[position]
        holder.imgKalp.setColorFilter(renk)
        // Seçili efekt
        if (position == seciliPozisyon) {
            holder.itemView.scaleX = 1.2f
            holder.itemView.scaleY = 1.2f
        } else {
            holder.itemView.scaleX = 1.0f
            holder.itemView.scaleY = 1.0f
        }
        // --- TIKLAMA MANTIĞI (Tek Tık: Seç, Çift Tık: Sil) ---
        holder.itemView.setOnClickListener {
            val suankiZaman = System.currentTimeMillis()
            // Eğer son tıklamadan bu yana 300ms'den az geçtiyse -> ÇİFT TIKLAMA
            if (suankiZaman - sonTiklamaZamani < 300) {
                // Silme işlemini tetikle
                onRenkSil(holder.adapterPosition)
            } else {
                // Değilse -> NORMAL SEÇİM
                val eskiPozisyon = seciliPozisyon
                seciliPozisyon = holder.adapterPosition
                notifyItemChanged(eskiPozisyon)
                notifyItemChanged(seciliPozisyon)
                onRenkSecildi(renk)
            }
            sonTiklamaZamani = suankiZaman
        }
    }
    // --- Sürükle Bırak Fonksiyonu ---
    fun renkTasi(baslangic: Int, bitis: Int) {
        if (baslangic < renkListesi.size && bitis < renkListesi.size) {
            Collections.swap(renkListesi, baslangic, bitis)
            notifyItemMoved(baslangic, bitis)
            // Seçimi de taşı
            if (seciliPozisyon == baslangic) seciliPozisyon = bitis
            else if (seciliPozisyon == bitis) seciliPozisyon = baslangic
        }
    }
    fun yeniRenkEkle(yeniRenk: Int) {
        if (renkListesi.isNotEmpty() && renkListesi[0] == yeniRenk) return
        renkListesi.add(0, yeniRenk)
        val eskiSecili = seciliPozisyon
        seciliPozisyon = 0
        notifyItemInserted(0)
        notifyItemChanged(0)
        if (eskiSecili + 1 < renkListesi.size) notifyItemChanged(eskiSecili + 1)
    }
    fun renkSil(pozisyon: Int) {
        if (pozisyon >= 0 && pozisyon < renkListesi.size) {
            renkListesi.removeAt(pozisyon)
            notifyItemRemoved(pozisyon)
            if (seciliPozisyon == pozisyon) {
                seciliPozisyon = 0
                if (renkListesi.isNotEmpty()) notifyItemChanged(0)
            } else if (seciliPozisyon > pozisyon) {
                seciliPozisyon--
            }
        }
    }
    override fun getItemCount(): Int = renkListesi.size
    class RenkHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgKalp: ImageView = itemView.findViewById(R.id.imgRenkKalp)
        val cardView: CardView = itemView.findViewById(R.id.cardRenk)
    }
}