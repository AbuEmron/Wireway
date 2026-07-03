package com.wirewaypro.app.di

import android.content.Context
import androidx.room.Room
import com.wirewaypro.app.data.local.ClientDao
import com.wirewaypro.app.data.local.JobDao
import com.wirewaypro.app.data.local.JobDrawDao
import com.wirewaypro.app.data.local.OverrideDao
import com.wirewaypro.app.data.local.QuoteDao
import com.wirewaypro.app.data.local.QuoteDraftDao
import com.wirewaypro.app.data.local.WirewayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the local Room [WirewayDatabase] and its DAOs. Real migrations keep
 * unsynced local rows across upgrades; [fallbackToDestructiveMigration] is only
 * a last-resort backstop for an unversioned/corrupt DB (a refresh repopulates
 * the cached server rows).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WirewayDatabase =
        Room.databaseBuilder(context, WirewayDatabase::class.java, WirewayDatabase.NAME)
            .addMigrations(
                WirewayDatabase.MIGRATION_1_2,
                WirewayDatabase.MIGRATION_2_3,
                WirewayDatabase.MIGRATION_3_4,
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideQuoteDao(db: WirewayDatabase): QuoteDao = db.quoteDao()

    @Provides
    fun provideQuoteDraftDao(db: WirewayDatabase): QuoteDraftDao = db.quoteDraftDao()

    @Provides
    fun provideJobDao(db: WirewayDatabase): JobDao = db.jobDao()

    @Provides
    fun provideClientDao(db: WirewayDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideJobDrawDao(db: WirewayDatabase): JobDrawDao = db.jobDrawDao()

    @Provides
    fun provideOverrideDao(db: WirewayDatabase): OverrideDao = db.overrideDao()
}
