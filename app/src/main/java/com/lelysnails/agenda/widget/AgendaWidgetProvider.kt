package com.lelysnails.agenda.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.lelysnails.agenda.R
import com.lelysnails.agenda.data.AppointmentStore
import java.util.Calendar

class AgendaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.clear(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            if (action == ACTION_REFRESH_ALL) {
                refreshAll(context)
            }
            return
        }

        when (action) {
            ACTION_PREV -> {
                var m = WidgetPrefs.getMonth(context, widgetId)
                var y = WidgetPrefs.getYear(context, widgetId)
                m--; if (m < 0) { m = 11; y-- }
                WidgetPrefs.setYearMonth(context, widgetId, y, m)
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
            ACTION_NEXT -> {
                var m = WidgetPrefs.getMonth(context, widgetId)
                var y = WidgetPrefs.getYear(context, widgetId)
                m++; if (m > 11) { m = 0; y++ }
                WidgetPrefs.setYearMonth(context, widgetId, y, m)
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
            ACTION_REFRESH -> {
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
            ACTION_DAY_CLICK -> {
                val key = intent.getStringExtra(EXTRA_DATE_KEY) ?: return
                val store = AppointmentStore(context)
                if (store.isOff(key)) {
                    // No se puede modificar desde el widget
                    return
                }
                WidgetPrefs.setSelectedKey(context, widgetId, key)
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
            }
        }
    }

    companion object {
        const val ACTION_PREV = "com.lelysnails.agenda.WIDGET_PREV"
        const val ACTION_NEXT = "com.lelysnails.agenda.WIDGET_NEXT"
        const val ACTION_REFRESH = "com.lelysnails.agenda.WIDGET_REFRESH"
        const val ACTION_REFRESH_ALL = "com.lelysnails.agenda.WIDGET_REFRESH_ALL"
        const val ACTION_DAY_CLICK = "com.lelysnails.agenda.WIDGET_DAY"
        const val EXTRA_DATE_KEY = "date_key"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, AgendaWidgetProvider::class.java))
            ids.forEach { updateWidget(context, mgr, it) }
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetGrid)
        }

        fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_agenda)
            val store = AppointmentStore(context)

            val alpha = WidgetPrefs.getAlpha(context, widgetId)
            // Alpha del fondo: 20-95% -> 0x33..0xF2 sobre #FDF6F9
            val a = (alpha * 255 / 100).coerceIn(40, 245)
            val bgColor = Color.argb(a, 0xFD, 0xF6, 0xF9)
            views.setInt(R.id.widgetRoot, "setBackgroundColor", bgColor)

            val year = WidgetPrefs.getYear(context, widgetId)
            val month = WidgetPrefs.getMonth(context, widgetId)
            val months = context.resources.getStringArray(R.array.months)
            views.setTextViewText(R.id.widgetMonthLabel, "${months[month]} $year")

            // Nav intents
            views.setOnClickPendingIntent(R.id.widgetBtnPrev, broadcast(context, widgetId, ACTION_PREV))
            views.setOnClickPendingIntent(R.id.widgetBtnNext, broadcast(context, widgetId, ACTION_NEXT))
            views.setOnClickPendingIntent(R.id.widgetBtnRefresh, broadcast(context, widgetId, ACTION_REFRESH))

            // Grid adapter
            val svc = Intent(context, AgendaWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widgetGrid, svc)
            views.setEmptyView(R.id.widgetGrid, R.id.widgetMonthLabel)

            val clickTpl = Intent(context, AgendaWidgetProvider::class.java).apply {
                action = ACTION_DAY_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val clickPi = PendingIntent.getBroadcast(
                context, widgetId, clickTpl,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widgetGrid, clickPi)

            // Selected day panel
            var sel = WidgetPrefs.getSelectedKey(context, widgetId)
            if (sel == null) {
                val now = Calendar.getInstance()
                sel = AppointmentStore.dateKey(
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
                )
                WidgetPrefs.setSelectedKey(context, widgetId, sel)
            }

            val parsed = AppointmentStore.parseKey(sel)
            if (parsed != null) {
                val (y, m, d) = parsed
                val cal = Calendar.getInstance().apply { set(y, m, d) }
                val daysLong = context.resources.getStringArray(R.array.days_long)
                val monthNames = context.resources.getStringArray(R.array.months)
                views.setTextViewText(
                    R.id.widgetSelectedDate,
                    "${daysLong[cal.get(Calendar.DAY_OF_WEEK) - 1]} $d de ${monthNames[m]}"
                )
            } else {
                views.setTextViewText(R.id.widgetSelectedDate, sel)
            }

            val isOff = store.isOff(sel)
            val max = store.maxSlots()
            val lines = StringBuilder()
            if (isOff) {
                lines.append("No laborable · no se puede modificar desde el widget")
                views.setViewVisibility(R.id.widgetBtnQuickAdd, View.GONE)
            } else {
                var any = false
                for (s in 1..max) {
                    val ap = store.getAppointment(sel, s)
                    if (ap != null) {
                        any = true
                        val t = if (ap.time.isNotBlank()) " ${ap.time}" else ""
                        lines.append("• ${ap.name}$t\n")
                    }
                }
                if (!any) lines.append("Sin turnos · toca + para agregar")
                val free = store.findFreeSlot(sel)
                if (free != null) {
                    views.setViewVisibility(R.id.widgetBtnQuickAdd, View.VISIBLE)
                    val add = Intent(context, QuickAddActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(EXTRA_DATE_KEY, sel)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                        data = Uri.parse("lelys://quickadd/$widgetId/$sel")
                    }
                    val addPi = PendingIntent.getActivity(
                        context, widgetId + 10000, add,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetBtnQuickAdd, addPi)
                } else {
                    views.setViewVisibility(R.id.widgetBtnQuickAdd, View.GONE)
                    if (lines.toString().startsWith("•")) {
                        // dia lleno
                    } else {
                        lines.append("\nDia completo")
                    }
                }
            }
            views.setTextViewText(R.id.widgetAppointments, lines.toString().trim())

            mgr.updateAppWidget(widgetId, views)
            mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widgetGrid)
        }

        private fun broadcast(context: Context, widgetId: Int, action: String): PendingIntent {
            val i = Intent(context, AgendaWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse("lelys://widget/$action/$widgetId")
            }
            return PendingIntent.getBroadcast(
                context, widgetId + action.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
