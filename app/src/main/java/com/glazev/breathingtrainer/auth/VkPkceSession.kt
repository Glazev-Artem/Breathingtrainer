package com.glazev.breathingtrainer.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal data class VkAuthorizationRequest(
    val codeChallenge: String,
    val state: String
)

internal data class VkAuthorizationProof(
    val codeVerifier: String,
    val state: String
)

internal class VkPkceSession(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    private var pendingProof: VkAuthorizationProof? = null

    @Synchronized
    fun begin(): VkAuthorizationRequest {
        val verifier = randomBase64Url(64)
        val state = randomBase64Url(32)
        pendingProof = VkAuthorizationProof(verifier, state)
        return VkAuthorizationRequest(
            codeChallenge = codeChallenge(verifier),
            state = state
        )
    }

    @Synchronized
    fun consume(): VkAuthorizationProof? = pendingProof.also { pendingProof = null }

    @Synchronized
    fun clear() {
        pendingProof = null
    }

    private fun randomBase64Url(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        internal fun codeChallenge(verifier: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.toByteArray(Charsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }
    }
}
