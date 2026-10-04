package com.lelysnails.agenda.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.lelysnails.agenda.R
import com.lelysnails.agenda.data.AppointmentStore
import com.lelysnails.agenda.model.DayKind
import com.lelysnails.agenda.ui.StylePalette
import java.util.Calendar

class AgendaWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val widgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private data class Cell(
        val day: Int,
        val key: String?,
        val kind: DayKind,
        val selected: Boolean,
        val isToday: Boolean,
        val dots: String
    )

    private var cells: List<Cell> = emptyList()
    private var palette: StylePalette = WidgetTheme.palette(context)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        palette = WidgetTheme.palette(context)
        val store = AppointmentStore(context)
        val year = WidgetPrefs.getYear(context, widgetId)
        val month = WidgetPrefs.getMonth(context, widgetId)
        val selected = WidgetPrefs.getSelectedKey(context, widgetId)

        val today = Calendar.getInstance()
        val todayKey = AppointmentStore.dateKey(
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val monFirst = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        val offset = monFirst.indexOf(firstDow).coerceAtLeast(0)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val list = mutableListOf<Cell>()
        repeat(offset) { list.add(Cell(0, null, DayKind.BLANK, false, false, "")) }

        for (d in 1..daysInMonth) {
            val key = AppointmentStore.dateKey(year, month, d)
            val kind = store.dayKind(key)
            val max = store.maxSlots()
            val dots = buildString {
                for (s in 1..minOf(max, 4)) {
                    append(if (store.getAppointment(key, s) != null) "\u25CF" else "\u25CB")
                }
            }.takeIf { kind != DayKind.OFF && kind != DayKind.BLANK } ?: ""
            list.add(Cell(d, key, kind, key == selected, key == todayKey, dots))
        }

        val total = offset + daysInMonth
        val tail = (7 - total % 7) % 7
        repeat(tail) { list.add(Cell(0, null, DayKind.BLANK, false, false, "")) }
        cells = list
    }

    override fun onDestroy() { cells = emptyList() }

    override fun getCount(): Int = cells.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_day_cell)
        val p = palette
        if (position !in cells.indices) return rv
        val cell = cells[position]

        if (cell.kind == DayKind.BLANK || cell.day == 0) {
            rv.setTextViewText(R.id.cellDay, "")
            rv.setTextViewText(R.id.cellDots, "")
            rv.setInt(R.id.cellInner, "setBackgroundColor", 0x00000000)
            return rv
        }

        val label = if (cell.isToday) "\u00B7${cell.day}" else cell.day.toString()
        rv.setTextViewText(R.id.cellDay, label)
        rv.setTextViewText(R.id.cellDots, cell.dots)

        when {
            cell.selected && cell.kind != DayKind.OFF -> {
                rv.setInt(R.id.cellInner, "setBackgroundColor", p.surface)
                rv.setTextColor(R.id.cellDay, p.primary)
                rv.setTextColor(R.id.cellDots, p.primary)
            }
            cell.kind == DayKind.OFF -> {
                rv.setInt(R.id.cellInner, "setBackgroundColor", p.offBg)
                rv.setTextColor(R.id.cellDay, p.offText)
                rv.setTextColor(R.id.cellDots, p.offText)
            }
            cell.kind == DayKind.PARTIAL -> {
                rv.setInt(R.id.cellInner, "setBackgroundColor", p.partialEnd)
                rv.setTextColor(R.id.cellDay, p.partialInk)
                rv.setTextColor(R.id.cellDots, p.partialInk)
            }
            cell.kind == DayKind.FULL -> {
                rv.setInt(R.id.cellInner, "setBackgroundColor", p.fullEnd)
                rv.setTextColor(R.id.cellDay, p.onPrimary)
                rv.setTextColor(R.id.cellDots, p.onPrimary)
            }
            else -> {
                rv.setInt(R.id.cellInner, "setBackgroundColor", p.freeBg)
                rv.setTextColor(
                    R.id.cellDay,
                    if (cell.isToday) p.primary else p.ink
                )
                rv.setTextColor(R.id.cellDots, p.primary)
            }
        }

        if (cell.key != null && cell.kind != DayKind.OFF) {
            val fill = Intent().apply {
                putExtra(AgendaWidgetProvider.EXTRA_DATE_KEY, cell.key)
            }
            rv.setOnClickFillInIntent(R.id.cellRoot, fill)
            rv.setOnClickFillInIntent(R.id.cellInner, fill)
            rv.setOnClickFillInIntent(R.id.cellDay, fill)
        }

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
