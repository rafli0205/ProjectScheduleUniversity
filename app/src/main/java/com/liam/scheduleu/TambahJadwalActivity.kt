package com.liam.scheduleu

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.liam.scheduleu.data.local.AppDatabase
import com.liam.scheduleu.data.local.ScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// penting: pastikan AlarmHelper.kt ada di package yang sama
import com.liam.scheduleu.scheduleAlarmForJadwal

class TambahJadwalActivity : AppCompatActivity() {

    private var jumlahJadwal = 1

    // id jadwal per slot, null = jadwal baru
    private val slotIds: Array<Long?> = arrayOf(null, null, null, null)

    // untuk picker hari
    private lateinit var cardPilihHari: CardView
    private lateinit var tvPilihHari: TextView
    private var selectedHari: String = "Senin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jadwal)

        // Tombol back (TextView)
        val btnBack = findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Picker hari
        cardPilihHari = findViewById(R.id.cardPilihHari)
        tvPilihHari = findViewById(R.id.tvPilihHari)
        tvPilihHari.text = selectedHari

        cardPilihHari.setOnClickListener {
            showHariDialog()
        }

        // Card jadwal tambahan
        val cardJadwal2 = findViewById<CardView>(R.id.cardJadwal2)
        val cardJadwal3 = findViewById<CardView>(R.id.cardJadwal3)
        val cardJadwal4 = findViewById<CardView>(R.id.cardJadwal4)

        // Tombol Tambah Jadwal
        val btnTambahJadwal = findViewById<MaterialButton>(R.id.btnTambahJadwal)
        btnTambahJadwal.setOnClickListener {
            when (jumlahJadwal) {
                1 -> {
                    cardJadwal2.visibility = View.VISIBLE
                    jumlahJadwal = 2
                }
                2 -> {
                    cardJadwal3.visibility = View.VISIBLE
                    jumlahJadwal = 3
                }
                3 -> {
                    cardJadwal4.visibility = View.VISIBLE
                    jumlahJadwal = 4
                }
                else -> {
                    Toast.makeText(this, "Maksimal 4 jadwal per hari", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Expand/collapse per jadwal
        setupExpandable(
            header = findViewById(R.id.headerJadwal1),
            icon = findViewById(R.id.iconJadwal1),
            detail = findViewById(R.id.detailJadwal1),
            startExpanded = true
        )
        setupExpandable(
            header = findViewById(R.id.headerJadwal2),
            icon = findViewById(R.id.iconJadwal2),
            detail = findViewById(R.id.detailJadwal2),
            startExpanded = false
        )
        setupExpandable(
            header = findViewById(R.id.headerJadwal3),
            icon = findViewById(R.id.iconJadwal3),
            detail = findViewById(R.id.detailJadwal3),
            startExpanded = false
        )
        setupExpandable(
            header = findViewById(R.id.headerJadwal4),
            icon = findViewById(R.id.iconJadwal4),
            detail = findViewById(R.id.detailJadwal4),
            startExpanded = false
        )

        // TimePicker untuk semua jam
        initTimePicker(findViewById(R.id.etJamMulai1))
        initTimePicker(findViewById(R.id.etJamSelesai1))
        initTimePicker(findViewById(R.id.etJamMulai2))
        initTimePicker(findViewById(R.id.etJamSelesai2))
        initTimePicker(findViewById(R.id.etJamMulai3))
        initTimePicker(findViewById(R.id.etJamSelesai3))
        initTimePicker(findViewById(R.id.etJamMulai4))
        initTimePicker(findViewById(R.id.etJamSelesai4))

        // Tombol simpan → simpan ke Room
        val btnSimpan = findViewById<MaterialButton>(R.id.btnSimpan)
        btnSimpan.setOnClickListener {
            if (!validateMinimal()) {
                Toast.makeText(this, "Isi minimal Jadwal 1 dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            simpanKeDatabase()
        }

        // Prefill hari default (Senin) saat pertama kali buka
        prefillJadwalForHari(selectedHari)
    }

    // ================= PREFILL DARI ROOM ==================

    private fun prefillJadwalForHari(hari: String) {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                dao.getSchedulesByHari(hari)
            }

            // reset id slot
            for (i in 0..3) slotIds[i] = null

            // atur card yang kelihatan
            jumlahJadwal = list.size.coerceIn(1, 4)
            findViewById<CardView>(R.id.cardJadwal2).visibility =
                if (jumlahJadwal >= 2) View.VISIBLE else View.GONE
            findViewById<CardView>(R.id.cardJadwal3).visibility =
                if (jumlahJadwal >= 3) View.VISIBLE else View.GONE
            findViewById<CardView>(R.id.cardJadwal4).visibility =
                if (jumlahJadwal >= 4) View.VISIBLE else View.GONE

            fun setSlot(
                index: Int,
                matkulId: Int,
                dosenId: Int,
                mulaiId: Int,
                selesaiId: Int,
                ruangId: Int
            ) {
                val item = list.getOrNull(index)
                val etMatkul = findViewById<TextInputEditText>(matkulId)
                val etDosen = findViewById<TextInputEditText>(dosenId)
                val etMulai = findViewById<TextInputEditText>(mulaiId)
                val etSelesai = findViewById<TextInputEditText>(selesaiId)
                val etRuang = findViewById<TextInputEditText>(ruangId)

                if (item == null) {
                    etMatkul.setText("")
                    etDosen.setText("")
                    etMulai.setText("")
                    etSelesai.setText("")
                    etRuang.setText("")
                    slotIds[index] = null
                } else {
                    etMatkul.setText(item.namaMatkul)
                    etDosen.setText(item.namaDosen)
                    etMulai.setText(item.jamMulai)
                    etSelesai.setText(item.jamSelesai)
                    etRuang.setText(item.ruang)
                    slotIds[index] = item.id
                }
            }

            setSlot(0, R.id.etMatkul1, R.id.etDosen1, R.id.etJamMulai1, R.id.etJamSelesai1, R.id.etRuang1)
            setSlot(1, R.id.etMatkul2, R.id.etDosen2, R.id.etJamMulai2, R.id.etJamSelesai2, R.id.etRuang2)
            setSlot(2, R.id.etMatkul3, R.id.etDosen3, R.id.etJamMulai3, R.id.etJamSelesai3, R.id.etRuang3)
            setSlot(3, R.id.etMatkul4, R.id.etDosen4, R.id.etJamMulai4, R.id.etJamSelesai4, R.id.etRuang4)
        }
    }

    // ================= SIMPAN KE DATABASE ==================

    private fun simpanKeDatabase() {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        data class SlotData(
            val id: Long?,
            val matkul: String,
            val dosen: String,
            val mulai: String,
            val selesai: String,
            val ruang: String
        )

        fun ambilSlot(
            index: Int,
            etMatkulId: Int,
            etDosenId: Int,
            etMulaiId: Int,
            etSelesaiId: Int,
            etRuangId: Int
        ): SlotData? {
            val matkul = findViewById<TextInputEditText>(etMatkulId).text?.toString()?.trim().orEmpty()
            val dosen = findViewById<TextInputEditText>(etDosenId).text?.toString()?.trim().orEmpty()
            val mulai = findViewById<TextInputEditText>(etMulaiId).text?.toString()?.trim().orEmpty()
            val selesai = findViewById<TextInputEditText>(etSelesaiId).text?.toString()?.trim().orEmpty()
            val ruang = findViewById<TextInputEditText>(etRuangId).text?.toString()?.trim().orEmpty()

            return if (
                matkul.isNotEmpty() &&
                dosen.isNotEmpty() &&
                mulai.isNotEmpty() &&
                selesai.isNotEmpty() &&
                ruang.isNotEmpty()
            ) {
                SlotData(
                    id = slotIds[index],
                    matkul = matkul,
                    dosen = dosen,
                    mulai = mulai,
                    selesai = selesai,
                    ruang = ruang
                )
            } else null
        }

        val slots = listOfNotNull(
            ambilSlot(0, R.id.etMatkul1, R.id.etDosen1, R.id.etJamMulai1, R.id.etJamSelesai1, R.id.etRuang1),
            ambilSlot(1, R.id.etMatkul2, R.id.etDosen2, R.id.etJamMulai2, R.id.etJamSelesai2, R.id.etRuang2),
            ambilSlot(2, R.id.etMatkul3, R.id.etDosen3, R.id.etJamMulai3, R.id.etJamSelesai3, R.id.etRuang3),
            ambilSlot(3, R.id.etMatkul4, R.id.etDosen4, R.id.etJamMulai4, R.id.etJamSelesai4, R.id.etRuang4)
        )

        if (slots.isEmpty()) {
            Toast.makeText(this, "Tidak ada jadwal yang lengkap diisi", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@TambahJadwalActivity)
            val dao = db.scheduleDao()
            val savedSchedules = mutableListOf<ScheduleEntity>()

            withContext(Dispatchers.IO) {
                for (s in slots) {
                    if (s.id != null && s.id != 0L) {
                        // update jadwal lama
                        val entity = ScheduleEntity(
                            id = s.id,
                            hari = selectedHari,
                            namaMatkul = s.matkul,
                            namaDosen = s.dosen,
                            jamMulai = s.mulai,
                            jamSelesai = s.selesai,
                            ruang = s.ruang
                        )
                        dao.updateSchedule(entity)
                        savedSchedules.add(entity)
                    } else {
                        // insert jadwal baru
                        val entity = ScheduleEntity(
                            hari = selectedHari,
                            namaMatkul = s.matkul,
                            namaDosen = s.dosen,
                            jamMulai = s.mulai,
                            jamSelesai = s.selesai,
                            ruang = s.ruang
                        )
                        val newId = dao.insertSchedule(entity)
                        savedSchedules.add(entity.copy(id = newId))
                    }
                }
            }

            // pasang alarm di main thread
            savedSchedules.forEach { schedule ->
                scheduleAlarmForJadwal(this@TambahJadwalActivity, schedule)
            }

            Toast.makeText(this@TambahJadwalActivity, "Jadwal tersimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ================= DIALOG PILIH HARI ==================

    private fun showHariDialog() {
        val items = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val currentIndex = items.indexOf(selectedHari).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Pilih hari")
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                selectedHari = items[which]
                tvPilihHari.text = selectedHari
                prefillJadwalForHari(selectedHari)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ================= UI UTIL ==================

    private fun setupExpandable(
        header: LinearLayout,
        icon: TextView,
        detail: LinearLayout,
        startExpanded: Boolean
    ) {
        detail.visibility = if (startExpanded) View.VISIBLE else View.GONE
        icon.text = if (startExpanded) "˄" else "˅"

        header.setOnClickListener {
            val isVisible = detail.visibility == View.VISIBLE
            detail.visibility = if (isVisible) View.GONE else View.VISIBLE
            icon.text = if (isVisible) "˅" else "˄"
        }
    }

    private fun initTimePicker(editText: TextInputEditText) {
        editText.setOnClickListener {
            showTimePicker { hour, minute ->
                editText.setText(formatTime(hour, minute))
            }
        }
    }

    private fun showTimePicker(onTimePicked: (Int, Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                onTimePicked(selectedHour, selectedMinute)
            },
            hour,
            minute,
            true
        )
        dialog.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        return "$h.$m"
    }

    private fun validateMinimal(): Boolean {
        val matkul1 = findViewById<TextInputEditText>(R.id.etMatkul1).text?.toString()?.trim().orEmpty()
        val dosen1 = findViewById<TextInputEditText>(R.id.etDosen1).text?.toString()?.trim().orEmpty()
        val jamMulai1 = findViewById<TextInputEditText>(R.id.etJamMulai1).text?.toString()?.trim().orEmpty()
        val jamSelesai1 = findViewById<TextInputEditText>(R.id.etJamSelesai1).text?.toString()?.trim().orEmpty()
        val ruang1 = findViewById<TextInputEditText>(R.id.etRuang1).text?.toString()?.trim().orEmpty()

        return matkul1.isNotEmpty() &&
                dosen1.isNotEmpty() &&
                jamMulai1.isNotEmpty() &&
                jamSelesai1.isNotEmpty() &&
                ruang1.isNotEmpty()
    }
}
