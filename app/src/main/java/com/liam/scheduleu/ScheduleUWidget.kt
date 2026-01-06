package com.liam.scheduleu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.liam.scheduleu.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleUWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // ambil jadwal hari ini di background
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val dao = db.scheduleDao()

            val hari = getTodayName()
            val list = dao.getSchedulesByHari(hari)

            // ambil jadwal terdekat (paling awal hari ini)
            val next = list.minByOrNull { it.jamMulai }

            appWidgetIds.forEach { appWidgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_scheduleu)

                if (next != null) {
                    views.setTextViewText(R.id.tvWidgetHari, hari)
                    views.setTextViewText(R.id.tvWidgetMatkul, next.namaMatkul)
                    views.setTextViewText(
                        R.id.tvWidgetJam,
                        "${next.jamMulai} - ${next.jamSelesai} • ${next.ruang}"
                    )
                } else {
                    views.setTextViewText(R.id.tvWidgetHari, hari)
                    views.setTextViewText(R.id.tvWidgetMatkul, "Tidak ada jadwal")
                    views.setTextViewText(R.id.tvWidgetJam, "Nikmati waktu luangmu ✨")
                }

                // klik widget -> buka HomeActivity
                val intent = Intent(context, HomeActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        fun getTodayName(): String {
            val cal = Calendar.getInstance()
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Senin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Kamis"
                Calendar.FRIDAY -> "Jumat"
                Calendar.SATURDAY -> "Sabtu"
                else -> "Minggu"
            }
        }

        // helper kalau nanti mau trigger update manual dari app
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ScheduleUWidget::class.java)
            )
            val intent = Intent(context, ScheduleUWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
