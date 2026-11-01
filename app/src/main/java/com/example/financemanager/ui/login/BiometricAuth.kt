package com.example.financemanager.ui.login

import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

sealed interface AuthResult {
    data object Success : AuthResult
    /** The user dismissed the prompt; nothing went wrong. */
    data object Cancelled : AuthResult
    /** A single attempt was rejected (e.g. unknown finger); the prompt stays open. */
    data object Rejected : AuthResult
    /** Nothing to check against after all, e.g. no fingerprint enrolled on Android 9. */
    data object Unavailable : AuthResult
    data class Error(val message: CharSequence) : AuthResult
}

/**
 * Asks Android's own [BiometricPrompt] for the fingerprint (or face), with the device
 * PIN/pattern/password allowed as a fallback. Android 9 has no such fallback in its prompt, so
 * there it is the fingerprint only, with a button to cancel.
 *
 * The platform prompt is used rather than AndroidX Biometric, which keeps whether a prompt is
 * showing in shared state: on Samsung phones that state could stay set after the prompt was
 * dismissed, and every later request was then silently ignored. Here each request goes
 * straight to the system.
 */
class BiometricAuth(
    private val context: Context,
    private val title: String,
    private val subtitle: String,
    /** The cancel button, shown only on Android 9. */
    private val cancel: String,
    private val onResult: (AuthResult) -> Unit,
) {
    private val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(AuthResult.Success)

        override fun onAuthenticationFailed() = onResult(AuthResult.Rejected)

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(
            when (errorCode) {
                BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> AuthResult.Cancelled
                BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS,
                BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT -> AuthResult.Unavailable
                else -> AuthResult.Error(errString)
            },
        )
    }

    /** False when the device has no screen lock at all, so there is nothing to verify against. */
    val isAvailable: Boolean
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> context.getSystemService(BiometricManager::class.java)
                .canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            // The PIN/pattern/password fallback works with any secure screen lock.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> context.getSystemService(KeyguardManager::class.java).isDeviceSecure
            // Whether a fingerprint is enrolled can't be asked here; the prompt says so if not.
            else -> context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) &&
                context.getSystemService(KeyguardManager::class.java).isDeviceSecure
        }

    fun authenticate() {
        val builder = BiometricPrompt.Builder(context).setTitle(title).setSubtitle(subtitle)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> builder.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> @Suppress("DEPRECATION") builder.setDeviceCredentialAllowed(true)
            else -> builder.setNegativeButton(cancel, context.mainExecutor) { _, _ -> onResult(AuthResult.Cancelled) }
        }
        // A new request replaces one still showing; the system cancels the old one.
        builder.build().authenticate(CancellationSignal(), context.mainExecutor, callback)
    }
}
