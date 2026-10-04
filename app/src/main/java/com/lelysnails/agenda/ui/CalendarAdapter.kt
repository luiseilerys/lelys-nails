package com.lelysnails.agenda.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lelysnails.agenda.R
import com.lelysnails.agenda.model.DayCell
import com.lelysnails.agenda.model.DayKind

class CalendarAdapter(
    private var items: List<DayCell> = emptyList(),
    private var palette: StylePalette,
    private val onDayClick: (DayCell) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    fun submit(list: List<DayCell>, palette: StylePalette) {
        this.palette = palette
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], palette)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.dayContainer)
        private val tvDay: TextView = itemView.findViewById(R.id.tvDayNum)
        private val todayMark: View = itemView.findViewById(R.id.todayMark)
        private val dotsRow: LinearLayout = itemView.findViewById(R.id.dotsRow)
        private val dot1: View = itemView.findViewById(R.id.dot1)
        private val dot2: View = itemView.findViewById(R.id.dot2)
        private val tvOffMark: TextView = itemView.findViewById(R.id.tvOffMark)

        fun bind(cell: DayCell, p: StylePalette) {
            if (cell.kind == DayKind.BLANK) {
                container.visibility = View.INVISIBLE
                container.setOnClickListener(null)
                return
            }

            container.visibility = View.VISIBLE
            container.alpha = 1f
            tvDay.text = cell.dayOfMonth.toString()
            tvDay.paintFlags = tvDay.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tvOffMark.visibility = View.GONE
            dotsRow.visibility = View.VISIBLE

            // Hoy: marca sutil (raya corta), sin fondo fuerte
            if (cell.isToday) {
                todayMark.visibility = View.VISIBLE
                todayMark.setBackgroundColor(p.primary)
            } else {
                todayMark.visibility = View.GONE
            }

            when (cell.kind) {
                DayKind.FREE -> {
                    val stroke = when {
                        cell.isSelected -> p.primary
                        else -> p.freeStroke
                    }
                    val strokeW = if (cell.isSelected) 2.5f else 1.5f
                    container.background = DrawableFactory.rounded(
                        container, p.freeBg, stroke, strokeW, p.cornerDay
                    )
                    // Numero: color normal; hoy solo un poco del color primario (no chillante)
                    tvDay.setTextColor(
                        if (cell.isToday && !cell.isSelected) p.primary else p.ink
                    )
                    setDots(cell, p.primary, p.line)
                }
                DayKind.PARTIAL -> {
                    container.background = DrawableFactory.gradient(
                        container, p.partialStart, p.partialEnd, p.cornerDay, p.partialEnd
                    )
                    if (cell.isSelected) {
                        container.background = DrawableFactory.gradient(
                            container, p.partialStart, p.partialEnd, p.cornerDay, p.primary
                        )
                    }
                    tvDay.setTextColor(p.partialInk)
                    setDots(cell, p.partialInk, p.partialStart)
                }
                DayKind.FULL -> {
                    container.background = DrawableFactory.gradient(
                        container, p.fullStart, p.fullEnd, p.cornerDay, p.fullEnd
                    )
                    tvDay.setTextColor(p.onPrimary)
                    // En dias llenos la raya de hoy se aclara un poco
                    if (cell.isToday) todayMark.setBackgroundColor(p.onPrimary)
                    setDots(cell, p.onPrimary, p.fullStart)
                }
                DayKind.OFF -> {
                    container.background = DrawableFactory.rounded(
                        container, p.offBg, p.offStroke, 1.5f, p.cornerDay, dash = true
                    )
                    container.alpha = 0.9f
                    tvDay.setTextColor(p.offText)
                    tvDay.paintFlags = tvDay.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    dotsRow.visibility = View.GONE
                    tvOffMark.visibility = View.VISIBLE
                    tvOffMark.setTextColor(p.offText)
                    // Hoy en dia off: marca suave con color muted
                    if (cell.isToday) todayMark.setBackgroundColor(p.offText)
                }
                else -> {}
            }

            container.setOnClickListener { onDayClick(cell) }
        }

        private fun setDots(cell: DayCell, onColor: Int, offColor: Int) {
            dotsRow.visibility = View.VISIBLE
            dot1.background = DrawableFactory.oval(if (cell.slot1Filled) onColor else offColor)
            dot2.background = DrawableFactory.oval(if (cell.slot2Filled) onColor else offColor)
        }
    }
}
