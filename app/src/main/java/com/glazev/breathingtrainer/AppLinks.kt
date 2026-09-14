package com.glazev.breathingtrainer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

object AppLinks {
    const val PRIVACY_POLICY =
        "https://breathingtrainer-ecec7-privacy.web.app/privacy-policy"
    const val RUSTORE_DEVELOPER_PAGE =
        "https://www.rustore.ru/catalog/developer/yqcezb2f"
    const val TELEGRAM = "https://t.me/Applavka"
    const val VK = "https://vk.ru/applavka"
}

fun Context.openExternalLink(url: String) {
    val uri = url.toUri()
    if (uri.scheme != "https") return

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        if (this@openExternalLink !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { startActivity(intent) }
        .onFailure {
            Toast.makeText(
                this,
                "Не удалось открыть ссылку",
                Toast.LENGTH_SHORT
            ).show()
        }
}
