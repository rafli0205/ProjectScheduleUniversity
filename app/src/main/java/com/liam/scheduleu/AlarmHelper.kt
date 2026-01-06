package com.liam.scheduleu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.liam.scheduleu.data.local.TaskEntity
import java.util.Calendar

/**
 * Helper untuk menjadwalkan alarm tugas (H-1 / H-2 / hanya saat deadline)
 * berdasarkan setting user.
 */
object TaskAlarmHelper {

    private data class NotifPrefs(
        val enabled: Boolean,
        val defaultReminder: String, // AT_DEADLINE, H1, H2
    )

    private fun getNotifPrefs(context: Context): NotifPrefs {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val enabled = prefs.getBoolean("pref_notifications_enabled", true)
        val defaultReminder = prefs.getString("pref_default_reminder", "H1") ?: "H1"
        return NotifPrefs(enabled, defaultReminder)
    }

    fun scheduleAlarmsForTask(context: Context, task: TaskEntity) {
        val prefs = getNotifPrefs(context)
        if (!prefs.enabled) return  // notifikasi dimatikan di Settings

        val date = task.deadlineDate ?: return   // format: yyyy-MM-dd
        val time = task.deadlineTime ?: return   // format: HH.mm

        val dateParts = date.split("-")
        val timeParts = time.split(".")

        if (dateParts.size != 3 || timeParts.size != 2) return

        val year = dateParts[0].toIntOrNull() ?: return
        val month = dateParts[1].toIntOrNull()?.minus(1) ?: return  // Calendar: 0-based
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

        // baseRequestCode unik per task
        val baseRequestCode =
            (task.scheduleHari + task.scheduleNamaMatkul + task.judul + date + time).hashCode()

        val deadlineText = "Deadline: $date • $time"

        // ====== Alarm tepat di jam deadline (SELALU dipasang kalau notif aktif) ======
        val intentDeadline = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_JUDUL", task.judul)
            putExtra("EXTRA_MATKUL", task.scheduleNamaMatkul)
            putExtra("EXTRA_DEADLINE_TEXT", deadlineText)
            putExtra("EXTRA_TYPE", "deadline")
            putExtra("EXTRA_TASK_ID", task.id)
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
        // gunakan setExactAndAllowWhileIdle untuk alarm penting bagi user. [web:613][web:877]

        // ====== Alarm sebelum hari-H berdasarkan setting (H1 / H2 / AT_DEADLINE) ======
        when (prefs.defaultReminder) {
            "H1" -> {
                scheduleDayOffsetAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    baseRequestCode = baseRequestCode + 1,
                    calendarBase = calendarDeadline,
                    dayOffset = -1,
                    type = "hminus1",
                    task = task,
                    deadlineText = deadlineText,
                    now = now
                )
            }
            "H2" -> {
                scheduleDayOffsetAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    baseRequestCode = baseRequestCode + 2,
                    calendarBase = calendarDeadline,
                    dayOffset = -2,
                    type = "hminus2",
                    task = task,
                    deadlineText = deadlineText,
                    now = now
                )
            }
            "AT_DEADLINE" -> {
                // tidak pasang alarm H-1/H-2, cukup yang jam deadline saja
            }
        }
    }

    private fun scheduleDayOffsetAlarm(
        context: Context,
        alarmManager: AlarmManager,
        baseRequestCode: Int,
        calendarBase: Calendar,
        dayOffset: Int,
        type: String,
        task: TaskEntity,
        deadlineText: String,
        now: Long
    ) {
        val cal = calendarBase.clone() as Calendar
        cal.add(Calendar.DAY_OF_MONTH, dayOffset)

        if (cal.timeInMillis <= now) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("EXTRA_JUDUL", task.judul)
            putExtra("EXTRA_MATKUL", task.scheduleNamaMatkul)
            putExtra("EXTRA_DEADLINE_TEXT", deadlineText)
            putExtra("EXTRA_TYPE", type)
            putExtra("EXTRA_TASK_ID", task.id)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            baseRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pending
        )
    }
}
