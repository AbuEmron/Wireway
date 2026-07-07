package com.wirewaypro.app.di

import android.content.Context
import androidx.room.Room
import com.wirewaypro.app.data.local.ClientDao
import com.wirewaypro.app.data.local.CrewMemberDao
import com.wirewaypro.app.data.local.JobDao
import com.wirewaypro.app.data.local.JobDrawDao
import com.wirewaypro.app.data.local.JurisdictionDao
import com.wirewaypro.app.data.local.OverrideDao
import com.wirewaypro.app.data.local.QuoteDao
import com.wirewaypro.app.data.local.QuotePhotoDao
import com.wirewaypro.app.data.local.QuoteDraftDao
import com.wirewaypro.app.data.local.UserAssemblyDao
import com.wirewaypro.app.data.local.WirewayDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the local Room [WirewayDatabase] and its DAOs. Real additive migrations
 * keep unsynced local rows across every upgrade; there is NO unconditional
 * destructive fallback (data loss is unacceptable). Only a downgrade — schema
 * newer than the code — drops the cache, which a refresh repopulates.
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
                WirewayDatabase.MIGRATION_4_5,
                WirewayDatabase.MIGRATION_5_6,
                WirewayDatabase.MIGRATION_6_7,
                WirewayDatabase.MIGRATION_7_8,
                // v8 → v9 lives WITH the e-signature module (self-contained) so its
                // schema travels with it; the DDL is additive (two new tables only).
                com.wirewaypro.app.esign.data.EsignSchema.MIGRATION_8_9,
            )
            // NEVER a blanket destructive fallback — data loss is unacceptable
            // (it once wiped real user data on a sibling app). Every version step
            // has a real additive migration, so an UPGRADE always has a path. If
            // the schema is ever unexpectedly newer than the code (a DOWNGRADE,
            // e.g. a dev sideloading an older build), drop-and-recreate the cache
            // rather than crash — a refresh repopulates the server-backed rows.
            // On any other unexpected schema the app fails LOUDLY instead of
            // silently erasing.
            .fallbackToDestructiveMigrationOnDowngrade()
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

    @Provides
    fun provideQuotePhotoDao(db: WirewayDatabase): QuotePhotoDao = db.quotePhotoDao()

    @Provides
    fun provideUserAssemblyDao(db: WirewayDatabase): UserAssemblyDao = db.userAssemblyDao()

    @Provides
    fun provideCrewMemberDao(db: WirewayDatabase): CrewMemberDao = db.crewMemberDao()

    @Provides
    fun provideJurisdictionDao(db: WirewayDatabase): JurisdictionDao = db.jurisdictionDao()

    @Provides
    fun provideEsignRecordDao(db: WirewayDatabase): com.wirewaypro.app.esign.data.EsignRecordDao =
        db.esignRecordDao()

    @Provides
    fun provideEsignAuditEventDao(db: WirewayDatabase): com.wirewaypro.app.esign.data.EsignAuditEventDao =
        db.esignAuditEventDao()
}
