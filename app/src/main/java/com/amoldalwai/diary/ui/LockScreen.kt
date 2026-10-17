package com.amoldalwai.diary.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

object BiometricHelper {
    fun isAvailable(context: Context): Boolean =
        context.getSystemService(BiometricManager::class.java)
            ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: Activity, title: String, onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButton("Use PIN", executor) { _, _ -> }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }
            },
        )
    }
}

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    verify: (String) -> Boolean,
    onBiometricSuccess: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    SystemBarsStyle(darkIcons = !appIsDark())

    val biometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val canUseBiometric = biometricEnabled && biometricAvailable
    fun promptBiometric() {
        context.findActivity()?.let { BiometricHelper.prompt(it, "Unlock your diary", onBiometricSuccess) }
    }
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            delay(350)
            promptBiometric()
        }
    }

    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun onDigit(d: String) {
        if (pin.length >= PIN_LENGTH || wrong) return
        pin += d
        if (pin.length == PIN_LENGTH) {
            if (!verify(pin)) {
                wrong = true
                scope.launch {
                    repeat(3) {
                        shake.animateTo(14f, tween(45))
                        shake.animateTo(-14f, tween(45))
                    }
                    shake.animateTo(0f, tween(45))
                    pin = ""
                    wrong = false
                }
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant)))
            .pointerInput(Unit) {},
    ) {
        FloatingBlobs()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            LogoMark(Modifier.size(72.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (wrong) "Wrong PIN, try again" else "Enter your PIN to open your diary",
                color = if (wrong) colors.error else colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            PinDots(pin.length, wrong, Modifier.graphicsLayer { translationX = shake.value.dp.toPx() })
            Spacer(Modifier.height(36.dp))
            PinPad(
                onDigit = ::onDigit,
                onBackspace = { if (!wrong) pin = pin.dropLast(1) },
                showBiometric = canUseBiometric,
                onBiometric = ::promptBiometric,
            )
            Spacer(Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun PinDots(filled: Int, error: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(PIN_LENGTH) { i ->
            val on = i < filled
            val scale by animateFloatAsState(
                if (on) 1.15f else 1f,
                spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                label = "dot",
            )
            Box(
                Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(if (on) (if (error) colors.error else colors.primary) else colors.surface)
                    .border(1.5.dp, if (error) colors.error else colors.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit,
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                row.forEach { d -> PadKey(onClick = { onDigit(d) }) { Text(d, fontSize = 26.sp, fontWeight = FontWeight.Medium) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (showBiometric) {
                PadKey(onClick = onBiometric, filled = false) {
                    Icon(Icons.Rounded.Fingerprint, "Use fingerprint", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                }
            } else {
                Spacer(Modifier.size(72.dp))
            }
            PadKey(onClick = { onDigit("0") }) { Text("0", fontSize = 26.sp, fontWeight = FontWeight.Medium) }
            PadKey(onClick = onBackspace, filled = false) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PadKey(onClick: () -> Unit, filled: Boolean = true, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(72.dp)
            .bouncyClick(onClick)
            .clip(CircleShape)
            .background(if (filled) colors.surface.copy(alpha = 0.85f) else colors.surface.copy(alpha = 0f))
            .then(if (filled) Modifier.border(1.dp, colors.outline.copy(alpha = 0.4f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.onSurface,
        ) { content() }
    }
}

/** Collects a PIN in a dialog. With [confirm] the user types it twice; otherwise [verify] decides. */
@Composable
fun PinDialog(
    title: String,
    confirm: Boolean,
    onDismiss: () -> Unit,
    verify: (String) -> Boolean = { true },
    onSuccess: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun fail(text: String) {
        message = text
        busy = true
        scope.launch {
            repeat(3) {
                shake.animateTo(14f, tween(45))
                shake.animateTo(-14f, tween(45))
            }
            shake.animateTo(0f, tween(45))
            pin = ""
            busy = false
        }
    }

    fun onDigit(d: String) {
        if (busy || pin.length >= PIN_LENGTH) return
        pin += d
        if (pin.length < PIN_LENGTH) return
        when {
            confirm && first == null -> {
                first = pin
                pin = ""
                message = null
            }
            confirm -> if (pin == first) onSuccess(pin) else {
                first = null
                fail("PINs didn't match, start again")
            }
            else -> if (verify(pin)) onSuccess(pin) else fail("Wrong PIN")
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .background(colors.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(
                message ?: if (confirm && first != null) "Enter it again to confirm" else "Enter a $PIN_LENGTH-digit PIN",
                color = if (message != null) colors.error else colors.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))
            PinDots(pin.length, message != null && busy, Modifier.graphicsLayer { translationX = shake.value.dp.toPx() })
            Spacer(Modifier.height(24.dp))
            PinPad(onDigit = ::onDigit, onBackspace = { if (!busy) pin = pin.dropLast(1) }, showBiometric = false, onBiometric = {})
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    }
}
