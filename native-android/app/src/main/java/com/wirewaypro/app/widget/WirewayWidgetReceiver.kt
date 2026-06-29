package com.wirewaypro.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The AppWidget host binds to this receiver. Glance does the rest — it renders
 * [WirewayWidget] and routes update/resize callbacks to it.
 */
class WirewayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WirewayWidget()
}
