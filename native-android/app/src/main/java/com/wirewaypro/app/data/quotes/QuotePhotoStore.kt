package com.wirewaypro.app.data.quotes

import android.content.Context
import android.net.Uri
import com.wirewaypro.app.data.local.QuotePhotoDao
import com.wirewaypro.app.data.local.QuotePhotoEntity
import com.wirewaypro.app.ui.util.ImageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores job-walk site photos for a quote: downscaled JPEG copies under
 * filesDir/quote-photos/<quoteId>/ plus a [QuotePhotoDao] row. On-device only —
 * honest scope until the backend grows an attachments home.
 */
@Singleton
class QuotePhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: QuotePhotoDao,
) {
    private fun dirFor(quoteId: String) = File(File(context.filesDir, "quote-photos"), quoteId)

    /** Copies (downscaled) the picked/captured image in; null if it can't be read. */
    suspend fun add(quoteId: String, uri: Uri): QuotePhotoEntity? = withContext(Dispatchers.IO) {
        val bytes = ImageUtil.downscaleToJpeg(context, uri) ?: return@withContext null
        val now = System.currentTimeMillis()
        val file = File(dirFor(quoteId).apply { mkdirs() }, "$now.jpg")
        file.writeBytes(bytes)
        val entity = QuotePhotoEntity(quoteId = quoteId, path = file.absolutePath, atMillis = now)
        entity.copy(id = dao.insert(entity))
    }

    suspend fun forQuote(quoteId: String): List<QuotePhotoEntity> = dao.forQuote(quoteId)

    suspend fun remove(photo: QuotePhotoEntity) = withContext(Dispatchers.IO) {
        runCatching { File(photo.path).delete() }
        dao.delete(photo.id)
    }

    /** The quote is gone — its photos and folder go with it. */
    suspend fun clearFor(quoteId: String) = withContext(Dispatchers.IO) {
        dao.deleteFor(quoteId)
        runCatching { dirFor(quoteId).deleteRecursively() }
    }
}
