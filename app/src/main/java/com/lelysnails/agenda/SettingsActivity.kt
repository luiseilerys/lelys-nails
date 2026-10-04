package com.lelysnails.agenda

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lelysnails.agenda.data.AppointmentStore
import com.lelysnails.agenda.data.AppStyle
import com.lelysnails.agenda.data.NightMode
import com.lelysnails.agenda.data.SettingsStore
import com.lelysnails.agenda.databinding.ActivitySettingsBinding
import com.lelysnails.agenda.ui.DrawableFactory
import com.lelysnails.agenda.ui.StylePalette
import com.lelysnails.agenda.widget.AgendaWidgetProvider
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: SettingsStore
    private lateinit var store: AppointmentStore
    private lateinit var palette: StylePalette

    private val weekDayLabels = listOf(
        Calendar.MONDAY to "Lunes",
        Calendar.TUESDAY to "Martes",
        Calendar.WEDNESDAY to "Miercoles",
        Calendar.THURSDAY to "Jueves",
        Calendar.FRIDAY to "Viernes",
        Calendar.SATURDAY to "Sabado",
        Calendar.SUNDAY to "Domingo"
    )

    private val offCheckboxes = mutableMapOf<Int, CheckBox>()
    private var servicesDraft = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsStore(this)
        store = AppointmentStore(this)
        palette = resolvePalette()
        servicesDraft = settings.getServices().toMutableList()

        applyChrome()
        setupStyleGroup()
        setupNightGroup()
        setupWeekStart()
        setupMaxSlots()
        setupOffDays()
        refreshServicesList()
        refreshBackupInfo()

        binding.etSalonName.setText(settings.salonName)
        binding.etSalonName.isFocusable = true
        binding.etSalonName.isFocusableInTouchMode = true
        binding.etNewService.isFocusable = true
        binding.etNewService.isFocusableInTouchMode = true

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveSettings.setOnClickListener { saveAndFinish() }

        binding.btnAddService.setOnClickListener {
            val name = binding.etNewService.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                toast("Escribe el nombre del servicio")
                return@setOnClickListener
            }
            if (servicesDraft.any { it.equals(name, true) }) {
                toast("Ese servicio ya existe")
                return@setOnClickListener
            }
            servicesDraft.add(name)
            binding.etNewService.setText("")
            refreshServicesList()
        }

        binding.btnExportBackup.setOnClickListener {
            try {
                val path = store.manualExport()
                refreshBackupInfo()
                toast("Respaldo guardado")
                AlertDialog.Builder(this)
                    .setTitle("Respaldo guardado")
                    .setMessage("Archivo:\n${store.backupPathHint()}\n\nRuta completa:\n$path")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                toast("Error al guardar: ${e.message}")
            }
        }

        binding.btnImportBackup.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cargar respaldo")
                .setMessage("Se reemplazaran los turnos actuales con el archivo JSON de LelysNails. ¿Continuar?")
                .setPositiveButton("Cargar") { _, _ ->
                    val ok = store.manualImport()
                    if (ok) {
                        toast("Respaldo cargado")
                        AgendaWidgetProvider.refreshAll(this)
                        refreshBackupInfo()
                        setResult(RESULT_OK)
                    } else {
                        toast("No se encontro archivo de respaldo")
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun refreshBackupInfo() {
        val info = if (store.backupExists()) {
            "Ultimo respaldo: ${store.backupLastModified()}\nCarpeta: LelysNails / agenda_latest.json\n${store.backupPathHint()}"
        } else {
            "Aun no hay respaldo. Se crea solo al guardar turnos."
        }
        binding.tvBackupInfo.text = info
        binding.tvBackupInfo.setTextColor(palette.muted)
    }

    private fun resolvePalette(): StylePalette {
        val dark = when (settings.nightMode) {
            NightMode.DARK -> true
            NightMode.LIGHT -> false
            NightMode.SYSTEM -> {
                val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                night == Configuration.UI_MODE_NIGHT_YES
            }
        }
        return StylePalette.resolve(settings.style, dark)
    }

    private fun applyChrome() {
        val p = palette
        binding.rootSettings.setBackgroundColor(p.bg)
        binding.headerSettings.background = DrawableFactory.gradient(
            binding.headerSettings, p.headerStart, p.headerEnd, 0f
        )
        binding.tvSettingsTitle.setTextColor(p.onPrimary)
        binding.btnBack.setColorFilter(p.onPrimary)

        listOf(
            binding.lblSalon, binding.lblStyle, binding.lblNight,
            binding.lblWeek, binding.lblSlots, binding.lblOffDays,
            binding.lblServices, binding.lblBackup
        ).forEach { it.setTextColor(p.muted) }

        binding.etSalonName.setTextColor(p.ink)
        binding.etSalonName.setHintTextColor(p.muted)
        binding.etSalonName.background = DrawableFactory.rounded(
            binding.etSalonName, p.surface, p.line, 1.5f, p.cornerBtn
        )
        binding.etNewService.setTextColor(p.ink)
        binding.etNewService.setHintTextColor(p.muted)
        binding.etNewService.background = DrawableFactory.rounded(
            binding.etNewService, p.surface, p.line, 1.5f, p.cornerBtn
        )

        binding.btnAddService.setBackgroundColor(p.primary)
        binding.btnAddService.setTextColor(p.onPrimary)
        binding.btnExportBackup.setBackgroundColor(p.primary)
        binding.btnExportBackup.setTextColor(p.onPrimary)
        binding.btnImportBackup.setTextColor(p.primary)
        binding.btnSaveSettings.setBackgroundColor(p.primary)
        binding.btnSaveSettings.setTextColor(p.onPrimary)
        binding.tvVersion.setTextColor(p.muted)
        binding.tvBackupInfo.setTextColor(p.muted)
    }

    private fun setupStyleGroup() {
        binding.rgStyle.removeAllViews()
        AppStyle.values().forEach { style ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = "${style.emoji}  ${style.label}"
                tag = style.id
                setTextColor(palette.ink)
                isChecked = settings.style == style
                setPadding(8, 16, 8, 16)
            }
            binding.rgStyle.addView(rb)
        }
    }

    private fun setupNightGroup() {
        binding.rgNight.removeAllViews()
        NightMode.values().forEach { mode ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = mode.label
                tag = mode.id
                setTextColor(palette.ink)
                isChecked = settings.nightMode == mode
                setPadding(8, 16, 8, 16)
            }
            binding.rgNight.addView(rb)
        }
    }

    private fun setupWeekStart() {
        val labels = weekDayLabels.map { it.second }
        binding.spWeekStart.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, labels
        )
        val idx = weekDayLabels.indexOfFirst { it.first == settings.weekStartsOn }.coerceAtLeast(0)
        binding.spWeekStart.setSelection(idx)
    }

    private fun setupMaxSlots() {
        val options = listOf("1 turno", "2 turnos", "3 turnos", "4 turnos")
        binding.spMaxSlots.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, options
        )
        binding.spMaxSlots.setSelection(settings.maxSlots - 1)
    }

    private fun setupOffDays() {
        binding.offDaysContainer.removeAllViews()
        offCheckboxes.clear()
        val current = settings.defaultOffDays
        weekDayLabels.forEach { (day, label) ->
            val cb = CheckBox(this).apply {
                text = label
                setTextColor(palette.ink)
                isChecked = day in current
                setPadding(8, 12, 8, 12)
            }
            offCheckboxes[day] = cb
            binding.offDaysContainer.addView(cb)
        }
    }

    private fun refreshServicesList() {
        binding.servicesContainer.removeAllViews()
        servicesDraft.forEach { name ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(8, 10, 8, 10)
            }
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = name
                setTextColor(palette.ink)
                textSize = 15f
            }
            val del = TextView(this).apply {
                text = "Eliminar"
                setTextColor(0xFFD9536F.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(16, 8, 8, 8)
                setOnClickListener {
                    servicesDraft.removeAll { it.equals(name, true) }
                    refreshServicesList()
                }
            }
            row.addView(tv)
            row.addView(del)
            binding.servicesContainer.addView(row)

            val line = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(palette.line)
            }
            binding.servicesContainer.addView(line)
        }
    }

    private fun saveAndFinish() {
        val checkedStyle = binding.rgStyle.checkedRadioButtonId
        if (checkedStyle != View.NO_ID) {
            val rb = binding.rgStyle.findViewById<RadioButton>(checkedStyle)
            val styleId = rb?.tag as? String
            if (styleId != null) settings.style = AppStyle.fromId(styleId)
        }

        val checkedNight = binding.rgNight.checkedRadioButtonId
        if (checkedNight != View.NO_ID) {
            val rb = binding.rgNight.findViewById<RadioButton>(checkedNight)
            val nightId = rb?.tag as? String
            if (nightId != null) settings.nightMode = NightMode.fromId(nightId)
        }

        val weekIdx = binding.spWeekStart.selectedItemPosition
        settings.weekStartsOn = weekDayLabels[weekIdx].first

        settings.maxSlots = binding.spMaxSlots.selectedItemPosition + 1
        settings.defaultOffDays = offCheckboxes.filter { it.value.isChecked }.keys
        settings.salonName = binding.etSalonName.text?.toString()?.trim().orEmpty()
        settings.setServices(servicesDraft)

        store.autoBackup()

        // Actualizar widgets con el nuevo estilo
        AgendaWidgetProvider.refreshAll(this)

        toast("Ajustes guardados \u00b7 estilo: ${settings.style.label}")
        setResult(RESULT_OK)
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
