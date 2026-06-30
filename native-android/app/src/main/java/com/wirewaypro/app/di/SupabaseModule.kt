package com.wirewaypro.app.di

import com.wirewaypro.app.BuildConfig
import com.wirewaypro.app.data.auth.AuthRepositoryImpl
import com.wirewaypro.app.data.clients.ClientRepositoryImpl
import com.wirewaypro.app.data.expenses.ExpenseRepositoryImpl
import com.wirewaypro.app.data.jobs.JobRepositoryImpl
import com.wirewaypro.app.data.money.MoneyRepositoryImpl
import com.wirewaypro.app.data.profile.ProfileRepositoryImpl
import com.wirewaypro.app.data.quotes.QuoteRepositoryImpl
import com.wirewaypro.app.data.trips.TripRepositoryImpl
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ClientRepository
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.repository.MoneyRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides the singleton [SupabaseClient], pointed at the SAME backend as the web
 * app. The URL + anon key come from BuildConfig, which is populated from
 * local.properties (REACT_APP_SUPABASE_URL / REACT_APP_SUPABASE_ANON_KEY) — see
 * app/build.gradle.kts. Only the public anon key is ever embedded; RLS on
 * Supabase enforces per-user access.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            // `quotes` / `jobs` rows carry many more columns than our slim DTOs
            // read, so ignore unknown keys; coerce bad/null values to defaults.
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )

            // Auth (gotrue) persists + auto-refreshes the session out of the box;
            // on Android it stores the session via androidx.startup-provided context.
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
}

/**
 * Binds the concrete repositories to their domain-layer interfaces so the UI/
 * domain layers never depend on Supabase types directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindClientRepository(impl: ClientRepositoryImpl): ClientRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindMoneyRepository(impl: MoneyRepositoryImpl): MoneyRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository
}
