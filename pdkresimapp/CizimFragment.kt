package com.nazli.pdkresimapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nazli.pdkresimapp.databinding.FragmentCizimBinding
import java.io.File
import android.view.MotionEvent
import android.content.Context
import android.view.inputmethod.InputMethodManager

class CizimFragment : Fragment() {
    private var _binding: FragmentCizimBinding? = null
    private val binding get() = _binding!!
    private var duzenlenenDosyaYolu: String? = null
    private lateinit var openGalleryLauncher: ActivityResultLauncher<Intent>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCizimBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGalleryLauncher()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRenkPaleti()
        checkGelenDosya()
        setupSlider()
        setupButtons()
        setupDrawingViewListeners()
    }
    // --- KURULUMLAR ---
    private fun initGalleryLauncher() {
        openGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, imageUri))
                        } else {
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                        }
                        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        binding.drawingView.resmiYukle(mutableBitmap)
                        Toast.makeText(context, "Resim yüklendi!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    private fun setupRenkPaleti() {
        val renkler = RenkData.renkListesi
        val renkAdapter = RenkAdapter(
            ArrayList(renkler),
            onRenkSecildi = { secilenRenk ->
                binding.drawingView.setPenColor(secilenRenk)
                if (binding.btnEraser.imageTintList == ColorStateList.valueOf(Color.BLACK)) {
                    guncelleButonRenkleri(binding.btnBrush)
                    binding.drawingView.kalem()
                }
            },
            onRenkSil = { silinecekIndex ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Rengi Sil")
                    .setMessage("Silmek istiyor musun?")
                    .setPositiveButton("Sil") { _, _ ->
                        (binding.colorPalette.adapter as? RenkAdapter)?.renkSil(silinecekIndex)
                    }
                    .setNegativeButton("İptal", null).show()
            }
        )
        // Telefonun geri tuşu
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Geri tuşuna basılınca ne olsun?
                cikisYap()
            }
        })
        binding.colorPalette.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.colorPalette.adapter = renkAdapter
        // SÜRÜKLE VE BIRAK ÖZELLİĞİ (DRAG & DROP)
        // Renk paleti
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT, // Sadece Yatay Sürükleme
            0 // Kaydırma (Swipe) kapalı
        ) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean {
                val baslangic = viewHolder.adapterPosition
                val bitis = target.adapterPosition

                // Adapter'daki taşıma fonksiyonunu çağırır
                renkAdapter.renkTasi(baslangic, bitis)
                return true
            }
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                // Sağa sola kaydırınca silme özelliği kapalı
            }
        })
        // Özelliği RecyclerView'a bağla
        itemTouchHelper.attachToRecyclerView(binding.colorPalette)
        // Renk Seçici Butonu
        binding.btncolorpicker.setOnLongClickListener {
            RenkSeciciDialog(requireContext()).goster(renkAdapter) { secilenRenk ->
                binding.colorPalette.scrollToPosition(0)
                binding.drawingView.setPenColor(secilenRenk)
                guncelleButonRenkleri(binding.btnBrush)
            }
            true
        }
    }
    //Resim yükleme fonksiyonu
    private fun checkGelenDosya() {
        duzenlenenDosyaYolu = arguments?.getString("gelenDosyaYolu")
        if (duzenlenenDosyaYolu != null) {
            val dosya = File(duzenlenenDosyaYolu)
            if (dosya.exists()) {
                val bitmap = BitmapFactory.decodeFile(dosya.absolutePath).copy(Bitmap.Config.ARGB_8888, true)
                binding.drawingView.resmiYukle(bitmap)
            }
        }
    }
    private fun setupDrawingViewListeners() {
        // Tuvale dokunma (Metin ekleme)
        binding.drawingView.onCanvasClick = { x, y -> showTextDialog(x, y) }
        // Damlalık (Renk seçme)
        binding.drawingView.onColorPicked = { color ->
            guncelleButonRenkleri(binding.btnBrush)
            (binding.colorPalette.adapter as? RenkAdapter)?.yeniRenkEkle(color)
            binding.colorPalette.scrollToPosition(0)
            Toast.makeText(context, "Renk Eklendi! 🎨", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setupButtons() {
        // Damlalık
        binding.btncolorpicker.setOnClickListener {
            binding.drawingView.damlalikModu()
            // Ama genelde damlalık tek seferliktir.
            Toast.makeText(context, "Renk Seçici (Damlalık) ", Toast.LENGTH_SHORT).show()
        }
        // Silgi tıklanınca
        binding.btnEraser.setOnClickListener {
            guncelleButonRenkleri(binding.btnEraser)
            binding.sliderContainer.visibility = View.GONE
            binding.drawingView.silme()
        }
        // Silgi uzun basılması
        binding.btnEraser.setOnLongClickListener {
            guncelleButonRenkleri(binding.btnEraser)
            binding.drawingView.silme()
            toggleSliderVisibility(isBrush = false)
            true
        }
        // Fırça tıklanınca
        binding.btnBrush.setOnClickListener {
            guncelleButonRenkleri(binding.btnBrush)
            toggleSliderVisibility(isBrush = true) // Fırçada slider açık kalsın/açılsın
            binding.drawingView.kalem()
        }
        //Fırçaya uzun basılması
        binding.btnBrush.setOnLongClickListener {
            guncelleButonRenkleri(binding.btnBrush)
            binding.drawingView.kalem()
            binding.colorPalette.visibility = View.VISIBLE
            toggleSliderVisibility(isBrush = true)
            true
        }
        // Metin
        binding.btnText.setOnClickListener {
            guncelleButonRenkleri(binding.btnText)
            binding.drawingView.metinModu()
            binding.colorPalette.visibility = View.VISIBLE
            binding.sliderContainer.visibility = View.GONE
            Toast.makeText(context, "Yazmak için dokun", Toast.LENGTH_SHORT).show()
        }
        // Boya kovası
        binding.btnboya.setOnClickListener {
            guncelleButonRenkleri(binding.btnboya)
            binding.sliderContainer.visibility = View.GONE
            binding.drawingView.kovaModu()

            Toast.makeText(requireContext(), "Boya Kovası Seçildi", Toast.LENGTH_SHORT).show()
        }
        // Resim Ekle (Galeri)
        binding.btnResimEkle.setOnClickListener {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(pickIntent)
        }
        // Şekiller Menüsü
        binding.btnShapes.setOnClickListener {
            guncelleButonRenkleri(binding.btnShapes)
            val popup = PopupMenu(requireContext(), binding.btnShapes)
            popup.menu.add("Kare")
            popup.menu.add("Daire")
            popup.menu.add("Çizgi")
            popup.setOnMenuItemClickListener { item ->
                binding.colorPalette.visibility = View.VISIBLE
                binding.sliderContainer.visibility = View.GONE

                when(item.title) {
                    "Kare" -> binding.drawingView.sekilModu(ShapeType.RECTANGLE)
                    "Daire" -> binding.drawingView.sekilModu(ShapeType.OVAL)
                    "Çizgi" -> binding.drawingView.sekilModu(ShapeType.LINE)
                }
                true
            }
            popup.show()
        }
        binding.btnShapes.setOnLongClickListener {
            // Şekillerdeyken uzun basınca fırça ayarı yerine şekil kalınlığı ayarı
            toggleSliderVisibility(isBrush = true)
            true
        }
        // Geri Dön, Kaydet, Undo, Redo işlemleri...
        binding.btnBack.setOnClickListener {
            val bitmap = binding.drawingView.getBitmap()
            if(ResimYoneticisi.tuvalBosMu(bitmap)) {
                Toast.makeText(requireContext(), "Tuval boş!", Toast.LENGTH_SHORT).show()
            } else {
                ResimYoneticisi.resmiKaydet(requireContext(), bitmap, duzenlenenDosyaYolu)
            }
            gerigaleri(it)
        }
        binding.btnUndo.setOnClickListener { binding.drawingView.geriAl() }
        binding.btnRedo.setOnClickListener { binding.drawingView.ileriAl() }
        binding.btnSave.setOnClickListener {
            val bitmap = binding.drawingView.getBitmap()
            // Önce kontrol et: Tuval boş mu?
            if (ResimYoneticisi.tuvalBosMu(bitmap)) {
                // Boşsa uyarı ver ve kaydetme
                Toast.makeText(requireContext(), "Tuval boş! İndirlecek bir şey yok.", Toast.LENGTH_SHORT).show()
            } else {
                // Doluysa kaydet
                FileUtils.kaydet(requireContext(), bitmap)
            }
        }
    }
    // --- YARDIMCILAR ---
    private fun showTextDialog(x: Float, y: Float) {
        // Context null ise işlem yapma (Çökmesini engeller)
        context?.let { ctx ->
            val input = EditText(ctx)
            input.hint = "Metin giriniz..."
            input.textSize = 25f
            input.setTextColor(Color.BLACK)
            // Kenar boşlukları ekleyelim ki yazı kutuya yapışmasın
            val container = android.widget.FrameLayout(ctx)
            val params = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = 40; params.rightMargin = 40
            input.layoutParams = params
            container.addView(input)
            val dialog = AlertDialog.Builder(ctx)
                .setTitle("Metin Ekle")
                .setView(container) // Input'u değil, container'ı ekler
                .setPositiveButton("Tamam") { _, _ ->
                    if (input.text.isNotEmpty()) {
                        binding.drawingView.addText(input.text.toString(), x, y)
                    }
                }
                .setNegativeButton("İptal", null)
                .create()
            // Klavyeyi açma kodu
            dialog.setOnShowListener {
                input.requestFocus()
                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
            dialog.show()
        }
    }
    private fun setupSlider() {
        binding.btnCloseSlider.setOnClickListener { binding.sliderContainer.visibility = View.GONE }
        binding.sliderTouchArea.setOnTouchListener { _, event ->
            val trackHeight = binding.sliderTrack.height
            val trackTop = binding.sliderTrack.top
            when (event.action) {
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_DOWN -> {
                    var touchY = event.y
                    if (touchY < trackTop) touchY = trackTop.toFloat()
                    if (touchY > trackTop + trackHeight) touchY = (trackTop + trackHeight).toFloat()
                    val percentage = 1.0f - ((touchY - trackTop) / trackHeight)
                    val params = binding.sliderThumb.layoutParams as ConstraintLayout.LayoutParams
                    params.verticalBias = (touchY - trackTop) / trackHeight
                    binding.sliderThumb.layoutParams = params
                    val newSize = 5f + (percentage * 195f)
                    binding.drawingView.setBrushSize(newSize)
                    binding.txtSliderTitle.text = String.format("%.0f", newSize)
                }
            }
            true
        }
    }
    private fun toggleSliderVisibility(isBrush: Boolean) {
        if (binding.sliderContainer.visibility == View.VISIBLE) {
            binding.sliderContainer.visibility = View.GONE
        } else {
            binding.sliderContainer.visibility = View.VISIBLE
            binding.txtSliderTitle.text = if (isBrush) "FIRÇA" else "SİLGİ"
            val currentSize = binding.drawingView.getCurrentSize()
            val percentage = (currentSize - 5f) / 95f
            val params = binding.sliderThumb.layoutParams as ConstraintLayout.LayoutParams
            params.verticalBias = 1.0f - percentage
            binding.sliderThumb.layoutParams = params
        }
    }
    private fun guncelleButonRenkleri(aktifButon: View) {
        // Tüm butonları pasif (BEYAZ) yapar
        binding.btnEraser.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnBrush.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnText.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnShapes.imageTintList = ColorStateList.valueOf(Color.WHITE)
        binding.btnboya.imageTintList = ColorStateList.valueOf(Color.WHITE)
        // Sadece tıklananı aktif (SİYAH) yapar
        if (aktifButon is FloatingActionButton) {
            aktifButon.imageTintList = ColorStateList.valueOf(Color.BLACK)
        }
    }
    //Galeriye yonlendirme
    fun gerigaleri (view: View) {
        val action = CizimFragmentDirections.actionCizimFragmentToGaleriFragment()
        Navigation.findNavController(view).navigate(action)
    }
    private fun cikisYap() {
        // Resmi al
        val bitmap = binding.drawingView.getBitmap()

        // Tuval boş mu kontrol et
        if (ResimYoneticisi.tuvalBosMu(bitmap)) {
            //Boşsa kaydetme ama yine de çıkış yap
            Toast.makeText(requireContext(), "Tuval boş, kaydedilmedi.", Toast.LENGTH_SHORT).show()
        } else {
            // Doluysa kaydet
            ResimYoneticisi.resmiKaydet(requireContext(), bitmap, duzenlenenDosyaYolu)
            Toast.makeText(requireContext(), "Otomatik Kaydedildi", Toast.LENGTH_SHORT).show()
        }
        // 3. Galeriye Dön (Navigation)
        findNavController().popBackStack()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}