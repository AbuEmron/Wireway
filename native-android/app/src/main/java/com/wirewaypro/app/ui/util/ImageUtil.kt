package com.wirewaypro.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Downscales a picked/captured image to a JPEG byte array, mirroring the web
 * app's processReceiptImage (max 1600px, quality 85) so receipt uploads stay
 * small. Runs synchronously — call it off the main thread.
 */
object ImageUtil {
    fun downscaleToJpeg(context: Context, uri: Uri, maxDim: Int = 1600, quality: Int = 85): ByteArray? {
        return try {
            val resolver = context.contentResolver

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sample = 1
            while (max(srcW, srcH) / sample > maxDim) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val scale = min(1f, maxDim.toFloat() / max(decoded.width, decoded.height))
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
            } else {
                decoded
            }

            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Renders the first page of a PDF to a JPEG byte array (for plan PDFs). */
    fun pdfFirstPageToJpeg(context: Context, uri: Uri, maxDim: Int = 1600, quality: Int = 85): ByteArray? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return null
                    renderer.openPage(0).use { page ->
                        // PDF pages are in points (~72dpi); upscale so the long side ≈ maxDim.
                        val scale = (maxDim.toFloat() / max(page.width, page.height)).coerceIn(0.5f, 4f)
                        val w = max(1, (page.width * scale).toInt())
                        val h = max(1, (page.height * scale).toInt())
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE) // PDFs assume a white page
                        val matrix = Matrix().apply { setScale(scale, scale) }
                        page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        ByteArrayOutputStream().use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
                            out.toByteArray()
                        }
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
