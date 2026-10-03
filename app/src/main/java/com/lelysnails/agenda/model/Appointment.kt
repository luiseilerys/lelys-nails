package com.lelysnails.agenda.model

data class Appointment(
    val name: String = "",
    val phone: String = "",
    val service: String = "",
    val time: String = "",
    val notes: String = ""
) {
    val isFilled: Boolean get() = name.isNotBlank()
}
