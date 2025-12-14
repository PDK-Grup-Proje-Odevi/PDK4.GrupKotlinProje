package com.nazli.pdkresimapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.nazli.pdkresimapp.databinding.FragmentGirisBinding

class GirisFragment : Fragment() {
    // --- ViewBinding Tanımlamaları ---
    // _binding: Arka planda tutulan, değeri null olabilen değişken.
    private var _binding: FragmentGirisBinding? = null
    // binding: Kodun içinde güvenle kullandığımız, null olmayan değişken.
    // get() metodu ile _binding'in içindeki değeri alıyoruz.
    // '!!' işareti "Merak etme, bu noktada bu değer boş değil" demektir.
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragment ilk oluşturulduğunda burası çalışır.
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // XML tasarım dosyasını (layout) koda bağlıyoruz (Inflate işlemi).
        _binding = FragmentGirisBinding.inflate(inflater, container, false)
        // Tasarımın kökünü (root) alıp ekrana çizilmesi için döndürüyoruz.
        val view = binding.root
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Görünümler tamamen oluştuktan sonra yapılacak işlemler burada yazılır.
        binding.buttonGiris.setOnClickListener {
            // Tıklanınca aşağıdaki navigasyon fonksiyonunu çağırıyoruz.
            galeriyegit(it)
        }
    }
    // Galeri sayfasına geçişi sağlayan fonksiyon
    fun galeriyegit(view: View) {
        // Navigation Component ve Safe Args kullanarak geçiş aksiyonunu tanımlıyoruz.
        // "GirisFragmentDirections" sınıfı, build.gradle'a eklediğimiz eklenti sayesinde otomatik oluşur.
        val action = GirisFragmentDirections.actionGirisFragmentToGaleriFragment()

        // NavController'ı (Yönlendirici) bulup, oluşturduğumuz aksiyonu başlatıyoruz.
        Navigation.findNavController(view).navigate(action)
    }
    // --- ÖNEMLİ:* Hafıza Temizliği ---
    // Fragment yok edildiğinde binding nesnesini de boşaltmalıyız.
    // Yoksa "Memory Leak" (Hafıza Sızıntısı) oluşabilir ve uygulama yavaşlayabilir.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}