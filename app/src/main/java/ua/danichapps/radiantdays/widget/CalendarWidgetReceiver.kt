package ua.danichapps.radiantdays.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver entry point for the home-screen widget.
 *
 * Declared in AndroidManifest with the `android.appwidget.action.APPWIDGET_UPDATE`
 * intent filter and the `@xml/calendar_widget_info` meta-data.
 */
class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}
