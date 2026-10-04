package com.lelysnails.agenda.widget

import android.content.Intent
import android.widget.RemoteViewsService

class AgendaWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AgendaWidgetFactory(applicationContext, intent)
    }
}
