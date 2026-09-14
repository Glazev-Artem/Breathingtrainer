package com.glazev.breathingtrainer.ui

internal object UriPermissionPolicy {
    fun unusedPermissions(
        heldUris: Set<String>,
        referencedUris: Set<String>
    ): Set<String> = heldUris.asSequence()
        .filter(String::isNotBlank)
        .filterNot(referencedUris::contains)
        .toSet()
}
