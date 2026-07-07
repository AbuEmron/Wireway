package com.wirewaypro.app.di

import com.wirewaypro.app.esign.crypto.AndroidKeystoreKeyProvider
import com.wirewaypro.app.esign.crypto.EsignKeyProvider
import com.wirewaypro.app.esign.crypto.SignatureVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Wires the e-signature module's crypto. The signature image is encrypted at rest
 * with an AES-256 key held in the Android Keystore (non-exportable). Swapping the
 * [EsignKeyProvider] binding is the only change needed to reuse the module elsewhere.
 */
@Module
@InstallIn(SingletonComponent::class)
object EsignModule {

    @Provides
    @Singleton
    fun provideEsignKeyProvider(): EsignKeyProvider = AndroidKeystoreKeyProvider()

    @Provides
    @Singleton
    fun provideSignatureVault(keyProvider: EsignKeyProvider): SignatureVault = SignatureVault(keyProvider)
}
