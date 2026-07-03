package com.wirewaypro.app.data.entitlements

import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the signed-in user's effective [Tier]: the highest of the server
 * profile's plan and the device's owned Play subscriptions (see [Tier.resolve]).
 * The pure mapping lives in the domain layer; this class only gathers the two
 * inputs. Everything degrades toward FREE, never toward a crash.
 */
@Singleton
class TierService @Inject constructor(
    private val auth: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val playEntitlements: PlayEntitlements,
) {
    suspend fun current(): Tier {
        val profile = auth.currentUserId()
            ?.let { profileRepository.getProfile(it).getOrNull() }
        return Tier.resolve(profile, playEntitlements.ownedProducts.value)
    }
}
