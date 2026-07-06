package com.wirewaypro.app.ui.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobCosting
import com.wirewaypro.app.domain.model.TimeEntry
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Renders an internal JOB COST REPORT (labor + materials, actual vs estimate) to
 * a clean dark-on-light PDF (US Letter) with Android's built-in [PdfDocument] —
 * no external dependency, mirroring [QuotePdfGenerator]. This is a contractor-only
 * report (not client-facing), so no branding/watermark logic: it shows the real
 * numbers — estimated vs actual, per-crew labor detail, and true profit.
 *
 * Accuracy: labor is reported in HOURS for the estimate comparison (the estimate's
 * labor dollars are a bill rate, not a cost); actual labor COST = Σ(hours × crew
 * cost rate). Nothing here is computed for show — every figure comes from
 * [costing] / [entries]. Returns the saved file, or null on any error.
 */
object JobCostPdfGenerator {

    private const val PAGE_W = 612
    private const val PAGE_H = 792
    private const val MARGIN = 48f
    private const val RIGHT = PAGE_W - MARGIN
    private const val BOTTOM = PAGE_H - MARGIN

    private const val INK = 0xFF0A0E14.toInt()
    private const val ACCENT = 0xFF3AA9FF.toInt()
    private const val MUTED = 0xFF6B7280.toInt()
    private const val HAIR = 0xFFE5E7EB.toInt()
    private const val GREEN = 0xFF16A34A.toInt()
    private const val RED = 0xFFDC2626.toInt()

    fun generate(
        context: Context,
        job: Job,
        costing: JobCosting,
        entries: List<TimeEntry>,
        businessName: String? = null,
    ): File? = runCatching {
        val doc = PdfDocument()
        val s = PageState(doc)
        s.start()

        drawHeader(s, businessName)
        drawJob(s, job)
        drawEstimateVsActual(s, costing)
        drawCrewDetail(s, entries.filter { !it.isRunning })
        drawBottomLine(s, costing)
        drawFooter(s)

        doc.finishPage(s.page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tag = (job.title.ifBlank { job.id }).replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val file = File(dir, "job-cost-$tag.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }.getOrNull()

    private class PageState(val doc: PdfDocument) {
        lateinit var page: PdfDocument.Page
        lateinit var canvas: android.graphics.Canvas
        var y = MARGIN
        private var pageNo = 0
        fun start() {
            pageNo += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
        }
        fun ensure(need: Float) {
            if (y + need > BOTTOM) { doc.finishPage(page); start() }
        }
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun money(v: Double?): String = Format.money(v)
    private fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
    private fun signedMoney(v: Double) = (if (v >= 0) "+" else "-") + money(abs(v))
    private fun signedHours(v: Double) = (if (v >= 0) "+" else "-") + trimNum(abs(v)) + " hrs"

    private fun rule(s: PageState) =
        s.canvas.drawLine(MARGIN, s.y, RIGHT, s.y, paint(HAIR, 1f).apply { strokeWidth = 1f })

    private fun drawHeader(s: PageState, businessName: String?) {
        s.canvas.drawText("WIREWAY", MARGIN, s.y + 22f, paint(INK, 24f, bold = true))
        s.canvas.drawText("PRO", MARGIN + 118f, s.y + 22f, paint(ACCENT, 16f, bold = true))
        s.canvas.drawText("JOB COST REPORT", RIGHT, s.y + 14f, paint(MUTED, 12f, bold = true, align = Paint.Align.RIGHT))
        businessName?.takeIf { it.isNotBlank() }?.let {
            s.canvas.drawText(it, RIGHT, s.y + 32f, paint(INK, 12f, align = Paint.Align.RIGHT))
        }
        s.y += 44f
        rule(s)
        s.y += 16f
    }

    private fun drawJob(s: PageState, job: Job) {
        s.canvas.drawText(job.title.ifBlank { "Job" }, MARGIN, s.y + 14f, paint(INK, 15f, bold = true))
        s.y += 22f
        val lines = listOfNotNull(
            job.clientName?.takeIf { it.isNotBlank() },
            job.jobAddress?.takeIf { it.isNotBlank() },
            job.scheduledDate?.let { "Scheduled ${Format.date(it)}" },
            job.status?.let { "Status: ${Format.status(it)}" },
        )
        lines.forEach { s.canvas.drawText(it, MARGIN, s.y + 11f, paint(MUTED, 10f)); s.y += 14f }
        s.y += 8f
        rule(s)
        s.y += 14f
    }

    private fun drawEstimateVsActual(s: PageState, c: JobCosting) {
        s.canvas.drawText("ESTIMATE VS ACTUAL", MARGIN, s.y + 10f, paint(MUTED, 9f, bold = true))
        s.y += 18f
        if (!c.hasEstimate) {
            val p = paint(MUTED, 10f)
            s.canvas.drawText("No linked estimate — showing recorded actuals only.", MARGIN, s.y + 10f, p)
            s.y += 18f
        } else {
            // Column header.
            val col2 = RIGHT - 200f
            val col3 = RIGHT - 90f
            s.canvas.drawText("Estimated", col2, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
            s.canvas.drawText("Actual", col3, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
            s.canvas.drawText("Variance", RIGHT, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
            s.y += 16f
            rule(s); s.y += 6f

            // Materials (cost vs cost).
            costRow(
                s, "Materials", money(c.estimatedMaterialCost), money(c.actualMaterialCost),
                signedMoney(c.materialVariance), over = c.materialVariance > 0.0, col2, col3,
            )
            // Labor (hours vs hours).
            costRow(
                s, "Labor (hours)", "${trimNum(c.estimatedLaborHours)} hrs",
                "${trimNum(c.actualLaborHours)} hrs", signedHours(c.laborHoursVariance),
                over = c.laborHoursVariance > 0.0, col2, col3,
            )
            s.y += 4f
            s.canvas.drawText(
                "Actual labor cost (Σ hours × crew cost rate): ${money(c.actualLaborCost)}",
                MARGIN, s.y + 10f, paint(INK, 10f),
            )
            s.y += 18f
        }
        s.y += 6f
        rule(s); s.y += 14f
    }

    private fun costRow(
        s: PageState, label: String, est: String, act: String, variance: String,
        over: Boolean, col2: Float, col3: Float,
    ) {
        s.ensure(18f)
        s.canvas.drawText(label, MARGIN, s.y + 11f, paint(INK, 11f))
        s.canvas.drawText(est, col2, s.y + 11f, paint(MUTED, 11f, align = Paint.Align.RIGHT))
        s.canvas.drawText(act, col3, s.y + 11f, paint(INK, 11f, align = Paint.Align.RIGHT))
        s.canvas.drawText(variance, RIGHT, s.y + 11f, paint(if (over) RED else GREEN, 11f, bold = true, align = Paint.Align.RIGHT))
        s.y += 18f
    }

    private fun drawCrewDetail(s: PageState, completed: List<TimeEntry>) {
        if (completed.isEmpty()) return
        s.canvas.drawText("CREW LABOR DETAIL", MARGIN, s.y + 10f, paint(MUTED, 9f, bold = true))
        s.y += 18f
        val hrsX = RIGHT - 170f
        val rateX = RIGHT - 90f
        s.canvas.drawText("HOURS", hrsX, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText("RATE", rateX, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText("COST", RIGHT, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
        s.y += 16f
        rule(s); s.y += 6f
        var totalHours = 0.0
        var totalCost = 0.0
        completed.forEach { e ->
            s.ensure(18f)
            val hrs = e.hours ?: 0.0
            totalHours += hrs
            totalCost += e.laborCost
            s.canvas.drawText((e.workerName ?: "Crew").take(40), MARGIN, s.y + 11f, paint(INK, 11f))
            s.canvas.drawText(trimNum(hrs), hrsX, s.y + 11f, paint(INK, 11f, align = Paint.Align.RIGHT))
            s.canvas.drawText(money(e.rate) + "/hr", rateX, s.y + 11f, paint(MUTED, 11f, align = Paint.Align.RIGHT))
            s.canvas.drawText(money(e.laborCost), RIGHT, s.y + 11f, paint(INK, 11f, align = Paint.Align.RIGHT))
            s.y += 17f
        }
        s.y += 4f
        s.canvas.drawLine(hrsX - 20f, s.y, RIGHT, s.y, paint(HAIR, 1f).apply { strokeWidth = 1f })
        s.y += 10f
        s.canvas.drawText("Total labor", MARGIN, s.y + 11f, paint(INK, 11f, bold = true))
        s.canvas.drawText("${trimNum(totalHours)} hrs", hrsX, s.y + 11f, paint(INK, 11f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText(money(totalCost), RIGHT, s.y + 11f, paint(INK, 11f, bold = true, align = Paint.Align.RIGHT))
        s.y += 24f
        rule(s); s.y += 14f
    }

    private fun drawBottomLine(s: PageState, c: JobCosting) {
        s.ensure(110f)
        s.canvas.drawText("BOTTOM LINE", MARGIN, s.y + 10f, paint(MUTED, 9f, bold = true))
        s.y += 20f
        val labelX = RIGHT - 160f
        fun row(label: String, value: String, color: Int = INK, bold: Boolean = false) {
            s.canvas.drawText(label, labelX, s.y + 12f, paint(MUTED, 11f, align = Paint.Align.RIGHT))
            s.canvas.drawText(value, RIGHT, s.y + 12f, paint(color, 11f, bold = bold, align = Paint.Align.RIGHT))
            s.y += 18f
        }
        row("Collected", money(c.collected))
        row("Actual materials", "-" + money(c.actualMaterialCost))
        row("Actual labor", "-" + money(c.actualLaborCost))
        s.y += 4f
        s.canvas.drawLine(labelX, s.y, RIGHT, s.y, paint(HAIR, 1f).apply { strokeWidth = 1f })
        s.y += 12f
        val profitColor = if (c.trueProfit >= 0) GREEN else RED
        s.canvas.drawText("TRUE PROFIT", labelX, s.y + 14f, paint(INK, 13f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText(money(c.trueProfit), RIGHT, s.y + 14f, paint(profitColor, 14f, bold = true, align = Paint.Align.RIGHT))
        s.y += 22f
        c.margin?.let { m ->
            s.canvas.drawText(
                "Margin: ${(m * 100).toInt()}% of collected — true profit = collected − actual costs.",
                RIGHT, s.y + 6f, paint(MUTED, 9f, align = Paint.Align.RIGHT),
            )
            s.y += 18f
        }
    }

    private fun drawFooter(s: PageState) {
        s.canvas.drawText(
            "Generated by Wireway Pro — figures reflect only what was recorded on this job.",
            MARGIN, BOTTOM + 18f, paint(MUTED, 9f),
        )
    }
}
