package com.wirewaypro.app.di

import com.wirewaypro.app.BuildConfig
import com.wirewaypro.app.data.auth.AuthRepositoryImpl
import com.wirewaypro.app.data.profile.ProfileRepositoryImpl
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
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
            // Auth (gotrue) persists + auto-refreshes the session out of the box;
            // on Android it stores the session via androidx.startup-provided context.
            install(Auth)
            install(Postgrest)
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
}
