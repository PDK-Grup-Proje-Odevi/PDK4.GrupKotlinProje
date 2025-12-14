import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nazli.pdkresimapp.CizimModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nazli.pdkresimapp.R
import java.io.File

// Bu sınıf, listeyi alır ve RecyclerView'a "Al bunları göster" der.
class CizimAdapter(private val cizimListesi: List<CizimModel>,
                   private val onResimTiklandi: (String) -> Unit ,
                   private val onSilTiklandi: (CizimModel, Int) -> Unit,
                   private val onFavoriTiklandi: (CizimModel, Int) -> Unit,
                    private val onIndirTiklandi: (CizimModel) -> Unit):
    RecyclerView.Adapter<CizimAdapter.CizimHolder>() {

    // Görünümü Oluşturma (XML'i Java/Kotlin koduna çevirir)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CizimHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_design_card, parent, false) // Tasarladığımız Kart
        return CizimHolder(view)
    }
    // Veriyi Bağlama (Hangi resim hangi karta gidecek?)
    override fun onBindViewHolder(holder: CizimHolder, position: Int) {
        val suankiCizim = cizimListesi[position]

        if (suankiCizim.dosyaYolu != null) {
            val dosya = File(suankiCizim.dosyaYolu)
            if (dosya.exists()) {
                val bitmap = BitmapFactory.decodeFile(dosya.absolutePath)
                holder.imgDrawing.setImageBitmap(bitmap)

                // Karta tıklanınca bu dosya yolunu GaleriFragment'a gönder
                holder.itemView.setOnClickListener {
                    onResimTiklandi(suankiCizim.dosyaYolu)
                }
            }
        }
        // Hazır süs resmi (Drawable'dan al)
        else if (suankiCizim.resimId != null) {
            // Tasarımdaki ImageView'a resmi atıyoruz
            holder.imgDrawing.setImageResource(suankiCizim.resimId)
        }
        // Kalbin içi dolu mu boş mu olacak?
        if (suankiCizim.favoriMi) {
            holder.btnLike.setImageResource(R.drawable.heartfill) // Dolu kalp
        } else {
            holder.btnLike.setImageResource(R.drawable.heart)     // Boş kalp
        }
        // Kalbe tıklanınca
        holder.btnLike.setOnClickListener {
            onFavoriTiklandi(suankiCizim, position)
        }
        // Silmeye tiklaninca
        holder.btnDelete.setOnClickListener {
            onSilTiklandi(suankiCizim, position)
        }
        // Galeriye tiklaninca
        holder.btnDownload.setOnClickListener {
            onIndirTiklandi(suankiCizim)
        }
    }
    // LİSTE BOYUTU (Kaç tane kart var?)
    override fun getItemCount(): Int {
        return cizimListesi.size
    }
    // HOLDER: Tasarımdaki elemanları (ID'leri) tutan yardımcı sınıf
    class CizimHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // item_design_card.xml ID'leri
        val imgDrawing: ImageView = itemView.findViewById(R.id.imgDrawing)
        val btnLike: FloatingActionButton = itemView.findViewById(R.id.btnLike)
        val btnDelete: FloatingActionButton = itemView.findViewById(R.id.btnDelete)
        val btnDownload: FloatingActionButton = itemView.findViewById(R.id.btnDownload)
    }
}