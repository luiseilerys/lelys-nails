package com.lelysnails.agenda.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.lelysnails.agenda.R
import com.lelysnails.agenda.model.DayCell
import com.lelysnails.agenda.model.DayKind

class CalendarAdapter(
    private var items: List<DayCell> = emptyList(),
    private val onDayClick: (DayCell) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.VH>() {

    fun submit(list: List<DayCell>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.dayContainer)
        private val tvDay: TextView = itemView.findViewById(R.id.tvDayNum)
        private val dotsRow: LinearLayout = itemView.findViewById(R.id.dotsRow)
        private val dot1: View = itemView.findViewById(R.id.dot1)
        private val dot2: View = itemView.findViewById(R.id.dot2)

        fun bind(cell: DayCell) {
            if (cell.kind == DayKind.BLANK) {
                container.visibility = View.INVISIBLE
                container.setOnClickListener(null)
                return
            }

            container.visibility = View.VISIBLE
            tvDay.text = cell.dayOfMonth.toString()
            tvDay.paintFlags = tvDay.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            val ctx = itemView.context

            when (cell.kind) {
                DayKind.FREE -> {
                    container.setBackgroundResource(
                        if (cell.isSelected) R.drawable.bg_day_selected else R.drawable.bg_day_free
                    )
                    tvDay.setTextColor(
                        ContextCompat.getColor(
                            ctx,
                            if (cell.isToday) R.color.rose else R.color.ink
                        )
                    )
                    setDots(cell, R.drawable.bg_dot_on, R.drawable.bg_dot_off)
                }
                DayKind.PARTIAL -> {
                    container.setBackgroundResource(R.drawable.bg_day_partial)
                    tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.yellow_ink))
                    setDots(cell, R.drawable.bg_dot_on_yellow, R.drawable.bg_dot_off)
                }
                DayKind.FULL -> {
                    container.setBackgroundResource(R.drawable.bg_day_full)
                    tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                    setDots(cell, R.drawable.bg_dot_on_white, R.drawable.bg_dot_off)
                }
                DayKind.OFF -> {
                    container.setBackgroundResource(R.drawable.bg_day_off)
                    tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.off_text))
                    tvDay.paintFlags = tvDay.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    setDots(cell, R.drawable.bg_dot_off, R.drawable.bg_dot_off)
                }
                else -> {}
            }

            if (cell.isSelected && cell.kind != DayKind.PARTIAL && cell.kind != DayKind.FULL) {
                container.setBackgroundResource(R.drawable.bg_day_selected)
            }

            container.setOnClickListener { onDayClick(cell) }
        }

        private fun setDots(cell: DayCell, onRes: Int, offRes: Int) {
            dotsRow.visibility = View.VISIBLE
            dot1.setBackgroundResource(if (cell.slot1Filled) onRes else offRes)
            dot2.setBackgroundResource(if (cell.slot2Filled) onRes else offRes)
        }
    }
}
