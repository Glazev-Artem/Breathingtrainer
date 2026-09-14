package com.glazev.breathingtrainer.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuStoreDeepLinkPolicyTest {
    @Test
    fun acceptsOnlyExpectedViewScheme() {
        val viewAction = "android.intent.action.VIEW"
        val validUri = "glazevbreathingpay://return?status=ok"

        assertTrue(RuStoreDeepLinkPolicy.accepts(viewAction, "glazevbreathingpay", validUri))
        assertFalse(RuStoreDeepLinkPolicy.accepts("android.intent.action.MAIN", "glazevbreathingpay", validUri))
        assertFalse(RuStoreDeepLinkPolicy.accepts(viewAction, "https", "https://example.com"))
        assertFalse(RuStoreDeepLinkPolicy.accepts(viewAction, null, ""))
    }

    @Test
    fun rejectsOversizedCallback() {
        val raw = "glazevbreathingpay://return?payload=${"a".repeat(4_096)}"
        assertFalse(RuStoreDeepLinkPolicy.accepts("android.intent.action.VIEW", "glazevbreathingpay", raw))
    }
}
