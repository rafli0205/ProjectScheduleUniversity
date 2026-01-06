package com.liam.scheduleu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class JadwalAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hari = intent.getStringExtra("EXTRA_HARI") ?: ""
        val matkul = intent.getStringExtra("EXTRA_MATKUL") ?: "Kelas"
        val jam = intent.getStringExtra("EXTRA_JAM") ?: ""
        val ruang = intent.getStringExtra("EXTRA_RUANG") ?: ""

        val channelId = "jadwal_kuliah_channel"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Pengingat Jadwal Kuliah",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(ch)
        }

        val tapIntent = Intent(context, HomeActivity::class.java)
        val pendingTap = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Kelas $matkul sebentar lagi"
        val text = "$hari · $jam · Ruang $ruang"

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingTap)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
