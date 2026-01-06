package com.liam.scheduleu

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.liam.scheduleu.data.local.AppDatabase
import com.liam.scheduleu.data.local.ScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var tvHalo: TextView

    private lateinit var card1: CardView
    private lateinit var card2: CardView
    private lateinit var card3: CardView
    private lateinit var card4: CardView

    private lateinit var tvTitle1: TextView
    private lateinit var tvTitle2: TextView
    private lateinit var tvTitle3: TextView
    private lateinit var tvTitle4: TextView

    private lateinit var tvJam1: TextView
    private lateinit var tvJam2: TextView
    private lateinit var tvJam3: TextView
    private lateinit var tvJam4: TextView

    private lateinit var tvRuang1: TextView
    private lateinit var tvRuang2: TextView
    private lateinit var tvRuang3: TextView
    private lateinit var tvRuang4: TextView

    // cache untuk dialog detail
    private var jadwalHariIni: List<ScheduleEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvHalo = findViewById(R.id.tvHalo)

        // === TAMBAHAN: tombol settings ===
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        // === END TAMBAHAN ===

        // Ambil greeting dari Splash (fallback "Halo" kalau kosong)
        val greeting = intent.getStringExtra("EXTRA_GREETING") ?: "Halo"

        // Ambil nama user dari SharedPreferences
        val name = getUserName()

        tvHalo.text = if (name.isNotEmpty()) {
            "$greeting, $name"
        } else {
            greeting
        }

        // Kalau belum ada nama sama sekali, minta isi
        if (name.isEmpty()) {
            showNameInputDialog(greeting)
        } else {
            // (opsional) kasih hint ke user cara ganti nama
            // Toast.makeText(this, "Tap lama teks halo untuk ganti nama", Toast.LENGTH_SHORT).show()
        }

        // Biar user bisa ganti nama dengan tap lama pada sapaan
        tvHalo.setOnLongClickListener {
            showNameInputDialog(greeting)
            true
        }

        // inisialisasi view jadwal
        card1 = findViewById(R.id.cardMath)
        card2 = findViewById(R.id.cardPhysics)
        card3 = findViewById(R.id.cardEnglish)
        card4 = findViewById(R.id.cardEmpty)

        tvTitle1 = findViewById(R.id.tvTitle1)
        tvTitle2 = findViewById(R.id.tvTitle2)
        tvTitle3 = findViewById(R.id.tvTitle3)
        tvTitle4 = findViewById(R.id.tvTitle4)

        tvJam1 = findViewById(R.id.tvJam1)
        tvJam2 = findViewById(R.id.tvJam2)
        tvJam3 = findViewById(R.id.tvJam3)
        tvJam4 = findViewById(R.id.tvJam4)

        tvRuang1 = findViewById(R.id.tvRuang1)
        tvRuang2 = findViewById(R.id.tvRuang2)
        tvRuang3 = findViewById(R.id.tvRuang3)
        tvRuang4 = findViewById(R.id.tvRuang4)

        // klik card → buka dialog detail berdasarkan data Room
        setupCardClick(card1, 0)
        setupCardClick(card2, 1)
        setupCardClick(card3, 2)
        setupCardClick(card4, 3)

        // tombol bawah
        val btnJadwal = findViewById<MaterialButton>(R.id.btnJadwalAnda)
        val btnTambah = findViewById<MaterialButton>(R.id.btnTambahJadwal)

        btnJadwal.setOnClickListener {
            startActivity(Intent(this, JadwalListActivity::class.java))
        }

        btnTambah.setOnClickListener {
            startActivity(Intent(this, TambahJadwalActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadJadwalHariIni()
    }

    // =============== NAMA USER & SAPAAN ===============

    private fun saveUserName(name: String) {
        val prefs = getSharedPreferences("scheduleu_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", name)
            .apply()
    }

    private fun getUserName(): String {
        val prefs = getSharedPreferences("scheduleu_prefs", MODE_PRIVATE)
        return prefs.getString("user_name", "") ?: ""
    }

    private fun showNameInputDialog(greeting: String) {
        val editText = android.widget.EditText(this).apply {
            hint = "Nama kamu"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS

            val currentName = getUserName()
            if (currentName.isNotEmpty()) {
                setText(currentName)
                setSelection(currentName.length)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Isi nama kamu")
            .setView(editText)
            .setPositiveButton("Simpan") { dialog, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveUserName(name)
                    tvHalo.text = "$greeting, $name"
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // =============== JADWAL HARI INI ===============

    private fun loadJadwalHariIni() {
        val hari = getTodayName()

        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            val list: List<ScheduleEntity> = withContext(Dispatchers.IO) {
                dao.getSchedulesByHari(hari)
            }

            jadwalHariIni = list

            // isi 4 slot
            setSlot(0, list.getOrNull(0))
            setSlot(1, list.getOrNull(1))
            setSlot(2, list.getOrNull(2))
            setSlot(3, list.getOrNull(3))
        }
    }

    private fun setSlot(index: Int, item: ScheduleEntity?) {
        val titleView = when (index) {
            0 -> tvTitle1
            1 -> tvTitle2
            2 -> tvTitle3
            else -> tvTitle4
        }
        val jamView = when (index) {
            0 -> tvJam1
            1 -> tvJam2
            2 -> tvJam3
            else -> tvJam4
        }
        val ruangView = when (index) {
            0 -> tvRuang1
            1 -> tvRuang2
            2 -> tvRuang3
            else -> tvRuang4
        }

        if (item == null) {
            titleView.text = "Kosong"
            jamView.text = "-"
            ruangView.text = "-"
        } else {
            titleView.text = item.namaMatkul
            jamView.text = "${item.jamMulai} - ${item.jamSelesai}"
            ruangView.text = item.ruang
        }
    }

    private fun setupCardClick(card: CardView, index: Int) {
        card.setOnClickListener {
            val item = jadwalHariIni.getOrNull(index)
            if (item == null) {
                val dialog = DetailJadwalDialog.newInstance(
                    namaMatkul = "Kosong",
                    namaDosen = "-",
                    jam = "-",
                    ruang = "-",
                    motivasi = "Tidak ada jadwal, gunakan waktu luang dengan baik!"
                )
                dialog.show(supportFragmentManager, "detail_matkul")
            } else {
                val dialog = DetailJadwalDialog.newInstance(
                    namaMatkul = item.namaMatkul,
                    namaDosen = item.namaDosen,
                    jam = "${item.jamMulai} - ${item.jamSelesai}",
                    ruang = item.ruang
                )
                dialog.show(supportFragmentManager, "detail_matkul")
            }
        }
    }

    private fun getTodayName(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Minggu"
        }
    }
}
