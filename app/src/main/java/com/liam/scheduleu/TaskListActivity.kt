package com.liam.scheduleu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liam.scheduleu.data.local.AppDatabase
import com.liam.scheduleu.data.local.TaskDao
import com.liam.scheduleu.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskListActivity : AppCompatActivity() {

    private lateinit var tvHeader: TextView
    private lateinit var rvTasks: RecyclerView
    private lateinit var btnTambah: Button

    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterActive: Button
    private lateinit var btnFilterDone: Button

    private lateinit var adapter: TaskListAdapter

    private var hari: String = ""
    private var namaMatkul: String = ""

    private enum class FilterMode { ALL, ACTIVE, DONE }
    private var currentFilter = FilterMode.ALL
    private var fullList: List<TaskEntity> = emptyList()

    private var pendingImageCallback: ((Uri?) -> Unit)? = null
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            pendingImageCallback?.invoke(uri)
            pendingImageCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        hari = intent.getStringExtra("EXTRA_HARI") ?: ""
        namaMatkul = intent.getStringExtra("EXTRA_MATKUL") ?: ""

        tvHeader = findViewById(R.id.tvHeaderTask)
        rvTasks = findViewById(R.id.rvTasks)
        btnTambah = findViewById(R.id.btnTambahTask)

        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterActive = findViewById(R.id.btnFilterActive)
        btnFilterDone = findViewById(R.id.btnFilterDone)

        tvHeader.text = "Tugas untuk $namaMatkul ($hari)"

        rvTasks.layoutManager = LinearLayoutManager(this)

        val db = AppDatabase.getInstance(this)
        val taskDao = db.taskDao()

        adapter = TaskListAdapter(
            emptyList(),
            onCheckedChange = { task, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(task.copy(selesai = isChecked))
                    val list = taskDao.getTasksForSchedule(hari, namaMatkul)
                    withContext(Dispatchers.Main) {
                        fullList = list
                        applyFilter()
                    }
                }
            },
            onDeleteClick = { task ->
                AlertDialog.Builder(this)
                    .setTitle("Hapus tugas?")
                    .setMessage("Yakin mau hapus \"${task.judul}\"?")
                    .setPositiveButton("Hapus") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            taskDao.deleteTask(task)
                            val list = taskDao.getTasksForSchedule(hari, namaMatkul)
                            withContext(Dispatchers.Main) {
                                fullList = list
                                applyFilter()
                            }
                        }
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        )
        rvTasks.adapter = adapter

        btnFilterAll.setOnClickListener {
            currentFilter = FilterMode.ALL
            applyFilter()
        }
        btnFilterActive.setOnClickListener {
            currentFilter = FilterMode.ACTIVE
            applyFilter()
        }
        btnFilterDone.setOnClickListener {
            currentFilter = FilterMode.DONE
            applyFilter()
        }

        btnTambah.setOnClickListener {
            showTambahDialog(taskDao)
        }

        loadTasks(taskDao)
    }

    private fun loadTasks(taskDao: TaskDao) {
        lifecycleScope.launch {
            fullList = withContext(Dispatchers.IO) {
                taskDao.getTasksForSchedule(hari, namaMatkul)
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            FilterMode.ALL -> fullList
            FilterMode.ACTIVE -> fullList.filter { !it.selesai }
            FilterMode.DONE -> fullList.filter { it.selesai }
        }
        adapter.submitList(filtered)
    }

    private fun showTambahDialog(taskDao: TaskDao) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val etJudul = dialogView.findViewById<EditText>(R.id.etJudulTask)
        val etDeskripsi = dialogView.findViewById<EditText>(R.id.etDeskripsiTask)
        val tvTanggal = dialogView.findViewById<TextView>(R.id.tvTanggalTask)
        val tvJam = dialogView.findViewById<TextView>(R.id.tvJamTask)
        val btnPilihTanggal = dialogView.findViewById<Button>(R.id.btnPilihTanggal)
        val btnPilihJam = dialogView.findViewById<Button>(R.id.btnPilihJam)

        val tvDeadlineInfo = dialogView.findViewById<TextView>(R.id.tvDeadlineInfo)
        tvDeadlineInfo.text =
            "Tanggal & jam di bawah ini adalah DEADLINE tugas (opsional, boleh dikosongkan)."

        val btnPilihGambar = dialogView.findViewById<Button>(R.id.btnPilihGambar)
        val imgPreview = dialogView.findViewById<ImageView>(R.id.imgPreviewTask)

        var pickedDate: String? = null
        var pickedTime: String? = null
        var pickedImageUri: String? = null

        btnPilihTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            val dp = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    pickedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    tvTanggal.text = pickedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dp.show()
        }

        btnPilihJam.setOnClickListener {
            val cal = Calendar.getInstance()
            val tp = TimePickerDialog(
                this,
                { _, hour, minute ->
                    pickedTime = String.format("%02d.%02d", hour, minute)
                    tvJam.text = pickedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            )
            tp.show()
        }

        btnPilihGambar.setOnClickListener {
            pendingImageCallback = { uri ->
                if (uri != null) {
                    pickedImageUri = uri.toString()
                    imgPreview.setImageURI(uri)
                    imgPreview.visibility = ImageView.VISIBLE
                }
            }
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(this)
            .setTitle("Tambah Tugas")
            .setView(dialogView)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val judul = etJudul.text.toString().trim()
                val desk = etDeskripsi.text.toString().trim().ifBlank { null }

                if (judul.isBlank()) {
                    Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val task = TaskEntity(
                    scheduleHari = hari,
                    scheduleNamaMatkul = namaMatkul,
                    judul = judul,
                    deskripsi = desk,
                    deadlineDate = pickedDate,
                    deadlineTime = pickedTime,
                    imageUri = pickedImageUri,
                    selesai = false
                )

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val id = taskDao.insertTask(task)
                        val taskWithId = task.copy(id = id)
                        scheduleAlarmsForTask(this@TaskListActivity, taskWithId)
                    }
                    loadTasks(taskDao)
                }
            }
            .show()
    }
}
