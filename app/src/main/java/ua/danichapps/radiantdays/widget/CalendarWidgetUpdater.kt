package ua.danichapps.radiantdays.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateAll

class CalendarWidgetUpdater(private val context: Context) {

    suspend fun refresh() {
        CalendarWidget().updateAll(context)
    }
}
