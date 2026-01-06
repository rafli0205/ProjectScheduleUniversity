package com.liam.scheduleu

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.liam.scheduleu.data.local.AppDatabase
import com.liam.scheduleu.data.local.ScheduleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JadwalListActivity : AppCompatActivity() {

    private lateinit var adapter: JadwalListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_list)

        findViewById<TextView?>(R.id.btnBack)?.setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvJadwal)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = JadwalListAdapter(emptyList()) { schedule: ScheduleEntity ->
            val intent = Intent(this, EditJadwalActivity::class.java)
            intent.putExtra("hari", schedule.hari)
            intent.putExtra("jamMulai", schedule.jamMulai)
            startActivity(intent)
        }
        rv.adapter = adapter

        findViewById<MaterialButton?>(R.id.btnExport)?.setOnClickListener {
            exportAndShareJadwal()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val db = AppDatabase.getInstance(this)
        val scheduleDao = db.scheduleDao()
        val taskDao = db.taskDao()

        lifecycleScope.launch {
            val listWithCount: List<ScheduleWithCount> = withContext(Dispatchers.IO) {
                val schedules = scheduleDao.getAllSchedules()
                schedules.map { s ->
                    val count = taskDao.countUnfinishedForSchedule(s.hari, s.namaMatkul)
                    ScheduleWithCount(schedule = s, taskCount = count)
                }
            }
            adapter.submitList(listWithCount)
        }
    }

    // =============== EXPORT & SHARE JADWAL ===============

    private fun exportAndShareJadwal() {
        val db = AppDatabase.getInstance(this)
        val dao = db.scheduleDao()

        lifecycleScope.launch {
            val all: List<ScheduleEntity> = withContext(Dispatchers.IO) {
                dao.getAllSchedules()
            }

            if (all.isEmpty()) {
                Toast.makeText(
                    this@JadwalListActivity,
                    "Belum ada jadwal untuk diexport",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val builder = StringBuilder()
            val grouped = all.groupBy { it.hari }
                .toSortedMap(compareBy { hariOrder(it) })

            for ((hari, list) in grouped) {
                builder.appendLine(hari.uppercase())
                list.sortedBy { it.jamMulai }.forEach { s ->
                    builder.appendLine("${s.jamMulai} - ${s.jamSelesai}  ${s.namaMatkul} (${s.ruang})")
                }
                builder.appendLine()
            }

            val text = builder.toString()

            val fileName = "ScheduleU_jadwal.txt"
            val file = java.io.File(filesDir, fileName)
            file.writeText(text, Charsets.UTF_8)

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this@JadwalListActivity,
                "${packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Jadwal Kuliah")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(sendIntent, "Bagikan jadwal via"))
        }
    }

    private fun hariOrder(hari: String): Int =
        when (hari) {
            "Senin" -> 1
            "Selasa" -> 2
            "Rabu" -> 3
            "Kamis" -> 4
            "Jumat" -> 5
            "Sabtu" -> 6
            "Minggu" -> 7
            else -> 99
        }
}
