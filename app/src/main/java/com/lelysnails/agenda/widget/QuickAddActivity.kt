package com.lelysnails.agenda.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lelysnails.agenda.R
import com.lelysnails.agenda.data.AppointmentStore
import com.lelysnails.agenda.model.Appointment
import java.util.Calendar

class QuickAddActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_add)

        val dateKey = intent.getStringExtra(AgendaWidgetProvider.EXTRA_DATE_KEY)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (dateKey.isNullOrBlank()) {
            finish()
            return
        }

        val store = AppointmentStore(this)
        if (store.isOff(dateKey)) {
            Toast.makeText(this, "Dia no laborable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val free = store.findFreeSlot(dateKey)
        if (free == null) {
            Toast.makeText(this, "No hay espacios libres", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val parsed = AppointmentStore.parseKey(dateKey)
        val tvDate = findViewById<TextView>(R.id.tvQuickDate)
        if (parsed != null) {
            val (y, m, d) = parsed
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            val days = resources.getStringArray(R.array.days_long)
            val months = resources.getStringArray(R.array.months)
            tvDate.text = "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]} $d de ${months[m]} $y · Turno $free"
        } else {
            tvDate.text = dateKey
        }

        val etName = findViewById<EditText>(R.id.etQuickName)
        etName.requestFocus()

        findViewById<Button>(R.id.btnQuickCancel).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnQuickSave).setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                etName.error = getString(R.string.name_required)
                return@setOnClickListener
            }
            store.saveAppointment(
                dateKey, free,
                Appointment(name = name, phone = "", service = "", time = "", notes = "")
            )
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
            AgendaWidgetProvider.refreshAll(this)
            finish()
        }
    }
}
