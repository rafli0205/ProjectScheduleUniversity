package com.liam.scheduleu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScheduleDao {

    @Insert
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @androidx.room.Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE hari = :hari ORDER BY jamMulai")
    suspend fun getSchedulesByHari(hari: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules ORDER BY hari, jamMulai")
    suspend fun getAllSchedules(): List<ScheduleEntity>

    @Query("DELETE FROM schedules WHERE hari = :hari")
    suspend fun deleteByHari(hari: String)
}

