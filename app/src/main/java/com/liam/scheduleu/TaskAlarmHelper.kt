package com.liam.scheduleu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.liam.scheduleu.data.local.TaskEntity
import java.util.Calendar

fun scheduleAlarmsForTask(context: Context, task: TaskEntity) {
    val date = task.deadlineDate ?: return   // yyyy-MM-dd
    val time = task.deadlineTime ?: return   // HH.mm

    val dateParts = date.split("-")
    val timeParts = time.split(".")

    if (dateParts.size != 3 || timeParts.size != 2) return

    val year = dateParts[0].toIntOrNull() ?: return
    val month = dateParts[1].toIntOrNull()?.minus(1) ?: return  // Calendar month 0-based
    val day = dateParts[2].toIntOrNull() ?: return
    val hour = timeParts[0].toIntOrNull() ?: return
    val minute = timeParts[1].toIntOrNull() ?: return

    val calendarDeadline = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val now = System.currentTimeMillis()
    if (calendarDeadline.timeInMillis <= now) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val baseRequestCode =
        (task.scheduleHari + task.scheduleNamaMatkul + task.judul + date + time).hashCode()

    val deadlineText = "Deadline: $date • $time"

    // Alarm tepat di jam deadline
    val intentDeadline = Intent(context, TaskAlarmReceiver::class.java).apply {
        putExtra("EXTRA_JUDUL", task.judul)
        putExtra("EXTRA_MATKUL", task.scheduleNamaMatkul)
        putExtra("EXTRA_DEADLINE_TEXT", deadlineText)
        putExtra("EXTRA_TYPE", "deadline")
        putExtra("EXTRA_TASK_ID", task.id)  // penting untuk auto delete
    }

    val pendingDeadline = PendingIntent.getBroadcast(
        context,
        baseRequestCode,
        intentDeadline,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendarDeadline.timeInMillis,
        pendingDeadline
    )

    // Alarm H-1 (opsional)
    val calHminus1 = calendarDeadline.clone() as Calendar
    calHminus1.add(Calendar.DAY_OF_MONTH, -1)

    if (calHminus1.timeInMillis > now) {
        val intentHminus1 = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_JUDUL", task.judul)
            putExtra("EXTRA_MATKUL", task.scheduleNamaMatkul)
            putExtra("EXTRA_DEADLINE_TEXT", deadlineText)
            putExtra("EXTRA_TYPE", "hminus1")
            putExtra("EXTRA_TASK_ID", task.id)
        }

        val pendingHminus1 = PendingIntent.getBroadcast(
            context,
            baseRequestCode + 1,
            intentHminus1,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calHminus1.timeInMillis,
            pendingHminus1
        )
    }
}
