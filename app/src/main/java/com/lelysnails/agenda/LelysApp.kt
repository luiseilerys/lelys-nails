package com.lelysnails.agenda

import android.app.Application
import com.lelysnails.agenda.data.AppointmentStore

class LelysApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Si no hay datos en SharedPreferences pero existe JSON, restaurar.
            // Si hay datos, actualizar el archivo de respaldo.
            AppointmentStore(this).restoreOnStartupIfNeeded()
        } catch (_: Exception) {
        }
    }
}
