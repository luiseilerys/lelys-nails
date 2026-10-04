package com.lelysnails.agenda.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.lelysnails.agenda.R
import java.util.Calendar

class WidgetConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val seek = findViewById<SeekBar>(R.id.seekAlpha)
        val label = findViewById<TextView>(R.id.tvAlphaLabel)
        val btn = findViewById<Button>(R.id.btnCreateWidget)

        // SeekBar 0..70 maps to opacity 25..95
        seek.max = 70
        seek.progress = 63 // ~88%
        fun opacity() = 25 + seek.progress
        label.text = "Opacidad: ${opacity()}%"

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = "Opacidad: ${opacity()}%"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btn.setOnClickListener {
            val alpha = opacity()
            WidgetPrefs.setAlpha(this, widgetId, alpha)
            val now = Calendar.getInstance()
            WidgetPrefs.setYearMonth(
                this, widgetId,
                now.get(Calendar.YEAR), now.get(Calendar.MONTH)
            )
            val key = com.lelysnails.agenda.data.AppointmentStore.dateKey(
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
            )
            WidgetPrefs.setSelectedKey(this, widgetId, key)

            AgendaWidgetProvider.updateWidget(
                this, AppWidgetManager.getInstance(this), widgetId
            )

            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}
