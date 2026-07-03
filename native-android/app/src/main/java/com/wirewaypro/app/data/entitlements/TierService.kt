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
    /**
     * Last profile that actually loaded, reused only when a later fetch FAILS
     * (offline job site) — a fetch that succeeds with a lower plan still
     * downgrades. Play purchases already survive offline via Play's own cache.
     */
    @Volatile
    private var lastKnownProfile: com.wirewaypro.app.domain.model.UserProfile? = null

    suspend fun current(): Tier {
        val userId = auth.currentUserId()
        val profile = userId?.let { profileRepository.getProfile(it).getOrNull() }
            ?.also { lastKnownProfile = it }
            ?: lastKnownProfile?.takeIf { it.id == userId }
        return Tier.resolve(profile, playEntitlements.ownedProducts.value)
    }
}
