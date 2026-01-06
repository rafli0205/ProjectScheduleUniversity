package com.liam.scheduleu.data.local

import androidx.room.*

@Dao
interface TaskDao {

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE scheduleHari = :hari AND scheduleNamaMatkul = :namaMatkul ORDER BY deadlineDate, deadlineTime")
    suspend fun getTasksForSchedule(hari: String, namaMatkul: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deadlineDate = :date ORDER BY deadlineTime")
    suspend fun getTasksByDate(date: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE scheduleHari = :hari AND scheduleNamaMatkul = :nama AND selesai = 0")
    suspend fun countUnfinishedForSchedule(hari: String, nama: String): Int

    @Query("DELETE FROM tasks WHERE deadlineDate < :today OR (deadlineDate = :today AND deadlineTime < :nowTime)")
    suspend fun deleteExpiredTasks(today: String, nowTime: String)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

}
