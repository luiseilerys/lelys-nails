package com.lelysnails.agenda.model

enum class DayKind {
    FREE, PARTIAL, FULL, OFF, BLANK
}

data class DayCell(
    val dayOfMonth: Int = 0,
    val dateKey: String = "",
    val kind: DayKind = DayKind.BLANK,
    val isToday: Boolean = false,
    val isSelected: Boolean = false,
    val isDefaultOff: Boolean = false,
    val isCustomOff: Boolean = false,
    val slot1Filled: Boolean = false,
    val slot2Filled: Boolean = false
)
