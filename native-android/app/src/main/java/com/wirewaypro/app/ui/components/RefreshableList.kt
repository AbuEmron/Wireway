package com.wirewaypro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Wraps a [LazyColumn] in a Material 3 pull-to-refresh container and renders the
 * canonical loading / error / empty / content states. The empty + error states
 * still fill the viewport so the pull gesture works when there are no rows.
 *
 * @param isEmpty whether the underlying data set is empty (drives state choice).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableList(
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String?,
    isEmpty: Boolean,
    emptyMessage: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    listContent: LazyListScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            when {
                isLoading && isEmpty -> item {
                    FullViewportBox { CircularProgressIndicator() }
                }

                error != null && isEmpty -> item {
                    FullViewportBox {
                        CenteredMessage(
                            title = error,
                            subtitle = "Pull down to retry.",
                        )
                    }
                }

                isEmpty -> item {
                    FullViewportBox {
                        CenteredMessage(title = emptyMessage, subtitle = null)
                    }
                }

                else -> listContent()
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.lazy.LazyItemScope.FullViewportBox(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillParentMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun CenteredMessage(title: String, subtitle: String?) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
