package com.glazev.breathingtrainer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UriPermissionPolicyTest {
    @Test
    fun releasesOnlyUrisThatNoProfileReferences() {
        val held = setOf("content://music/one", "content://music/two", "content://music/old")
        val referencesAcrossProfiles = setOf("content://music/one", "content://music/two")

        assertEquals(
            setOf("content://music/old"),
            UriPermissionPolicy.unusedPermissions(held, referencesAcrossProfiles)
        )
    }

    @Test
    fun keepsSharedUriWhileAnotherProfileStillUsesIt() {
        val shared = "content://music/shared"

        assertEquals(
            emptySet<String>(),
            UriPermissionPolicy.unusedPermissions(setOf(shared), setOf(shared))
        )
    }

    @Test
    fun ignoresInvalidBlankGrant() {
        assertEquals(
            setOf("content://music/old"),
            UriPermissionPolicy.unusedPermissions(
                heldUris = setOf("", "content://music/old"),
                referencedUris = emptySet()
            )
        )
    }
}
