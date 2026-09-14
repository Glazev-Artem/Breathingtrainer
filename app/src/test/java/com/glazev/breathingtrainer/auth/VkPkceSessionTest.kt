package com.glazev.breathingtrainer.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VkPkceSessionTest {
    @Test
    fun createsValidOneTimePkceProof() {
        val session = VkPkceSession()
        val request = session.begin()
        val proof = session.consume()

        assertNotNull(proof)
        assertEquals(request.state, proof!!.state)
        assertEquals(request.codeChallenge, VkPkceSession.codeChallenge(proof.codeVerifier))
        assertTrue(proof.codeVerifier.length in 43..128)
        assertTrue(request.state.matches(Regex("[A-Za-z0-9_-]+")))
        assertNull(session.consume())
    }

    @Test
    fun beginningAgainInvalidatesPreviousProof() {
        val session = VkPkceSession()
        val first = session.begin()
        val second = session.begin()

        assertNotEquals(first.state, second.state)
        assertEquals(second.state, session.consume()?.state)
    }
}
