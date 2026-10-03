package com.lelysnails.agenda

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lelysnails.agenda.data.AppointmentStore
import com.lelysnails.agenda.databinding.ActivityMainBinding
import com.lelysnails.agenda.databinding.BottomSheetDayBinding
import com.lelysnails.agenda.databinding.DialogAppointmentFormBinding
import com.lelysnails.agenda.databinding.ItemSlotBinding
import com.lelysnails.agenda.model.Appointment
import com.lelysnails.agenda.model.DayCell
import com.lelysnails.agenda.model.DayKind
import com.lelysnails.agenda.ui.CalendarAdapter
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: AppointmentStore
    private lateinit var adapter: CalendarAdapter

    private var viewYear = 0
    private var viewMonth = 0 // 0-based
    private var selectedKey: String? = null

    private var daySheet: BottomSheetDialog? = null
    private var formSheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = AppointmentStore(this)

        val now = Calendar.getInstance()
        viewYear = now.get(Calendar.YEAR)
        viewMonth = now.get(Calendar.MONTH)

        setupWeekdays()
        setupCalendar()
        setupNav()

        render()
    }

    private fun setupWeekdays() {
        val names = resources.getStringArray(R.array.weekdays)
        binding.weekdaysRow.removeAllViews()
        names.forEachIndexed { i, name ->
            val tv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = name
                textSize = 10.5f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                // Mié (2) y Dom (6) tachados
                if (i == 2 || i == 6) {
                    setTextColor(ContextCompat.getColor(context, R.color.off_text))
                    paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.muted))
                }
            }
            binding.weekdaysRow.addView(tv)
        }
    }

    private fun setupCalendar() {
        adapter = CalendarAdapter { cell -> onDayClicked(cell) }
        binding.rvCalendar.layoutManager = GridLayoutManager(this, 7)
        binding.rvCalendar.adapter = adapter
        binding.rvCalendar.itemAnimator = null
    }

    private fun setupNav() {
        binding.btnPrevMonth.setOnClickListener {
            viewMonth--
            if (viewMonth < 0) {
                viewMonth = 11
                viewYear--
            }
            render()
        }
        binding.btnNextMonth.setOnClickListener {
            viewMonth++
            if (viewMonth > 11) {
                viewMonth = 0
                viewYear++
            }
            render()
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

        val cells = buildCells()
        adapter.submit(cells)

        val (booked, free) = store.monthStats(viewYear, viewMonth)
        binding.tvStatCount.text = booked.toString()
        binding.tvStatFree.text = free.toString()
    }

    private fun buildCells(): List<DayCell> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Lunes = 0
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Dom .. 7=Sáb
        val offset = (firstDow + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()
        val todayKey = AppointmentStore.dateKey(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        )

        val list = mutableListOf<DayCell>()

        repeat(offset) { list.add(DayCell(kind = DayKind.BLANK)) }

        for (d in 1..daysInMonth) {
            val key = AppointmentStore.dateKey(viewYear, viewMonth, d)
            val defaultOff = store.isDefaultOff(viewYear, viewMonth, d)
            val customOff = store.isCustomOff(key)
            val kind = when {
                defaultOff || customOff -> DayKind.OFF
                else -> store.dayKind(key)
            }
            val s1 = store.getAppointment(key, 1) != null
            val s2 = store.getAppointment(key, 2) != null

            list.add(
                DayCell(
                    dayOfMonth = d,
                    dateKey = key,
                    kind = kind,
                    isToday = key == todayKey,
                    isSelected = key == selectedKey,
                    isDefaultOff = defaultOff,
                    isCustomOff = customOff,
                    slot1Filled = s1,
                    slot2Filled = s2
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

        if (cell.isDefaultOff) {
            toast(getString(R.string.day_off_msg))
            return
        }

        selectedKey = cell.dateKey
        render()
        openDaySheet(cell.dateKey)
    }

    // ==================== DAY SHEET ====================

    private fun openDaySheet(dateKey: String) {
        daySheet?.dismiss()

        val sheetBinding = BottomSheetDayBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)
        daySheet = dialog

        val parsed = AppointmentStore.parseKey(dateKey) ?: return
        val (y, m, d) = parsed
        val cal = Calendar.getInstance().apply { set(y, m, d) }
        val daysLong = resources.getStringArray(R.array.days_long)
        val monthsMin = resources.getStringArray(R.array.months).map { it.lowercase() }

        sheetBinding.tvSheetDayName.text = daysLong[cal.get(Calendar.DAY_OF_WEEK) - 1]
        sheetBinding.tvSheetDate.text = "$d de ${monthsMin[m]} $y"

        sheetBinding.btnCloseSheet.setOnClickListener {
            dialog.dismiss()
            selectedKey = null
            render()
        }

        dialog.setOnDismissListener {
            if (formSheet?.isShowing != true) {
                selectedKey = null
                render()
            }
        }

        refreshDaySheetContent(sheetBinding, dateKey)
        dialog.show()
    }

    private fun refreshDaySheetContent(sheetBinding: BottomSheetDayBinding, dateKey: String) {
        val isCustomOff = store.isCustomOff(dateKey)
        val slotsContainer = sheetBinding.slotsContainer
        slotsContainer.removeAllViews()

        if (isCustomOff) {
            sheetBinding.offNotice.visibility = View.VISIBLE
            sheetBinding.tvSlotsHint.visibility = View.GONE
            sheetBinding.btnToggleOff.text = getString(R.string.reactivate)
            sheetBinding.btnToggleOff.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            sheetBinding.offNotice.visibility = View.GONE
            sheetBinding.tvSlotsHint.visibility = View.VISIBLE
            sheetBinding.btnToggleOff.text = getString(R.string.mark_off)
            sheetBinding.btnToggleOff.setTextColor(ContextCompat.getColor(this, R.color.muted))

            for (slot in 1..2) {
                val slotView = ItemSlotBinding.inflate(LayoutInflater.from(this), slotsContainer, false)
                val appt = store.getAppointment(dateKey, slot)
                bindSlot(slotView, slot, appt) {
                    openFormSheet(dateKey, slot)
                }
                slotsContainer.addView(slotView.root)
            }
        }

        sheetBinding.btnToggleOff.setOnClickListener {
            toggleOffDay(dateKey, sheetBinding)
        }
    }

    private fun bindSlot(b: ItemSlotBinding, slot: Int, appt: Appointment?, onClick: () -> Unit) {
        b.tvSlotNum.text = slot.toString()
        if (appt != null) {
            b.tvSlotNum.setBackgroundResource(R.drawable.bg_slot_num_filled)
            b.tvSlotNum.setTextColor(ContextCompat.getColor(this, R.color.white))
            b.tvSlotName.text = appt.name
            b.tvSlotName.setTextColor(ContextCompat.getColor(this, R.color.ink))

            val parts = mutableListOf<String>()
            if (appt.time.isNotBlank()) parts.add("🕐 ${appt.time}")
            if (appt.service.isNotBlank()) parts.add(appt.service)
            var meta = parts.joinToString(" · ")
            if (appt.phone.isNotBlank()) {
                meta = if (meta.isEmpty()) "📞 ${appt.phone}" else "$meta\n📞 ${appt.phone}"
            }
            b.tvSlotMeta.text = meta.ifEmpty { "" }
            b.tvSlotMeta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

            if (appt.notes.isNotBlank()) {
                b.tvSlotNotes.visibility = View.VISIBLE
                b.tvSlotNotes.text = "📝 ${appt.notes}"
            } else {
                b.tvSlotNotes.visibility = View.GONE
            }
            b.tvSlotAction.text = "✎"
        } else {
            b.tvSlotNum.setBackgroundResource(R.drawable.bg_slot_num)
            b.tvSlotNum.setTextColor(ContextCompat.getColor(this, R.color.muted))
            b.tvSlotName.text = getString(R.string.slot_free, slot)
            b.tvSlotName.setTextColor(ContextCompat.getColor(this, R.color.muted))
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
                    toast(getString(R.string.day_marked_off))
                    refreshDaySheetContent(sheetBinding, dateKey)
                    render()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        } else {
            store.setCustomOff(dateKey, true)
            toast(getString(R.string.day_marked_off))
            refreshDaySheetContent(sheetBinding, dateKey)
            render()
        }
    }

    // ==================== FORM SHEET ====================

    private fun openFormSheet(dateKey: String, slot: Int) {
        formSheet?.dismiss()

        val formBinding = DialogAppointmentFormBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(formBinding.root)
        formSheet = dialog

        formBinding.tvFormTitle.text = "Turno $slot"

        val services = resources.getStringArray(R.array.services)
        formBinding.spService.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            services
        )

        val existing = store.getAppointment(dateKey, slot)
        if (existing != null) {
            formBinding.etName.setText(existing.name)
            formBinding.etPhone.setText(existing.phone)
            formBinding.etTime.setText(existing.time)
            formBinding.etNotes.setText(existing.notes)
            val idx = services.indexOf(existing.service).takeIf { it >= 0 } ?: 0
            formBinding.spService.setSelection(idx)
            formBinding.btnDelete.visibility = View.VISIBLE
        } else {
            formBinding.btnDelete.visibility = View.GONE
        }

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
            val service = if (serviceSel == services[0]) "" else serviceSel

            store.saveAppointment(
                dateKey, slot,
                Appointment(
                    name = name,
                    phone = formBinding.etPhone.text?.toString()?.trim().orEmpty(),
                    service = service,
                    time = formBinding.etTime.text?.toString()?.trim().orEmpty(),
                    notes = formBinding.etNotes.text?.toString()?.trim().orEmpty()
                )
            )
            toast(getString(R.string.saved))
            dialog.dismiss()
            // refrescar day sheet
            daySheet?.let { ds ->
                val sb = BottomSheetDayBinding.bind(ds.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!)
                // más simple: cerrar y reabrir day sheet
            }
            daySheet?.dismiss()
            openDaySheet(dateKey)
            render()
        }

        formBinding.btnDelete.setOnClickListener {
            store.deleteAppointment(dateKey, slot)
            toast(getString(R.string.deleted))
            dialog.dismiss()
            daySheet?.dismiss()
            openDaySheet(dateKey)
            render()
        }

        dialog.show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
