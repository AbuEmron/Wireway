package com.wirewaypro.app.di

import android.content.Context
import androidx.room.Room
import com.wirewaypro.app.data.local.QuoteDao
import com.wirewaypro.app.data.local.WirewayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the local Room [WirewayDatabase] and its DAOs. The DB is a rebuildable
 * offline cache of Supabase, so destructive migration is acceptable during
 * Phase 0 — a refresh repopulates it. (Unsynced local writes are flushed before
 * any version bump ships.)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WirewayDatabase =
        Room.databaseBuilder(context, WirewayDatabase::class.java, WirewayDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideQuoteDao(db: WirewayDatabase): QuoteDao = db.quoteDao()
}
