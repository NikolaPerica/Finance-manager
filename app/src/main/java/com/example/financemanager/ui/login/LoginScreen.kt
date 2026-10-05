package com.example.financemanager.ui.login

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.financemanager.R
import com.example.financemanager.ui.components.EmphasizedDecelerate
import com.example.financemanager.ui.components.shakeOn
import com.example.financemanager.ui.components.staggeredEntrance
import com.example.financemanager.ui.theme.FinanceTheme
import kotlinx.coroutines.delay

private val LoginError = Color(0xFFFFD6D8)
private val White70 = Color.White.copy(alpha = 0.7f)

private sealed interface LoginStatus {
    data object Idle : LoginStatus
    data object Success : LoginStatus
    data object Unavailable : LoginStatus
    data class Failed(val message: String) : LoginStatus
}

@Composable
fun LoginScreen(onUnlocked: () -> Unit) {
    val activity = LocalActivity.current as FragmentActivity
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)

    LightSystemBarIcons(activity)

    var status by remember { mutableStateOf<LoginStatus>(LoginStatus.Idle) }
    var shake by remember { mutableIntStateOf(0) }
    val failedText = stringResource(R.string.login_failed)

    val auth = remember(activity) {
        BiometricAuth(
            activity = activity,
            title = activity.getString(R.string.biometric_title),
            subtitle = activity.getString(R.string.biometric_subtitle),
        ) { result ->
            when (result) {
                AuthResult.Success -> status = LoginStatus.Success
                AuthResult.Cancelled -> status = LoginStatus.Idle
                AuthResult.Rejected -> {
                    status = LoginStatus.Failed(failedText)
                    shake++
                }
                is AuthResult.Error -> {
                    status = LoginStatus.Failed(result.message.toString())
                    shake++
                }
            }
        }
    }
    // Offer the prompt once, right after the intro has settled.
    var autoPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!auth.isAvailable) {
            status = LoginStatus.Unavailable
        } else if (!autoPrompted) {
            autoPrompted = true
            delay(750)
            auth.authenticate()
        }
    }
    LaunchedEffect(status) {
        if (status == LoginStatus.Success) {
            delay(450)
            currentOnUnlocked()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(FinanceTheme.colors.gradient)),
    ) {
        val firstLaunch = !autoPrompted || status == LoginStatus.Unavailable
        Blob(size = 320.dp, modifier = Modifier.offset(x = maxWidth - 200.dp, y = (-80).dp))
        Blob(size = 260.dp, modifier = Modifier.offset(x = (-100).dp, y = maxHeight - 160.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.staggeredEntrance(0, play = firstLaunch, startDelay = 100, step = 140),
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_wallet),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.login_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = White70,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.staggeredEntrance(1, play = firstLaunch, startDelay = 100, step = 140),
            ) {
                FingerprintButton(
                    succeeded = status == LoginStatus.Success,
                    enabled = status != LoginStatus.Unavailable,
                    modifier = Modifier.shakeOn(shake),
                    onClick = auth::authenticate,
                )
                Spacer(Modifier.height(16.dp))
                StatusText(status)
                if (status == LoginStatus.Unavailable) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onUnlocked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = FinanceTheme.colors.gradient.first(),
                        ),
                    ) { Text(stringResource(R.string.login_continue)) }
                }
            }
        }
    }
}

@Composable
private fun FingerprintButton(succeeded: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    Box(modifier = modifier.size(180.dp), contentAlignment = Alignment.Center) {
        if (!succeeded && enabled) {
            repeat(2) { ring ->
                val progress by pulse.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(1800, easing = LinearOutSlowInEasing),
                        RepeatMode.Restart,
                        initialStartOffset = StartOffset(ring * 900),
                    ),
                    label = "ring$ring",
                )
                Box(
                    Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            val scale = 1f + 0.6f * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.6f * (1f - progress)
                        }
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                )
            }
        }
        val pop = remember { Animatable(1f) }
        LaunchedEffect(succeeded) {
            if (succeeded) {
                pop.snapTo(0.8f)
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
            }
        }
        Surface(
            onClick = onClick,
            enabled = enabled && !succeeded,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.size(112.dp).scale(pop.value),
        ) {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = succeeded,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
                    label = "icon",
                ) { done ->
                    Icon(
                        painterResource(if (done) R.drawable.ic_check else R.drawable.ic_fingerprint),
                        contentDescription = stringResource(R.string.login_fingerprint_description),
                        tint = Color.White,
                        modifier = Modifier.size(60.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusText(status: LoginStatus) {
    AnimatedContent(
        targetState = status,
        transitionSpec = { fadeIn(tween(200, delayMillis = 120)) togetherWith fadeOut(tween(120)) },
        label = "status",
    ) { current ->
        val (text, color) = when (current) {
            LoginStatus.Idle -> stringResource(R.string.login_hint) to White70
            LoginStatus.Success -> stringResource(R.string.login_success) to White70
            LoginStatus.Unavailable -> stringResource(R.string.login_unavailable) to White70
            is LoginStatus.Failed -> current.message to LoginError
        }
        Text(text, color = color, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Blob(size: Dp, modifier: Modifier) {
    val grow = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1200, easing = EmphasizedDecelerate)) }
    Box(
        modifier
            .size(size)
            .scale(grow.value)
            .background(Color.White.copy(alpha = 0.08f), CircleShape),
    )
}

/** The gradient is dark in both themes, so keep the status/navigation bar icons light while shown. */
@Composable
private fun LightSystemBarIcons(activity: ComponentActivity) {
    DisposableEffect(activity) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        onDispose { activity.enableEdgeToEdge() }
    }
}
