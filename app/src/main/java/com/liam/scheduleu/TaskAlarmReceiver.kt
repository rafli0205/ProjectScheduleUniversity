package com.liam.scheduleu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.liam.scheduleu.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val judul = intent.getStringExtra("EXTRA_JUDUL") ?: "Tugas"
        val matkul = intent.getStringExtra("EXTRA_MATKUL") ?: ""
        val deadlineText = intent.getStringExtra("EXTRA_DEADLINE_TEXT") ?: ""
        val type = intent.getStringExtra("EXTRA_TYPE") ?: "deadline"
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)

        val channelId = "task_deadline_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val nm =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Tugas",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, HomeActivity::class.java)
        val pendingTap = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (type) {
            "hminus1" -> "Besok deadline tugas $judul ($matkul). $deadlineText"
            else -> "Deadline tugas $judul ($matkul) sekarang. $deadlineText"
        }

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pengingat Tugas")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingTap)
            .setAutoCancel(true)
            .build()

        nm.notify(notificationId, notif)

        // Auto delete saat tepat deadline
        if (type == "deadline" && taskId != -1L) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(appContext)
                db.taskDao().deleteById(taskId)
            }
        }
    }
}
