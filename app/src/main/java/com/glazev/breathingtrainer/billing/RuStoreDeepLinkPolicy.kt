package com.glazev.breathingtrainer.billing

import android.content.Intent

internal object RuStoreDeepLinkPolicy {
    private const val SCHEME = "glazevbreathingpay"
    private const val MAX_URI_LENGTH = 4_096

    fun accepts(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return accepts(intent.action, uri.scheme, uri.toString())
    }

    internal fun accepts(action: String?, scheme: String?, raw: String): Boolean {
        if (action != Intent.ACTION_VIEW) return false
        if (scheme != SCHEME) return false
        return raw.length in 1..MAX_URI_LENGTH && raw.all { character ->
            character.code in 0x21..0x7e
        }
    }
}
