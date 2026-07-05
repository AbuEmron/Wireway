package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import com.wirewaypro.app.data.local.QuotePhotoEntity

/**
 * Full-screen job-walk photo gallery: swipe between every site photo attached to
 * the record, pinch-to-zoom on any one, and delete or close. Pure viewer — these
 * are attachments, never AI input. Reads the on-device downscaled JPEGs the
 * [com.wirewaypro.app.data.quotes.QuotePhotoStore] wrote.
 */
@Composable
fun SitePhotoGallery(
    photos: List<QuotePhotoEntity>,
    initialIndex: Int,
    onRemove: (QuotePhotoEntity) -> Unit,
    onClose: () -> Unit,
) {
    if (photos.isEmpty()) { onClose(); return }
    val start = initialIndex.coerceIn(0, photos.lastIndex)
    val pagerState = rememberPagerState(initialPage = start) { photos.size }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableSitePhoto(photos[page])
        }

        // Top bar: index, delete this one, close.
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close gallery", tint = Color.White)
            }
            Text(
                "${pagerState.currentPage + 1} / ${photos.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                val current = photos.getOrNull(pagerState.currentPage) ?: return@IconButton
                onRemove(current)
                if (photos.size <= 1) onClose()
            }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete photo", tint = Color.White)
            }
        }
    }
}

/** One full-bleed photo with pinch-zoom + pan (double-tap not required — attachments). */
@Composable
private fun ZoomableSitePhoto(photo: QuotePhotoEntity) {
    val bitmap: ImageBitmap? = remember(photo.path) {
        runCatching {
            // A larger sample than the thumb — readable full-screen, still light.
            BitmapFactory.decodeFile(photo.path)?.asImageBitmap()
        }.getOrNull()
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Site photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f; offsetY = 0f
                            }
                        }
                    },
            )
        } else {
            Text("Couldn't load this photo", color = Color.White)
        }
    }
}
