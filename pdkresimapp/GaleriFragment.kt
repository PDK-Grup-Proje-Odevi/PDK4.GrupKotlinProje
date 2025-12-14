package com.nazli.pdkresimapp

import CizimAdapter
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.nazli.pdkresimapp.FileUtils.kaydet
import com.nazli.pdkresimapp.databinding.FragmentGaleriBinding
import java.io.File
import kotlin.collections.mutableListOf

class GaleriFragment : Fragment() {
    private var _binding: FragmentGaleriBinding?= null
    private val binding get()=_binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    // Favorileri hafızada tutmak için anahtar kelime
    private val PREFS_NAME = "FavoriAyarlar"
    private val FAV_KEY = "favori_listesi"
    val tumResimler = mutableListOf<CizimModel>()
    private lateinit var adapter: CizimAdapter
    private var isEnYeni = true // Varsayılan sıralama
    private var isSadeceFavoriler = false // Varsayılan filtre
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding=FragmentGaleriBinding.inflate(inflater,container,false)
        val view=binding.root
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ------ÖZEL HAFIZAYI TAR-------
        // Uygulamanın kaydettiği dosyaları bul
        val klasor = requireContext().filesDir
        val dosyalar = klasor.listFiles()
        // Favorileri Hafızadan Çek
        val sharedPref = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favoriSeti = sharedPref.getStringSet(FAV_KEY, mutableSetOf()) ?: mutableSetOf()
        // Dosyaları Tara ve Listeyi Doldur
        if (dosyalar != null) {
            dosyalar.sortByDescending { it.lastModified() }
            for (dosya in dosyalar) {
                if (dosya.name.startsWith("Cizim_") && dosya.name.endsWith(".png")) {
                    // Dosya yolu favori setinin içinde var mı? Varsa true, yoksa false
                    val isFav = favoriSeti.contains(dosya.absolutePath)
                    tumResimler.add(CizimModel(dosyaYolu = dosya.absolutePath, favoriMi = isFav))
                }
            }
        }

        adapter = CizimAdapter(
            tumResimler,
            // Karta Tıklanınca
            onResimTiklandi = { tiklananDosyaYolu ->
                val bundle = Bundle()
                bundle.putString("gelenDosyaYolu", tiklananDosyaYolu)
                findNavController().navigate(R.id.action_galeriFragment_to_cizimFragment, bundle)
            },
            // Silme(Resmi)
            onSilTiklandi = { model, position ->
                silmeIsleminiYap(model, position, tumResimler)
            },

            // Kalbe tiklaninca
            onFavoriTiklandi = { model, position ->
                favoriIsleminiYap(model, position)
            },
            onIndirTiklandi = { model ->
                Toast.makeText(context, "İndirildi!", Toast.LENGTH_SHORT).show()
                indirmeIslemi(model)
            }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(context, 1)
        binding.recyclerView.adapter = adapter
        binding.resimekle.setOnClickListener{
            resimCiz(it)
        }
        binding.buttonEneski.setOnClickListener {
            binding.buttonEneski.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.StarYellow))
            binding.buttonEnyeni.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            binding.buttonFavori.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            isEnYeni = false
            isSadeceFavoriler = false // Filtreyi kapat, tümünü göster
            resimleriListele()
        }
        binding.buttonEnyeni.setOnClickListener {
            binding.buttonEnyeni.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.StarYellow))
            binding.buttonEneski.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            binding.buttonFavori.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            isEnYeni = true
            isSadeceFavoriler = false
            resimleriListele()
        }
        binding.buttonFavori.setOnClickListener {
            binding.buttonFavori.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.StarYellow))
            binding.buttonEneski.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            binding.buttonEnyeni.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            isSadeceFavoriler = true
            resimleriListele()
        }
        binding.btnGeri.setOnClickListener {
            basadon(it)
        }
    }
    fun basadon(view: View)
    {
        val action=GaleriFragmentDirections.actionGaleriFragmentToGirisFragment()
        Navigation.findNavController(view).navigate(action)
    }
    fun resimCiz (view: View)
    {
        val action=GaleriFragmentDirections.actionGaleriFragmentToCizimFragment()
        Navigation.findNavController(view).navigate(action)
    }
    private fun resimleriListele() {
        // Listeyi temizle (Eski veriler gitsin)
        tumResimler.clear()
        // Favorileri Hafızadan Çek
        val sharedPref = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val favoriSeti = sharedPref.getStringSet(FAV_KEY, mutableSetOf()) ?: mutableSetOf()
        // Dosyaları Tara
        val klasor = requireContext().filesDir
        val dosyalar = klasor.listFiles()
        if (dosyalar != null) {
            if (isEnYeni) {
                // En Yeni -> En Eski (Azalan)
                dosyalar.sortByDescending { it.lastModified() }
            } else {
                // En Eski -> En Yeni (Artan - TERSİ)
                dosyalar.sortBy { it.lastModified() }
            }
            for (dosya in dosyalar) {
                if (dosya.name.startsWith("Cizim_") && dosya.name.endsWith(".png")) {
                    val isFav = favoriSeti.contains(dosya.absolutePath)
                    if (isSadeceFavoriler) {
                        // Eğer "Sadece Favoriler" modu açıksa, SADECE favori olanları ekle
                        if (isFav) {
                            tumResimler.add(CizimModel(dosyaYolu = dosya.absolutePath, favoriMi = true))
                        }
                    } else {
                        // Mod kapalıysa HEPSİNİ ekle
                        tumResimler.add(CizimModel(dosyaYolu = dosya.absolutePath, favoriMi = isFav))
                    }
                }
            }
        }

        // Adapter'a haber ver: "Veriler değişti, ekranı yenile!"
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
    private fun silmeIsleminiYap(model: CizimModel, position: Int, liste: MutableList<CizimModel>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Resmi Sil")
            .setMessage("Bu çizimi kalıcı olarak silmek istiyor musun?")
            .setPositiveButton("Evet, Sil") { _, _ ->
                if (model.dosyaYolu != null) {
                    val dosya = File(model.dosyaYolu)
                    if (dosya.exists()) {
                        dosya.delete() // Dosyayı telefondan sil
                        liste.removeAt(position) // Listeden sil
                        binding.recyclerView.adapter?.notifyItemRemoved(position) // Ekrandan sil
                        binding.recyclerView.adapter?.notifyItemRangeChanged(position, liste.size)
                        Toast.makeText(context, "Silindi!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }
    // YARDIMCI FONKSİYON: FAVORİLEME
    private fun favoriIsleminiYap(model: CizimModel, position: Int) {
        // Durumu tersine çevir (True ise False, False ise True yap)
        model.favoriMi = !model.favoriMi
        // Listeyi güncelle ki kalp ikonu değişsin
        binding.recyclerView.adapter?.notifyItemChanged(position)
        // Hafızayı (SharedPreferences) Güncelle
        val sharedPref = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mevcutSet = sharedPref.getStringSet(FAV_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (model.dosyaYolu != null) {
            if (model.favoriMi) {
                mevcutSet.add(model.dosyaYolu) // Listeye ekle
                Toast.makeText(context, "Favorilendi ", Toast.LENGTH_SHORT).show()
            } else {
                mevcutSet.remove(model.dosyaYolu) // Listeden çıkar
            }
            // Kaydet
            sharedPref.edit().putStringSet(FAV_KEY, mevcutSet).apply()
        }
    }

    private fun indirmeIslemi(model: CizimModel)
    {
        if (model.dosyaYolu != null) {
            val dosya = File(model.dosyaYolu)
            if (dosya.exists()) {
                // Dosyayı Bitmap'e çevir
                val bitmap = BitmapFactory.decodeFile(dosya.absolutePath)
                // Galeriye kaydet
                kaydet(requireContext(), bitmap)
            } else {
                Toast.makeText(context, "Hata: Dosya bulunamadı.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bu resim indirilemez.", Toast.LENGTH_SHORT).show()
        }
    }
}