package com.liam.scheduleu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.liam.scheduleu.data.local.ScheduleEntity
import java.util.Calendar

/**
 * Alarm untuk JADWAL KULIAH (bukan tugas).
 * Contoh: notifikasi 15 menit sebelum kelas mulai.
 */
fun scheduleAlarmForJadwal(context: Context, jadwal: ScheduleEntity) {
    // Kalau jam mulai kosong, jangan set alarm
    if (jadwal.jamMulai.isBlank()) return

    // jamMulai format "HH.mm"
    val parts = jadwal.jamMulai.split(".")
    if (parts.size != 2) return
    val hour = parts[0].toIntOrNull() ?: return
    val minute = parts[1].toIntOrNull() ?: return

    val cal = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Misal notifikasi 15 menit sebelum kelas
    cal.add(Calendar.MINUTE, -15)

    val now = System.currentTimeMillis()
    if (cal.timeInMillis <= now) return   // kalau sudah lewat, skip

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val requestCode = (jadwal.hari + jadwal.namaMatkul + jadwal.jamMulai).hashCode()

    val intent = Intent(context, JadwalAlarmReceiver::class.java).apply {
        putExtra("EXTRA_HARI", jadwal.hari)
        putExtra("EXTRA_MATKUL", jadwal.namaMatkul)
        putExtra("EXTRA_JAM", jadwal.jamMulai)
        putExtra("EXTRA_RUANG", jadwal.ruang)
    }

    val pending = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        cal.timeInMillis,
        pending
    )
}
