package com.lelysnails.agenda

import android.content.res.Configuration
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lelysnails.agenda.data.AppStyle
import com.lelysnails.agenda.data.NightMode
import com.lelysnails.agenda.data.SettingsStore
import com.lelysnails.agenda.databinding.ActivitySettingsBinding
import com.lelysnails.agenda.ui.DrawableFactory
import com.lelysnails.agenda.ui.StylePalette
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: SettingsStore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsStore(this)
        palette = resolvePalette()

        applyChrome()
        setupStyleGroup()
        setupNightGroup()
        setupWeekStart()
        setupMaxSlots()
        setupOffDays()

        binding.etSalonName.setText(settings.salonName)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveSettings.setOnClickListener { saveAndFinish() }
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
            binding.lblWeek, binding.lblSlots, binding.lblOffDays
        ).forEach { it.setTextColor(p.muted) }

        binding.etSalonName.setTextColor(p.ink)
        binding.etSalonName.setHintTextColor(p.muted)
        binding.etSalonName.background = DrawableFactory.rounded(
            binding.etSalonName, p.surface, p.line, 1.5f, p.cornerBtn
        )

        binding.btnSaveSettings.setBackgroundColor(p.primary)
        binding.btnSaveSettings.setTextColor(p.onPrimary)
        binding.tvVersion.setTextColor(p.muted)
    }

    private fun setupStyleGroup() {
        binding.rgStyle.removeAllViews()
        AppStyle.entries.forEach { style ->
            val rb = RadioButton(this).apply {
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
        NightMode.entries.forEach { mode ->
            val rb = RadioButton(this).apply {
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

    private fun saveAndFinish() {
        val styleId = (0 until binding.rgStyle.childCount)
            .map { binding.rgStyle.getChildAt(it) as RadioButton }
            .firstOrNull { it.isChecked }?.tag as? String
        if (styleId != null) settings.style = AppStyle.fromId(styleId)

        val nightId = (0 until binding.rgNight.childCount)
            .map { binding.rgNight.getChildAt(it) as RadioButton }
            .firstOrNull { it.isChecked }?.tag as? String
        if (nightId != null) settings.nightMode = NightMode.fromId(nightId)

        val weekIdx = binding.spWeekStart.selectedItemPosition
        settings.weekStartsOn = weekDayLabels[weekIdx].first

        settings.maxSlots = binding.spMaxSlots.selectedItemPosition + 1

        settings.defaultOffDays = offCheckboxes.filter { it.value.isChecked }.keys

        settings.salonName = binding.etSalonName.text?.toString()?.trim().orEmpty()

        Toast.makeText(this, "Ajustes guardados", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}
