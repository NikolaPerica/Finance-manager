package com.example.financemanager.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.PathInterpolator
import androidx.core.view.children
import com.example.financemanager.R

/** Material "emphasized decelerate" easing, used for content entering the screen. */
val EmphasizedDecelerate = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

/** Fades and slides each child of [group] into place, one after another. */
fun staggerIn(group: ViewGroup, startDelay: Long = 60L, step: Long = 70L) {
    val offset = 32f * group.resources.displayMetrics.density
    group.children.forEachIndexed { index, child ->
        child.alpha = 0f
        child.translationY = offset
        child.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(startDelay + index * step)
            .setDuration(520L)
            .setInterpolator(EmphasizedDecelerate)
            .start()
    }
}

fun View.shake() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
}
