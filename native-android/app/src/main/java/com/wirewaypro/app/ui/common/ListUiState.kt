package com.wirewaypro.app.ui.common

/** Generic state for a read-only list screen with pull-to-refresh. */
data class ListUiState<T>(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<T> = emptyList(),
    val error: String? = null,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
