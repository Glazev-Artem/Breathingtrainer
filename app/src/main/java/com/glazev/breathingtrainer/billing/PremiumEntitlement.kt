package com.glazev.breathingtrainer.billing

/**
 * Premium starts locked on every process start and can only be changed by a
 * fresh result from the store. Local preferences and cloud documents are not
 * accepted as entitlement sources.
 */
internal class PremiumEntitlement {
    var isPremium: Boolean = false
        private set

    fun applyVerifiedResult(hasPremium: Boolean) {
        isPremium = hasPremium
    }
}
