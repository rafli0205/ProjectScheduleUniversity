package com.liam.scheduleu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hari: String,
    val namaMatkul: String,
    val namaDosen: String,
    val jamMulai: String,
    val jamSelesai: String,
    val ruang: String,
    val motivasi: String? = null
)
