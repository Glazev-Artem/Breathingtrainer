package com.glazev.breathingtrainer.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementTest {
    @Test
    fun premiumIsLockedUntilAStoreResultIsApplied() {
        val entitlement = PremiumEntitlement()

        assertFalse(entitlement.isPremium)
        entitlement.applyVerifiedResult(true)
        assertTrue(entitlement.isPremium)
    }

    @Test
    fun revokedPurchaseRemovesPremium() {
        val entitlement = PremiumEntitlement()
        entitlement.applyVerifiedResult(true)

        entitlement.applyVerifiedResult(false)

        assertFalse(entitlement.isPremium)
    }
}
