package com.example.financemanager

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.financemanager.databinding.ActivityLoginBinding
import com.example.financemanager.ui.EmphasizedDecelerate
import com.example.financemanager.ui.shake

class ActivityLogin : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val pulseAnimators = mutableListOf<Animator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), authCallback)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_title))
            .setSubtitle(getString(R.string.biometric_subtitle))
            .setNegativeButtonText(getString(R.string.odustani))
            .build()

        binding.fingerprintButton.setOnClickListener { authenticate() }

        if (savedInstanceState == null) {
            playEntrance()
            // Offer the prompt right away once the intro has settled.
            binding.root.postDelayed({ if (!isFinishing) authenticate() }, 750L)
        }
        startPulse()
    }

    override fun onDestroy() {
        pulseAnimators.forEach { it.cancel() }
        super.onDestroy()
    }

    private fun authenticate() {
        biometricPrompt.authenticate(promptInfo)
    }

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            val cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_CANCELED
            if (cancelled) {
                setStatus(getString(R.string.login_hint), isError = false)
            } else {
                setStatus(errString, isError = true)
                binding.fingerprintButton.shake()
            }
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onLoginSuccess()
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            setStatus(getString(R.string.login_failed), isError = true)
            binding.fingerprintButton.shake()
        }
    }

    private fun setStatus(text: CharSequence, isError: Boolean) {
        val status = binding.loginStatus
        status.animate().alpha(0f).setDuration(120L).withEndAction {
            status.text = text
            status.setTextColor(
                ContextCompat.getColor(this, if (isError) R.color.login_error else R.color.white_70)
            )
            status.animate().alpha(1f).setDuration(200L).start()
        }.start()
    }

    private fun onLoginSuccess() {
        pulseAnimators.forEach { it.cancel() }
        binding.pulseRing1.alpha = 0f
        binding.pulseRing2.alpha = 0f
        binding.loginImage.setImageResource(R.drawable.ic_check)
        setStatus(getString(R.string.login_success), isError = false)

        binding.fingerprintButton.apply {
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420L)
                .setInterpolator(OvershootInterpolator(2.5f))
                .withEndAction { openDashboard() }
                .start()
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun playEntrance() {
        val density = resources.displayMetrics.density
        listOf(binding.brandBlock, binding.authBlock).forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 40f * density
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(100L + index * 140L)
                .setDuration(700L)
                .setInterpolator(EmphasizedDecelerate)
                .start()
        }
        listOf(binding.blobTop, binding.blobBottom).forEach { blob ->
            blob.scaleX = 0.6f
            blob.scaleY = 0.6f
            blob.animate().scaleX(1f).scaleY(1f).setDuration(1200L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun startPulse() {
        listOf(binding.pulseRing1, binding.pulseRing2).forEachIndexed { index, ring ->
            pulseAnimators += pulse(ring, startDelay = index * 900L)
        }
    }

    private fun pulse(view: View, startDelay: Long): Animator =
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.6f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.6f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f)
        ).apply {
            duration = 1800L
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            start()
        }
}
