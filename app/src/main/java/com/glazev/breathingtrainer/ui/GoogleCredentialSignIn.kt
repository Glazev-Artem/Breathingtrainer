package com.glazev.breathingtrainer.ui

import android.app.Activity
import android.content.MutableContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.glazev.breathingtrainer.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

private const val GOOGLE_AUTH_TAG = "GoogleAuth"

/**
 * Returns an explicit "Sign in with Google" action backed by Credential Manager.
 * The resulting ID token is still exchanged through FirebaseAuth, so an existing
 * Google user keeps the same Firebase UID and therefore the same cloud profile.
 */
@Composable
internal fun rememberGoogleCredentialSignIn(
    onIdToken: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? Activity
    val credentialManager = remember(context.applicationContext) {
        CredentialManager.create(context.applicationContext)
    }
    val currentOnIdToken = rememberUpdatedState(onIdToken)
    val scope = rememberCoroutineScope()

    return remember(activity, credentialManager, scope) {
        {
            if (activity == null) {
                Log.e(GOOGLE_AUTH_TAG, "Google sign in requires an Activity context")
                Toast.makeText(context, "Не удалось открыть вход через Google", Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    try {
                        val googleOption = GetSignInWithGoogleOption.Builder(
                            context.getString(R.string.default_web_client_id)
                        ).build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleOption)
                            .build()
                        val result = credentialManager.getCredential(
                            context = MutableContextWrapper(activity),
                            request = request
                        )
                        val credential = result.credential
                        if (
                            credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            currentOnIdToken.value(googleCredential.idToken)
                        } else {
                            Log.e(GOOGLE_AUTH_TAG, "Credential Manager returned an unsupported credential")
                            Toast.makeText(context, "Google вернул неподдерживаемый способ входа", Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: GetCredentialCancellationException) {
                        Log.d(GOOGLE_AUTH_TAG, "Google sign in cancelled")
                    } catch (error: NoCredentialException) {
                        Log.w(GOOGLE_AUTH_TAG, "No Google credential is available", error)
                        Toast.makeText(
                            context,
                            "На устройстве нет доступного аккаунта Google",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (error: GoogleIdTokenParsingException) {
                        Log.e(GOOGLE_AUTH_TAG, "Failed to parse Google ID token", error)
                        Toast.makeText(context, "Не удалось проверить ответ Google", Toast.LENGTH_SHORT).show()
                    } catch (error: GetCredentialException) {
                        Log.e(GOOGLE_AUTH_TAG, "Google sign in failed", error)
                        Toast.makeText(context, "Не удалось войти через Google", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
