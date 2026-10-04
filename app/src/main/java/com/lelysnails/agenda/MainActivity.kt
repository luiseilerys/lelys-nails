package com.lelysnails.agenda

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lelysnails.agenda.data.AppointmentStore
import com.lelysnails.agenda.data.NightMode
import com.lelysnails.agenda.data.SettingsStore
import com.lelysnails.agenda.databinding.ActivityMainBinding
import com.lelysnails.agenda.databinding.BottomSheetDayBinding
import com.lelysnails.agenda.databinding.DialogAppointmentFormBinding
import com.lelysnails.agenda.databinding.ItemSlotBinding
import com.lelysnails.agenda.model.Appointment
import com.lelysnails.agenda.model.DayCell
import com.lelysnails.agenda.model.DayKind
import com.lelysnails.agenda.ui.CalendarAdapter
import com.lelysnails.agenda.ui.DrawableFactory
import com.lelysnails.agenda.ui.StylePalette
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: AppointmentStore
    private lateinit var settings: SettingsStore
    private lateinit var palette: StylePalette
    private lateinit var adapter: CalendarAdapter

    private var viewYear = 0
    private var viewMonth = 0
    private var selectedKey: String? = null

    private var daySheet: BottomSheetDialog? = null
    private var formSheet: BottomSheetDialog? = null
    private var currentDaySheetBinding: BottomSheetDayBinding? = null

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { recreate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            store = AppointmentStore(this)
            settings = SettingsStore(this)
            palette = resolvePalette()

            val now = Calendar.getInstance()
            viewYear = now.get(Calendar.YEAR)
            viewMonth = now.get(Calendar.MONTH)

            adapter = CalendarAdapter(emptyList(), palette) { onDayClicked(it) }
            binding.rvCalendar.layoutManager = GridLayoutManager(this, 7)
            binding.rvCalendar.adapter = adapter
            binding.rvCalendar.itemAnimator = null
            binding.rvCalendar.isNestedScrollingEnabled = false

            setupNav()
            binding.btnSettings.setOnClickListener {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
            }

            applyThemeToChrome()
            setupWeekdays()
            setupLegend()
            render()
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

    private fun applyThemeToChrome() {
        val p = palette
        binding.rootMain.setBackgroundColor(p.bg)
        binding.scrollMain.setBackgroundColor(p.bg)

        binding.headerBar.background = DrawableFactory.gradient(
            binding.headerBar, p.headerStart, p.headerEnd, 0f
        )
        try {
            binding.ivLogo.background = DrawableFactory.rounded(
                binding.ivLogo, 0x38FFFFFF, null, 0f, p.cornerBtn
            )
            binding.ivLogo.setColorFilter(p.onPrimary)
        } catch (_: Exception) {
            binding.tvLogo.text = p.logoEmoji
            binding.tvLogo.background = DrawableFactory.rounded(
                binding.tvLogo, 0x38FFFFFF, null, 0f, p.cornerBtn
            )
        }
        binding.tvAppName.text = settings.salonName
        binding.tvAppName.setTextColor(p.onPrimary)
        binding.tvSubtitle.text = "Agenda \u00b7 ${store.maxSlots()} por dia"
        binding.tvSubtitle.setTextColor(0xE6FFFFFF.toInt())

        binding.cardCalendar.setCardBackgroundColor(p.surface)
        binding.cardCalendar.radius = p.cornerCard * resources.displayMetrics.density

        binding.tvMonthLabel.setTextColor(p.ink)
        binding.tvStatCount.setTextColor(p.primary)
        binding.tvStatFree.setTextColor(p.primary)
        binding.tvStatCountLabel.setTextColor(p.muted)
        binding.tvStatFreeLabel.setTextColor(p.muted)
        binding.tvHint.setTextColor(p.muted)

        binding.btnPrevMonth.background = DrawableFactory.rounded(
            binding.btnPrevMonth, p.bg, null, 0f, p.cornerBtn
        )
        binding.btnNextMonth.background = DrawableFactory.rounded(
            binding.btnNextMonth, p.bg, null, 0f, p.cornerBtn
        )
        binding.btnPrevMonth.setColorFilter(p.primary)
        binding.btnNextMonth.setColorFilter(p.primary)

        binding.btnToday.background = DrawableFactory.rounded(
            binding.btnToday, p.bg, null, 0f, 99f
        )
        binding.btnToday.setTextColor(p.primary)

        binding.btnSettings.setBackgroundColor(Color.TRANSPARENT)
        binding.btnSettings.alpha = 0.55f
        binding.btnSettings.setColorFilter(ColorUtils.setAlphaComponent(p.primary, 200))
    }

    private fun setupWeekdays() {
        val names = resources.getStringArray(R.array.weekdays)
        val calOrder = orderedWeekDays(settings.weekStartsOn)
        val labelMap = mapOf(
            Calendar.MONDAY to names[0],
            Calendar.TUESDAY to names[1],
            Calendar.WEDNESDAY to names[2],
            Calendar.THURSDAY to names[3],
            Calendar.FRIDAY to names[4],
            Calendar.SATURDAY to names[5],
            Calendar.SUNDAY to names[6]
        )

        binding.weekdaysRow.removeAllViews()
        calOrder.forEach { dow ->
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = labelMap[dow] ?: ""
                textSize = 10.5f
                gravity = android.view.Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (settings.isWeekdayOff(dow)) {
                    setTextColor(palette.offText)
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    setTextColor(palette.muted)
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }
            binding.weekdaysRow.addView(tv)
        }
    }

    private fun orderedWeekDays(start: Int): List<Int> {
        val all = listOf(
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
        )
        val idx = all.indexOf(start).coerceAtLeast(0)
        return all.drop(idx) + all.take(idx)
    }

    private fun setupLegend() {
        binding.legendRow.removeAllViews()
        val items = listOf(
            Triple("Libre", palette.freeBg, palette.freeStroke),
            Triple("1+", palette.partialEnd, palette.partialEnd),
            Triple("Lleno", palette.fullEnd, palette.fullEnd),
            Triple("Off", palette.offBg, palette.offStroke)
        )
        items.forEach { (label, fill, stroke) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 24, 0)
            }
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28)
                background = DrawableFactory.oval(fill, stroke, 3)
            }
            val tv = TextView(this).apply {
                text = label
                textSize = 10f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(palette.muted)
                setPadding(10, 0, 0, 0)
            }
            row.addView(dot)
            row.addView(tv)
            binding.legendRow.addView(row)
        }
    }

    private fun setupNav() {
        binding.btnPrevMonth.setOnClickListener {
            viewMonth--; if (viewMonth < 0) { viewMonth = 11; viewYear-- }; render()
        }
        binding.btnNextMonth.setOnClickListener {
            viewMonth++; if (viewMonth > 11) { viewMonth = 0; viewYear++ }; render()
        }
        binding.btnToday.setOnClickListener {
            val n = Calendar.getInstance()
            viewYear = n.get(Calendar.YEAR)
            viewMonth = n.get(Calendar.MONTH)
            render()
        }
    }

    private fun render() {
        val months = resources.getStringArray(R.array.months)
        binding.tvMonthLabel.text = "${months[viewMonth]} $viewYear"
        adapter.submit(buildCells(), palette)
        val (booked, free) = store.monthStats(viewYear, viewMonth)
        binding.tvStatCount.text = booked.toString()
        binding.tvStatFree.text = free.toString()
        binding.tvSubtitle.text = "Agenda \u00b7 ${store.maxSlots()} por dia"
    }

    private fun buildCells(): List<DayCell> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val weekOrder = orderedWeekDays(settings.weekStartsOn)
        val offset = weekOrder.indexOf(firstDow).coerceAtLeast(0)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val todayKey = AppointmentStore.dateKey(
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        )

        val list = mutableListOf<DayCell>()
        repeat(offset) { list.add(DayCell(kind = DayKind.BLANK)) }

        for (d in 1..daysInMonth) {
            val key = AppointmentStore.dateKey(viewYear, viewMonth, d)
            val defaultOff = store.isDefaultOff(viewYear, viewMonth, d)
            val customOff = store.isCustomOff(key)
            val workEx = store.isWorkException(key)
            // Dia off solo si sigue bloqueado (sin excepcion de trabajo)
            val kind = store.dayKind(key)
            list.add(
                DayCell(
                    dayOfMonth = d,
                    dateKey = key,
                    kind = kind,
                    isToday = key == todayKey,
                    isSelected = key == selectedKey,
                    isDefaultOff = defaultOff && !workEx,
                    isCustomOff = customOff,
                    slot1Filled = store.getAppointment(key, 1) != null,
                    slot2Filled = store.getAppointment(key, 2) != null
                )
            )
        }

        val total = offset + daysInMonth
        val tail = (7 - total % 7) % 7
        repeat(tail) { list.add(DayCell(kind = DayKind.BLANK)) }
        return list
    }

    private fun onDayClicked(cell: DayCell) {
        if (cell.kind == DayKind.BLANK) return

        // Domingo / miercoles (u otro fijo off) sin excepcion: confirmar
        if (store.isDefaultOffKey(cell.dateKey) && !store.isWorkException(cell.dateKey) && !store.isCustomOff(cell.dateKey)) {
            val parsed = AppointmentStore.parseKey(cell.dateKey) ?: return
            val (y, m, d) = parsed
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            val dayName = resources.getStringArray(R.array.days_long)[cal.get(Calendar.DAY_OF_WEEK) - 1]
            val months = resources.getStringArray(R.array.months)
            val dateLabel = "$d de ${months[m]} $y"
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_work_title)
                .setMessage(getString(R.string.confirm_work_message, dayName, dateLabel))
                .setPositiveButton(R.string.confirm_work_yes) { _, _ ->
                    store.setWorkException(cell.dateKey, true)
                    toast(getString(R.string.work_exception_enabled))
                    selectedKey = cell.dateKey
                    render()
                    openDaySheet(cell.dateKey)
                }
                .setNegativeButton(R.string.confirm_work_no, null)
                .show()
            return
        }

        selectedKey = cell.dateKey
        render()
        openDaySheet(cell.dateKey)
    }

    private fun expandSheet(dialog: BottomSheetDialog) {
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
        }
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun openDaySheet(dateKey: String) {
        daySheet?.dismiss()
        val sheetBinding = BottomSheetDayBinding.inflate(layoutInflater)
        currentDaySheetBinding = sheetBinding
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)
        expandSheet(dialog)
        daySheet = dialog

        val parsed = AppointmentStore.parseKey(dateKey) ?: return
        val (y, m, d) = parsed
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        val daysLong = resources.getStringArray(R.array.days_long)
        val monthsMin = resources.getStringArray(R.array.months).map { it.lowercase() }
        sheetBinding.tvSheetDayName.text = daysLong[cal.get(Calendar.DAY_OF_WEEK) - 1]
        sheetBinding.tvSheetDate.text = "$d de ${monthsMin[m]} $y"

        sheetBinding.btnCloseSheet.setOnClickListener {
            dialog.dismiss(); selectedKey = null; render()
        }
        dialog.setOnDismissListener {
            if (formSheet?.isShowing != true) {
                selectedKey = null; currentDaySheetBinding = null; render()
            }
        }
        refreshDaySheetContent(sheetBinding, dateKey)
        dialog.show()
    }

    private fun refreshDaySheetContent(sheetBinding: BottomSheetDayBinding, dateKey: String) {
        val isCustomOff = store.isCustomOff(dateKey)
        sheetBinding.slotsContainer.removeAllViews()
        val max = store.maxSlots()

        if (isCustomOff) {
            sheetBinding.offNotice.visibility = View.VISIBLE
            sheetBinding.tvSlotsHint.visibility = View.GONE
            sheetBinding.btnToggleOff.text = getString(R.string.reactivate)
        } else {
            sheetBinding.offNotice.visibility = View.GONE
            sheetBinding.tvSlotsHint.visibility = View.VISIBLE
            sheetBinding.tvSlotsHint.text = "Maximo $max clientas por dia.\nToca un turno para agendar o editar."
            sheetBinding.btnToggleOff.text = getString(R.string.mark_off)

            for (slot in 1..max) {
                val slotView = ItemSlotBinding.inflate(LayoutInflater.from(this), sheetBinding.slotsContainer, false)
                bindSlot(slotView, slot, store.getAppointment(dateKey, slot)) {
                    openFormSheet(dateKey, slot)
                }
                sheetBinding.slotsContainer.addView(slotView.root)
            }
        }

        sheetBinding.btnToggleOff.setOnClickListener { toggleOffDay(dateKey, sheetBinding) }
    }

    private fun bindSlot(b: ItemSlotBinding, slot: Int, appt: Appointment?, onClick: () -> Unit) {
        b.tvSlotNum.text = slot.toString()
        if (appt != null) {
            b.tvSlotNum.background = DrawableFactory.gradient(b.tvSlotNum, palette.primaryLight, palette.primary, palette.cornerBtn)
            b.tvSlotNum.setTextColor(palette.onPrimary)
            b.tvSlotName.text = appt.name
            b.tvSlotName.setTextColor(palette.ink)
            val parts = mutableListOf<String>()
            if (appt.time.isNotBlank()) parts.add("\uD83D\uDD50 ${appt.time}")
            if (appt.service.isNotBlank()) parts.add(appt.service)
            var meta = parts.joinToString(" \u00b7 ")
            if (appt.phone.isNotBlank()) {
                meta = if (meta.isEmpty()) "\uD83D\uDCDE ${appt.phone}" else "$meta\n\uD83D\uDCDE ${appt.phone}"
            }
            b.tvSlotMeta.text = meta
            b.tvSlotMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
            if (appt.notes.isNotBlank()) {
                b.tvSlotNotes.visibility = View.VISIBLE
                b.tvSlotNotes.text = "\uD83D\uDCDD ${appt.notes}"
            } else b.tvSlotNotes.visibility = View.GONE
            b.tvSlotAction.text = "\u270E"
        } else {
            b.tvSlotNum.background = DrawableFactory.rounded(b.tvSlotNum, palette.bg, null, 0f, palette.cornerBtn)
            b.tvSlotNum.setTextColor(palette.muted)
            b.tvSlotName.text = getString(R.string.slot_free, slot)
            b.tvSlotName.setTextColor(palette.muted)
            b.tvSlotMeta.text = getString(R.string.slot_available)
            b.tvSlotMeta.visibility = View.VISIBLE
            b.tvSlotNotes.visibility = View.GONE
            b.tvSlotAction.text = "+"
        }
        b.root.setOnClickListener { onClick() }
    }

    private fun toggleOffDay(dateKey: String, sheetBinding: BottomSheetDayBinding) {
        if (store.isCustomOff(dateKey)) {
            store.setCustomOff(dateKey, false)
            toast(getString(R.string.day_reactivated))
            refreshDaySheetContent(sheetBinding, dateKey)
            render()
            return
        }
        val booked = store.bookedCount(dateKey)
        if (booked > 0) {
            AlertDialog.Builder(this)
                .setTitle(R.string.confirm_off_title)
                .setMessage(getString(R.string.confirm_off_message, booked))
                .setPositiveButton(R.string.yes) { _, _ ->
                    store.clearDay(dateKey)
                    store.setCustomOff(dateKey, true)
                    store.setWorkException(dateKey, false)
                    toast(getString(R.string.day_marked_off))
                    refreshDaySheetContent(sheetBinding, dateKey)
                    render()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        } else {
            store.setCustomOff(dateKey, true)
            store.setWorkException(dateKey, false)
            toast(getString(R.string.day_marked_off))
            refreshDaySheetContent(sheetBinding, dateKey)
            render()
        }
    }

    private fun openFormSheet(dateKey: String, slot: Int) {
        formSheet?.dismiss()
        val formBinding = DialogAppointmentFormBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(formBinding.root)
        expandSheet(dialog)
        formSheet = dialog

        formBinding.tvFormTitle.text = "Turno $slot"

        val serviceList = mutableListOf("Elegir...")
        serviceList.addAll(settings.getServices())
        formBinding.spService.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, serviceList
        )

        val existing = store.getAppointment(dateKey, slot)
        if (existing != null) {
            formBinding.etName.setText(existing.name)
            formBinding.etPhone.setText(existing.phone)
            formBinding.etTime.setText(existing.time)
            formBinding.etNotes.setText(existing.notes)
            val idx = serviceList.indexOfFirst { it.equals(existing.service, true) }.coerceAtLeast(0)
            formBinding.spService.setSelection(idx)
            formBinding.btnDelete.visibility = View.VISIBLE
            formBinding.btnMove.visibility = View.VISIBLE
        } else {
            formBinding.btnDelete.visibility = View.GONE
            formBinding.btnMove.visibility = View.GONE
        }

        formBinding.etName.isFocusable = true
        formBinding.etName.isFocusableInTouchMode = true
        formBinding.etPhone.isFocusable = true
        formBinding.etPhone.isFocusableInTouchMode = true
        formBinding.etNotes.isFocusable = true
        formBinding.etNotes.isFocusableInTouchMode = true

        formBinding.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val parts = formBinding.etTime.text?.toString()?.split(":")
            val h = parts?.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
            val min = parts?.getOrNull(1)?.toIntOrNull() ?: 0
            TimePickerDialog(this, { _, hour, minute ->
                formBinding.etTime.setText("%02d:%02d".format(hour, minute))
            }, h, min, true).show()
        }

        formBinding.btnBackForm.setOnClickListener { dialog.dismiss() }

        formBinding.btnSave.setOnClickListener {
            val name = formBinding.etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                formBinding.etName.error = getString(R.string.name_required)
                formBinding.etName.requestFocus()
                return@setOnClickListener
            }
            val serviceSel = formBinding.spService.selectedItem?.toString().orEmpty()
            store.saveAppointment(
                dateKey, slot,
                Appointment(
                    name = name,
                    phone = formBinding.etPhone.text?.toString()?.trim().orEmpty(),
                    service = if (serviceSel == "Elegir...") "" else serviceSel,
                    time = formBinding.etTime.text?.toString()?.trim().orEmpty(),
                    notes = formBinding.etNotes.text?.toString()?.trim().orEmpty()
                )
            )
            toast(getString(R.string.saved))
            dialog.dismiss()
            currentDaySheetBinding?.let { refreshDaySheetContent(it, dateKey) }
            render()
        }

        formBinding.btnMove.setOnClickListener {
            promptMoveAppointment(dateKey, slot, dialog)
        }

        formBinding.btnDelete.setOnClickListener {
            store.deleteAppointment(dateKey, slot)
            toast(getString(R.string.deleted))
            dialog.dismiss()
            currentDaySheetBinding?.let { refreshDaySheetContent(it, dateKey) }
            render()
        }

        dialog.show()
        formBinding.etName.post { formBinding.etName.requestFocus() }
    }

    private fun promptMoveAppointment(fromKey: String, fromSlot: Int, formDialog: BottomSheetDialog) {
        val parsed = AppointmentStore.parseKey(fromKey) ?: return
        val (y, m, d) = parsed
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val toKey = AppointmentStore.dateKey(year, month, dayOfMonth)
            // Si destino es off fijo, pedir excepcion primero no; solo permitir si ya es laborable o forzar
            if (store.isOff(toKey)) {
                toast("Ese dia no es laborable. Habilitalo antes tocandolo en el calendario.")
                return@DatePickerDialog
            }
            val msg = store.moveAppointment(fromKey, fromSlot, toKey)
            toast(msg)
            if (msg.startsWith("Turno trasladado")) {
                formDialog.dismiss()
                daySheet?.dismiss()
                selectedKey = toKey
                viewYear = year
                viewMonth = month
                render()
                openDaySheet(toKey)
            }
        }, y, m, d).show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "LelysNails"
    }
}
