package com.lelysnails.agenda.widget

import android.content.Context
import java.util.Calendar

object WidgetPrefs {
    private const val PREFS = "lelys_widget_prefs"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setAlpha(ctx: Context, widgetId: Int, alphaPercent: Int) {
        p(ctx).edit().putInt("alpha_$widgetId", alphaPercent.coerceIn(20, 95)).apply()
    }

    fun getAlpha(ctx: Context, widgetId: Int): Int =
        p(ctx).getInt("alpha_$widgetId", 88)

    fun setYearMonth(ctx: Context, widgetId: Int, year: Int, month0: Int) {
        p(ctx).edit()
            .putInt("year_$widgetId", year)
            .putInt("month_$widgetId", month0)
            .apply()
    }

    fun getYear(ctx: Context, widgetId: Int): Int {
        val def = Calendar.getInstance().get(Calendar.YEAR)
        return p(ctx).getInt("year_$widgetId", def)
    }

    fun getMonth(ctx: Context, widgetId: Int): Int {
        val def = Calendar.getInstance().get(Calendar.MONTH)
        return p(ctx).getInt("month_$widgetId", def)
    }

    fun setSelectedKey(ctx: Context, widgetId: Int, key: String?) {
        p(ctx).edit().putString("sel_$widgetId", key).apply()
    }

    fun getSelectedKey(ctx: Context, widgetId: Int): String? =
        p(ctx).getString("sel_$widgetId", null)

    fun clear(ctx: Context, widgetId: Int) {
        p(ctx).edit()
            .remove("alpha_$widgetId")
            .remove("year_$widgetId")
            .remove("month_$widgetId")
            .remove("sel_$widgetId")
            .apply()
    }
}
