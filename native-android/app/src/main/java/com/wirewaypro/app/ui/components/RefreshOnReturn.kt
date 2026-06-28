package com.wirewaypro.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Calls [onRefresh] each time the screen RESUMES *except the first* — i.e. when
 * the user returns from a pushed create/edit screen — so list screens pick up
 * newly created or edited rows without a manual pull-to-refresh. The first
 * resume is skipped because the ViewModel already loads on init.
 */
@Composable
fun RefreshOnReturn(onRefresh: () -> Unit) {
    var first by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (first) first = false else onRefresh()
    }
}
