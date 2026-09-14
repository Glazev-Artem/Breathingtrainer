package com.glazev.breathingtrainer.auth

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTokenExchangeClientTest {
    @Test
    fun rejectsMissingHttpsEndpointBeforeSendingCredentials() {
        val result = AuthTokenExchangeClient("").exchange(ExternalAuthProvider.YANDEX, "token")

        assertTrue(result.isFailure)
    }
}
