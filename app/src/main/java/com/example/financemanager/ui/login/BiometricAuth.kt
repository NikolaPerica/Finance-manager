package com.example.financemanager.ui.login

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface AuthResult {
    data object Success : AuthResult
    /** The user dismissed the prompt; nothing went wrong. */
    data object Cancelled : AuthResult
    /** A single attempt was rejected (e.g. unknown finger); the prompt stays open. */
    data object Rejected : AuthResult
    data class Error(val message: CharSequence) : AuthResult
}

/**
 * Wraps [BiometricPrompt]. Fingerprint (or face) is offered first and the device
 * PIN/pattern/password is allowed as a fallback.
 */
class BiometricAuth(
    private val activity: FragmentActivity,
    private val title: String,
    private val subtitle: String,
    private val onResult: (AuthResult) -> Unit,
) {
    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onResult(AuthResult.Success)

            override fun onAuthenticationFailed() = onResult(AuthResult.Rejected)

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> AuthResult.Cancelled
                    else -> AuthResult.Error(errString)
                },
            )
        },
    )

    /** False when the device has no screen lock at all, so there is nothing to verify against. */
    val isAvailable: Boolean
        get() = BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate() {
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }

    private companion object {
        // BIOMETRIC_STRONG cannot be combined with DEVICE_CREDENTIAL below API 30.
        const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
}
