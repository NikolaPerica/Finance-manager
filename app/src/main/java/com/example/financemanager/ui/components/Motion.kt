package com.example.financemanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Material "emphasized decelerate" easing, used for content entering the screen. */
val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Fades and slides content up into place, delayed by its [index] so that a column
 * of items arrives one after another. Plays only when [play] is true.
 */
fun Modifier.staggeredEntrance(index: Int, play: Boolean, startDelay: Int = 60, step: Int = 70): Modifier =
    composed {
        val progress = remember { Animatable(if (play) 0f else 1f) }
        val offset = with(LocalDensity.current) { 32.dp.toPx() }
        LaunchedEffect(Unit) {
            progress.animateTo(
                1f,
                tween(durationMillis = 520, delayMillis = startDelay + index * step, easing = EmphasizedDecelerate),
            )
        }
        graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * offset
        }
    }

/** Shakes the content horizontally every time [trigger] changes to a new non-zero value. */
fun Modifier.shakeOn(trigger: Int): Modifier = composed {
    val offset = remember { Animatable(0f) }
    val distance = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        offset.animateTo(0f, keyframes {
            durationMillis = 400
            distance at 50
            -distance at 150
            distance * 0.6f at 250
            -distance * 0.3f at 330
        })
    }
    graphicsLayer { translationX = offset.value }
}

/** Counts a money value up or down to [target] instead of jumping to it. */
@Composable
fun animatedAmount(target: Double): Double {
    var displayed by rememberSaveable { mutableDoubleStateOf(0.0) }
    LaunchedEffect(target) {
        val from = displayed
        if (from == target) return@LaunchedEffect
        animate(0f, 1f, animationSpec = tween(900, easing = EmphasizedDecelerate)) { value, _ ->
            displayed = from + (target - from) * value
        }
        displayed = target
    }
    return displayed
}
