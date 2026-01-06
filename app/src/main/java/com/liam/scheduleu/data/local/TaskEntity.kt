package com.liam.scheduleu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scheduleHari: String,
    val scheduleNamaMatkul: String,

    val judul: String,
    val deskripsi: String? = null,

    val deadlineDate: String? = null,  // ini deadline tanggal
    val deadlineTime: String? = null,  // ini deadline jam

    val imageUri: String? = null,      // URI gambar tugas (opsional) [web:526]
    val selesai: Boolean = false
)

