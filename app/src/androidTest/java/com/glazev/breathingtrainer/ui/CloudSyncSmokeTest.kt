package com.glazev.breathingtrainer.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Read-only smoke test against the profile of the account signed in on the test device. */
@RunWith(AndroidJUnit4::class)
class CloudSyncSmokeTest {

    @Test
    fun signedInAccountHasCompleteVersionedCloudProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        assertNotNull("The device must be signed in before this smoke test", user)

        val latch = CountDownLatch(1)
        val result = AtomicReference<Map<String, Any>?>()
        val failure = AtomicReference<Exception?>()
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user!!.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                result.set(snapshot.data)
                latch.countDown()
            }
            .addOnFailureListener { error ->
                failure.set(error)
                latch.countDown()
            }

        assertTrue("Timed out while reading the cloud profile", latch.await(20, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Cloud profile read failed", it) }
        val data = result.get()
        assertNotNull("Cloud profile document does not exist", data)
        assertEquals(2L, (data!!["schemaVersion"] as Number).toLong())

        val requiredFields = setOf(
            "updatedAt",
            "clientRevision",
            "selectedVoice",
            "selectedMusicId",
            "musicVolume",
            "breathVolume",
            "finalSoundVolume",
            "finalSoundId",
            "vibrationEnabled",
            "streakCount",
            "lastTrainingDate",
            "reminderTime",
            "dayReminders",
            "userPresets",
            "trainingHistory"
        )
        assertTrue("Cloud profile is missing required fields", data.keys.containsAll(requiredFields))
    }
}
