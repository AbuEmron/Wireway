package com.wirewaypro.app.widget

import com.wirewaypro.app.data.widget.WidgetSnapshotStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Glance widgets render outside the Activity/ViewModel graph, so they can't use
 * constructor injection. This entry point lets [WirewayWidget] pull the cached
 * snapshot store from the application's Hilt graph.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetSnapshotStore(): WidgetSnapshotStore
}
