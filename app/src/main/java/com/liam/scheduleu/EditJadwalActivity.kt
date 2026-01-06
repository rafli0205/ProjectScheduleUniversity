package com.liam.scheduleu

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.liam.scheduleu.data.local.AppDatabase
import com.liam.scheduleu.data.local.ScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import com.liam.scheduleu.scheduleAlarmForJadwal
import android.content.Intent
import androidx.cardview.widget.CardView

class EditJadwalActivity : AppCompatActivity() {

    private lateinit var tvHariEdit: TextView
    private lateinit var etMatkulEdit: TextInputEditText
    private lateinit var etDosenEdit: TextInputEditText
    private lateinit var etJamMulaiEdit: TextInputEditText
    private lateinit var etJamSelesaiEdit: TextInputEditText
    private lateinit var etRuangEdit: TextInputEditText

    private lateinit var btnTab1: TextView
    private lateinit var btnTab2: TextView
    private lateinit var btnTab3: TextView
    private lateinit var btnTab4: TextView

    // tombol untuk buka list tugas
    private lateinit var btnTugas: CardView

    private var selectedHari: String = "Senin"
    private var initialJamMulai: String? = null

    // cache jadwal 1–4 untuk hari ini
    private var jadwalList: MutableList<ScheduleEntity> = mutableListOf()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_jadwal)

        // Back
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvHariEdit = findViewById(R.id.tvHariEdit)
        etMatkulEdit = findViewById(R.id.etMatkulEdit)
        etDosenEdit = findViewById(R.id.etDosenEdit)
        etJamMulaiEdit = findViewById(R.id.etJamMulaiEdit)
        etJamSelesaiEdit = findViewById(R.id.etJamSelesaiEdit)
        etRuangEdit = findViewById(R.id.etRuangEdit)

        btnTab1 = findViewById(R.id.tabJadwal1)
        btnTab2 = findViewById(R.id.tabJadwal2)
        btnTab3 = findViewById(R.id.tabJadwal3)
        btnTab4 = findViewById(R.id.tabJadwal4)

        btnTugas = findViewById(R.id.btnTugas) // pastikan ada di XML

        // Hari & jamMulai dikirim lewat Intent dari JadwalListActivity
        selectedHari = intent.getStringExtra("hari") ?: "Senin"
        tvHariEdit.text = selectedHari

        initialJamMulai = intent.getStringExtra("jamMulai")

        // Picker hari
        findViewById<View>(R.id.cardPilihHariEdit).setOnClickListener {
            showHariDialog()
        }

        // Time picker
        initTimePicker(etJamMulaiEdit)
        initTimePicker(etJamSelesaiEdit)

        // Tab jadwal 1–4
        btnTab1.setOnClickListener { switchToSlot(0) }
        btnTab2.setOnClickListener { switchToSlot(1) }
        btnTab3.setOnClickListener { switchToSlot(2) }
        btnTab4.setOnClickListener { switchToSlot(3) }

        findViewById<MaterialButton>(R.id.btnSimpanEdit).setOnClickListener {
            saveCurrentSlotToCache(currentIndex)
            saveAllToDatabase()
        }

        findViewById<TextView>(R.id.btnHapus).setOnClickListener {
            showHapusDialog()
        }

        // === TUGAS / TASK ===
        btnTugas.setOnClickListener {
            val namaMatkul = etMatkulEdit.text?.toString()?.trim().orEmpty()
            if (namaMatkul.isBlank()) {
                Toast.makeText(this, "Isi nama mata kuliah dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, TaskListActivity::class.java).apply {
                putExtra("EXTRA_HARI", selectedHari)
                putExtra("EXTRA_MATKUL", namaMatkul)
            }
            startActivity(intent)
        }

        // load data dari Room
        loadData()
    }

    // ========== LOAD & CACHE ==========

    private fun loadData() {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            val dataForHari = withContext(Dispatchers.IO) {
                dao.getSchedulesByHari(selectedHari)
            }

            jadwalList = MutableList(4) { index ->
                dataForHari.getOrNull(index) ?: ScheduleEntity(
                    id = 0L,
                    hari = selectedHari,
                    namaMatkul = "",
                    namaDosen = "",
                    jamMulai = "",
                    jamSelesai = "",
                    ruang = "",
                    motivasi = null
                )
            }

            // pilih slot awal sesuai jadwal yang diklik (jamMulai)
            currentIndex = initialJamMulai?.let { jam ->
                jadwalList.indexOfFirst { it.jamMulai == jam }.takeIf { it >= 0 } ?: 0
            } ?: 0

            showSlot(currentIndex)
            updateTabUI()
        }
    }

    // ========== TAB / SLOT HANDLING ==========

    private fun switchToSlot(index: Int) {
        if (index == currentIndex) return
        saveCurrentSlotToCache(currentIndex)
        currentIndex = index
        showSlot(currentIndex)
        updateTabUI()
    }

    private fun showSlot(index: Int) {
        val s = jadwalList[index]
        etMatkulEdit.setText(s.namaMatkul)
        etDosenEdit.setText(s.namaDosen)
        etJamMulaiEdit.setText(s.jamMulai)
        etJamSelesaiEdit.setText(s.jamSelesai)
        etRuangEdit.setText(s.ruang)
    }

    private fun saveCurrentSlotToCache(index: Int) {
        val old = jadwalList[index]
        jadwalList[index] = old.copy(
            hari = selectedHari,
            namaMatkul = etMatkulEdit.text?.toString()?.trim().orEmpty(),
            namaDosen = etDosenEdit.text?.toString()?.trim().orEmpty(),
            jamMulai = etJamMulaiEdit.text?.toString()?.trim().orEmpty(),
            jamSelesai = etJamSelesaiEdit.text?.toString()?.trim().orEmpty(),
            ruang = etRuangEdit.text?.toString()?.trim().orEmpty()
        )
    }

    private fun updateTabUI() {
        val tabs = listOf(btnTab1, btnTab2, btnTab3, btnTab4)
        tabs.forEachIndexed { i, tv ->
            tv.setBackgroundResource(
                if (i == currentIndex) R.drawable.bg_tab_selected
                else R.drawable.bg_tab_unselected
            )
        }
    }

    // ========== SIMPAN KE ROOM ==========

    private fun saveAllToDatabase() {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            saveCurrentSlotToCache(currentIndex)

            val nonEmpty: List<ScheduleEntity>

            withContext(Dispatchers.IO) {
                nonEmpty = jadwalList.filter { it.namaMatkul.isNotBlank() }

                dao.deleteByHari(selectedHari)
                if (nonEmpty.isNotEmpty()) {
                    dao.insertSchedules(nonEmpty)
                }
            }

            // pasang alarm untuk jadwal yang tersisa
            nonEmpty.forEach { schedule ->
                scheduleAlarmForJadwal(this@EditJadwalActivity, schedule)
            }

            Toast.makeText(this@EditJadwalActivity, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ========== DIALOG HAPUS CHECKLIST ==========

    private fun showHapusDialog() {
        val items = arrayOf("Jadwal 1", "Jadwal 2", "Jadwal 3", "Jadwal 4")
        val checked = BooleanArray(4) { false }

        AlertDialog.Builder(this)
            .setTitle("Hapus jadwal yang mana?")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                performDeleteSlots(checked)
            }
            .show()
    }

    private fun performDeleteSlots(checked: BooleanArray) {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                saveCurrentSlotToCache(currentIndex)

                val remaining = jadwalList.mapIndexedNotNull { index, s ->
                    if (checked[index]) {
                        null
                    } else if (s.namaMatkul.isNotBlank()) {
                        s
                    } else null
                }

                dao.deleteByHari(selectedHari)
                if (remaining.isNotEmpty()) {
                    dao.insertSchedules(remaining)
                }
            }

            Toast.makeText(this@EditJadwalActivity, "Jadwal terhapus", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ========== PICKER HARI & JAM ==========

    private fun showHariDialog() {
        val items = arrayOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val currentIndex = items.indexOf(selectedHari).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Pilih hari")
            .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                selectedHari = items[which]
                tvHariEdit.text = selectedHari
                dialog.dismiss()
                loadData()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun initTimePicker(editText: TextInputEditText) {
        editText.setOnClickListener {
            showTimePicker { hour, minute ->
                val h = hour.toString().padStart(2, '0')
                val m = minute.toString().padStart(2, '0')
                editText.setText("$h.$m")
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
}
