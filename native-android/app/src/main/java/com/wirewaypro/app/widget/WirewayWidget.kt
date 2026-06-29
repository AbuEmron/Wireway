package com.wirewaypro.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.wirewaypro.app.MainActivity
import com.wirewaypro.app.data.widget.WidgetSnapshot
import dagger.hilt.android.EntryPointAccessors
import java.text.NumberFormat
import java.util.Locale

// Brand palette (mirrors ui/theme/Color.kt — Glance can't read the Compose theme).
private val Bg = Color(0xFF0E1420)
private val Accent = Color(0xFF3AA9FF)
private val TextPrimary = Color(0xFFE8EDF5)
private val TextSecondary = Color(0xFF9BAAC0)

/**
 * Home-screen widget: this month's collected, unpaid-invoice count, and today's
 * job count. Reads the cached [WidgetSnapshot] from DataStore (written by
 * WidgetUpdater when the app is foregrounded). Tapping anywhere opens the app.
 *
 * Laid out as stacked stat blocks (value over label) so it needs no weighted
 * rows — keeps the Glance layout simple and resilient across widget sizes.
 */
class WirewayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .widgetSnapshotStore()
        val snapshot = store.current()
        provideContent { WidgetBody(snapshot) }
    }
}

@Composable
private fun WidgetBody(s: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Bg))
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = "Wireway",
            style = TextStyle(
                color = ColorProvider(Accent),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
        Spacer(GlanceModifier.height(10.dp))
        StatBlock(if (s.hasData) money(s.collectedThisMonth) else "—", "Collected this month")
        Spacer(GlanceModifier.height(8.dp))
        StatBlock(if (s.hasData) s.unpaidInvoices.toString() else "—", "Unpaid invoices")
        Spacer(GlanceModifier.height(8.dp))
        StatBlock(if (s.hasData) s.todayJobs.toString() else "—", "Jobs today")
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(TextPrimary),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(TextSecondary), fontSize = 11.sp),
        )
    }
}

private fun money(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)
