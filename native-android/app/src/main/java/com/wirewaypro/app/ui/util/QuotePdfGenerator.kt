package com.wirewaypro.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.wirewaypro.app.domain.model.BusinessInfo
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.RateMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a quote/invoice to a clean dark-on-light PDF (US Letter) with Android's
 * built-in [PdfDocument] — no external dependency. Brand header, client block,
 * catalog + custom line items, totals breakdown and notes; paginates if the
 * line-item list overflows a page. Returns the saved file, or null on any error.
 */
object QuotePdfGenerator {

    private const val PAGE_W = 612 // 8.5in * 72
    private const val PAGE_H = 792 // 11in * 72
    private const val MARGIN = 48f
    private const val RIGHT = PAGE_W - MARGIN
    private const val BOTTOM = PAGE_H - MARGIN

    private const val INK = 0xFF0A0E14.toInt()
    private const val ACCENT = 0xFF3AA9FF.toInt()
    private const val MUTED = 0xFF6B7280.toInt()
    private const val HAIR = 0xFFE5E7EB.toInt()

    private const val DEFAULT_TERMS =
        "This proposal is valid for 30 days from the date above. All work will be performed " +
            "in accordance with NEC 2023 and applicable local codes. Any changes to the scope " +
            "of work require a written change order. Permits and inspection fees are included " +
            "unless otherwise noted. Warranty: one (1) year on workmanship from date of completion."

    fun generate(
        context: Context,
        quote: QuoteDetail,
        business: BusinessInfo? = null,
        logo: Bitmap? = null,
        accent: Int? = null,
        financingLink: String? = null,
        watermark: Boolean = false,
    ): File? = runCatching {
        val doc = PdfDocument()
        val state = PageState(doc, accent ?: ACCENT, watermark)
        state.start()

        drawHeader(state, quote)
        if (business != null) drawBusiness(state, business, logo)
        drawClient(state, quote)
        drawLineItems(state, quote)
        drawTotals(state, quote)
        // Only for estimates the client hasn't yet accepted, and only when the
        // contractor supplied a real financing link (never faked).
        if (!quote.isInvoice) {
            financingLink?.takeIf { it.isNotBlank() }?.let { drawFinancing(state, it) }
        }
        drawNotes(state, quote)
        drawTerms(state)
        drawSignature(state, business, quote)
        drawFooter(state)

        doc.finishPage(state.page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val kind = if (quote.isInvoice) "invoice" else "estimate"
        val tag = quote.quoteNumber?.takeIf { it.isNotBlank() } ?: quote.id.take(8)
        val file = File(dir, "$kind-${tag.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        file
    }.getOrNull()

    // ── Page bookkeeping ───────────────────────────────────────────────────────
    private class PageState(val doc: PdfDocument, val accent: Int, val watermark: Boolean = false) {
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = MARGIN
        private var pageNo = 0

        fun start() {
            pageNo += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            canvas = page.canvas
            y = MARGIN
            // Drawn first so the page content renders over it.
            if (watermark) drawWatermark(canvas)
        }

        /** Ensure [need] points of vertical space; start a new page if not. */
        fun ensure(need: Float) {
            if (y + need > BOTTOM) {
                doc.finishPage(page)
                start()
            }
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

    // ── Sections ───────────────────────────────────────────────────────────────
    private fun drawHeader(s: PageState, q: QuoteDetail) {
        s.canvas.drawText("WIREWAY", MARGIN, s.y + 22f, paint(INK, 24f, bold = true))
        s.canvas.drawText("PRO", MARGIN + 118f, s.y + 22f, paint(s.accent, 16f, bold = true))

        val kind = if (q.isInvoice) "INVOICE" else "ESTIMATE"
        s.canvas.drawText(kind, RIGHT, s.y + 14f, paint(MUTED, 12f, bold = true, align = Paint.Align.RIGHT))
        q.quoteNumber?.takeIf { it.isNotBlank() }?.let {
            s.canvas.drawText("#$it", RIGHT, s.y + 32f, paint(INK, 14f, align = Paint.Align.RIGHT))
        }
        s.y += 44f
        rule(s)
        s.y += 16f
    }

    private fun drawBusiness(s: PageState, business: BusinessInfo, logo: Bitmap? = null) {
        val yTop = s.y
        val name = business.name?.takeIf { it.isNotBlank() }
        if (name != null) {
            s.canvas.drawText(name, MARGIN, s.y + 13f, paint(INK, 13f, bold = true))
            s.y += 18f
        }
        val contact = listOfNotNull(
            business.address?.takeIf { it.isNotBlank() },
            business.phone?.takeIf { it.isNotBlank() },
            business.email?.takeIf { it.isNotBlank() },
            business.license?.takeIf { it.isNotBlank() }?.let { "License #$it" },
            business.website?.takeIf { it.isNotBlank() },
        )
        contact.forEach { line ->
            s.canvas.drawText(line, MARGIN, s.y + 11f, paint(MUTED, 10f))
            s.y += 14f
        }
        // Business logo, right-aligned beside the text block (best-effort).
        val hasLogo = logo != null && logo.width > 0 && logo.height > 0
        if (hasLogo) {
            val maxW = 140f
            val maxH = 56f
            val scale = minOf(maxW / logo!!.width, maxH / logo.height)
            val w = logo.width * scale
            val h = logo.height * scale
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            s.canvas.drawBitmap(logo, null, RectF(RIGHT - w, yTop, RIGHT, yTop + h), bmpPaint)
            if (yTop + h + 8f > s.y) s.y = yTop + h + 8f
        }
        if (name != null || contact.isNotEmpty() || hasLogo) {
            s.y += 8f
            rule(s)
            s.y += 14f
        }
    }

    private fun drawFinancing(s: PageState, link: String) {
        s.ensure(48f)
        s.canvas.drawText("FINANCING AVAILABLE", MARGIN, s.y + 10f, paint(s.accent, 9f, bold = true))
        s.y += 18f
        val p = paint(INK, 10f)
        val text = "Prefer to spread this out? Flexible monthly payment options are available " +
            "for this project — apply in minutes here: $link"
        wrap(text, p, RIGHT - MARGIN).forEach { line ->
            s.ensure(14f)
            s.canvas.drawText(line, MARGIN, s.y + 10f, p)
            s.y += 14f
        }
        s.y += 10f
    }

    private fun drawTerms(s: PageState) {
        s.ensure(60f)
        s.canvas.drawText("TERMS", MARGIN, s.y + 10f, paint(MUTED, 9f, bold = true))
        s.y += 18f
        val p = paint(MUTED, 9f)
        wrap(DEFAULT_TERMS, p, RIGHT - MARGIN).forEach { line ->
            s.ensure(13f)
            s.canvas.drawText(line, MARGIN, s.y + 10f, p)
            s.y += 13f
        }
        s.y += 8f
    }

    private fun drawSignature(s: PageState, business: BusinessInfo?, q: QuoteDetail? = null) {
        s.ensure(70f)
        val who = business?.name?.takeIf { it.isNotBlank() } ?: "the contractor"
        // Already accepted: print the recorded acceptance instead of blank lines.
        val sig = q?.sigName?.takeIf { it.isNotBlank() }
        if (sig != null) {
            val date = q.signedAt?.take(10)
            wrap(
                "Accepted by $sig" + (date?.let { " on $it" } ?: "") +
                    " \u2014 signature recorded in person in Wireway Pro.",
                paint(INK, 10f),
                RIGHT - MARGIN,
            ).forEach { line ->
                s.canvas.drawText(line, MARGIN, s.y + 11f, paint(INK, 10f, bold = true))
                s.y += 14f
            }
            s.y += 10f
            return
        }
        wrap(
            "By signing below, you authorize $who to proceed with the work described above.",
            paint(INK, 10f),
            RIGHT - MARGIN,
        ).forEach { line ->
            s.canvas.drawText(line, MARGIN, s.y + 11f, paint(INK, 10f))
            s.y += 14f
        }
        s.y += 28f
        // Signature + date lines.
        val sigEnd = MARGIN + 240f
        val dateStart = sigEnd + 40f
        s.canvas.drawLine(MARGIN, s.y, sigEnd, s.y, paint(INK, 1f).apply { strokeWidth = 1f })
        s.canvas.drawLine(dateStart, s.y, RIGHT, s.y, paint(INK, 1f).apply { strokeWidth = 1f })
        s.y += 12f
        s.canvas.drawText("Signature", MARGIN, s.y + 9f, paint(MUTED, 9f))
        s.canvas.drawText("Date", dateStart, s.y + 9f, paint(MUTED, 9f))
        s.y += 18f
    }

    private fun drawClient(s: PageState, q: QuoteDetail) {
        val labelP = paint(MUTED, 9f, bold = true)
        val valueP = paint(INK, 12f)
        q.jobName?.takeIf { it.isNotBlank() }?.let {
            s.canvas.drawText("JOB", MARGIN, s.y, labelP)
            s.canvas.drawText(it, MARGIN, s.y + 15f, valueP)
            s.y += 30f
        }
        val lines = listOfNotNull(q.clientName, q.clientEmail, q.clientPhone)
        if (lines.isNotEmpty()) {
            s.canvas.drawText("BILL TO", MARGIN, s.y, labelP)
            s.y += 15f
            lines.forEach { line ->
                s.canvas.drawText(line, MARGIN, s.y, valueP)
                s.y += 15f
            }
        }
        s.y += 10f
    }

    private fun drawLineItems(s: PageState, q: QuoteDetail) {
        if (q.lineItems.isEmpty()) return
        drawItemsHeader(s)
        val descP = paint(INK, 11f)
        val qtyP = paint(INK, 11f, align = Paint.Align.RIGHT)
        val amtP = paint(INK, 11f, align = Paint.Align.RIGHT)
        val qtyX = RIGHT - 110f
        q.lineItems.forEach { item ->
            s.ensure(20f)
            if (s.y == MARGIN) drawItemsHeader(s) // repeated header after a page break
            val prefix = if (item.kind == "mileage") "* " else ""
            val label = "$prefix${item.label}".take(64)
            s.canvas.drawText(label, MARGIN, s.y + 12f, descP)
            s.canvas.drawText(trimNum(item.quantity), qtyX, s.y + 12f, qtyP)
            s.canvas.drawText(item.amount?.let { money(it) } ?: "—", RIGHT, s.y + 12f, amtP)
            s.y += 18f
        }
        s.y += 6f
        rule(s)
        s.y += 12f
    }

    private fun drawItemsHeader(s: PageState) {
        val h = paint(MUTED, 9f, bold = true)
        s.canvas.drawText("DESCRIPTION", MARGIN, s.y + 10f, h)
        s.canvas.drawText("QTY", RIGHT - 110f, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText("AMOUNT", RIGHT, s.y + 10f, paint(MUTED, 9f, bold = true, align = Paint.Align.RIGHT))
        s.y += 16f
        rule(s)
        s.y += 8f
    }

    private fun drawTotals(s: PageState, q: QuoteDetail) {
        s.ensure(120f)
        val labelP = paint(MUTED, 11f, align = Paint.Align.RIGHT)
        val valueP = paint(INK, 11f, align = Paint.Align.RIGHT)
        val labelX = RIGHT - 140f
        fun row(label: String, value: Double?) {
            if (value == null) return
            s.canvas.drawText(label, labelX, s.y + 12f, labelP)
            s.canvas.drawText(money(value), RIGHT, s.y + 12f, valueP)
            s.y += 17f
        }
        if (q.rateMode == RateMode.HOURLY) {
            val rate = q.hourlyRate?.let { " @ ${money(it)}/hr" } ?: ""
            val labor = (q.totalHours ?: 0.0) * (q.hourlyRate ?: 0.0)
            row(q.totalHours?.let { "Labor (${trimNum(it)} hrs$rate)" } ?: "Labor", labor)
            if (!q.clientBuysAll) {
                if (q.showMaterials) row("Materials", q.totalMaterial)
                if (q.taxEnabled) row("Tax", (q.totalMaterial ?: 0.0) * (q.taxRate ?: 0.0))
            }
        } else {
            if (q.showMaterials) row("Materials", q.totalMaterial)
            row(q.totalHours?.let { "Labor (${trimNum(it)} hrs)" } ?: "Labor", q.totalLabor)
            row("Markup", q.totalMarkup)
            if (q.taxEnabled) row("Tax", q.totalTax)
        }
        s.y += 6f
        s.canvas.drawLine(labelX, s.y, RIGHT, s.y, paint(HAIR, 1f).apply { strokeWidth = 1f })
        s.y += 12f
        s.canvas.drawText("TOTAL", labelX, s.y + 14f, paint(INK, 13f, bold = true, align = Paint.Align.RIGHT))
        s.canvas.drawText(money(q.total), RIGHT, s.y + 14f, paint(s.accent, 14f, bold = true, align = Paint.Align.RIGHT))
        s.y += 28f
        if (!q.isInvoice) {
            q.depositDue?.let { dep ->
                s.canvas.drawText(
                    "Deposit due on acceptance (${q.depositPercent}%): ${money(dep)}",
                    RIGHT, s.y + 6f, paint(MUTED, 10f, align = Paint.Align.RIGHT),
                )
                s.y += 20f
            }
        }
    }

    private fun drawNotes(s: PageState, q: QuoteDetail) {
        val notes = q.notes?.takeIf { it.isNotBlank() } ?: return
        s.ensure(40f)
        s.canvas.drawText("NOTES", MARGIN, s.y + 10f, paint(MUTED, 9f, bold = true))
        s.y += 18f
        val p = paint(INK, 11f)
        wrap(notes, p, RIGHT - MARGIN).forEach { line ->
            s.ensure(16f)
            s.canvas.drawText(line, MARGIN, s.y + 11f, p)
            s.y += 15f
        }
    }

    private fun drawFooter(s: PageState) {
        s.canvas.drawText(
            "Generated by Wireway Pro",
            MARGIN,
            BOTTOM + 18f,
            paint(MUTED, 9f),
        )
    }

    /**
     * Free-plan watermark (WIREWAY_PRICING_TIERS.md): a light diagonal
     * "MADE WITH WIREWAY" across every page. Pro exports never call this.
     */
    private fun drawWatermark(canvas: Canvas) {
        val p = paint(0x140A0E14, 40f, bold = true, align = Paint.Align.CENTER)
        canvas.save()
        canvas.rotate(-35f, PAGE_W / 2f, PAGE_H / 2f)
        canvas.drawText("MADE WITH WIREWAY", PAGE_W / 2f, PAGE_H / 2f, p)
        canvas.drawText("wirewaypro.com", PAGE_W / 2f, PAGE_H / 2f + 34f, paint(0x140A0E14, 16f, align = Paint.Align.CENTER))
        canvas.restore()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private fun rule(s: PageState) {
        s.canvas.drawLine(MARGIN, s.y, RIGHT, s.y, paint(HAIR, 1f).apply { strokeWidth = 1f })
    }

    private fun trimNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    /** Greedy word-wrap to fit [maxWidth] points. */
    private fun wrap(text: String, p: Paint, maxWidth: Float): List<String> {
        val out = mutableListOf<String>()
        var line = StringBuilder()
        for (word in text.split(Regex("\\s+"))) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(candidate) <= maxWidth) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) out.add(line.toString())
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        return out
    }
}
