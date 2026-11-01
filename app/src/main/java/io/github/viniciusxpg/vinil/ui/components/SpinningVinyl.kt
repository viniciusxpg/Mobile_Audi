package io.github.viniciusxpg.vinil.ui.components

import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

/** Tempo de uma volta completa do disco. Mais lento que um vinil real (1,8 s) para a capa continuar legível. */
private const val REVOLUTION_MS = 10_000

private val DiscColor = Color(0xFF161616)
private val GrooveColor = Color(0xFF2E2E2E)
private val GrooveRadii = listOf(0.95f, 0.89f, 0.83f, 0.77f, 0.71f, 0.66f)

/**
 * Disco de vinil com a capa do álbum no selo central. Gira enquanto [isPlaying];
 * ao pausar, para no ângulo em que está. O reflexo de luz fica fixo, como num disco de verdade.
 */
@Composable
fun SpinningVinyl(
    contentUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Respeita "Remover animações" das configurações de acessibilidade.
    val animationsEnabled = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying, animationsEnabled) {
        if (!isPlaying || !animationsEnabled) return@LaunchedEffect
        while (isActive) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = tween(durationMillis = REVOLUTION_MS, easing = LinearEasing),
            )
            rotation.snapTo(rotation.value % 360f)
        }
    }

    val holeColor = MaterialTheme.colorScheme.background

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        // Parte que gira: disco, sulcos, selo com a capa e furo central.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(DiscColor, radius)
                GrooveRadii.forEach { fraction ->
                    drawCircle(GrooveColor, radius * fraction, style = Stroke(width = 1.dp.toPx()))
                }
            }
            AlbumArt(
                contentUri = contentUri,
                modifier = Modifier.fillMaxSize(0.6f),
                cornerRadius = 1000.dp, // vira círculo
                sizePx = 768,
            )
            Box(
                Modifier
                    .fillMaxSize(0.05f)
                    .clip(CircleShape)
                    .background(holeColor),
            )
        }

        // Reflexo de luz (não gira).
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val arcRadius = radius * 0.86f
            val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
            val arcSize = Size(arcRadius * 2, arcRadius * 2)
            val stroke = Stroke(width = radius * 0.05f, cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.12f), 200f, 50f, false, topLeft, arcSize, style = stroke)
            drawArc(Color.White.copy(alpha = 0.07f), 20f, 50f, false, topLeft, arcSize, style = stroke)
        }
    }
}
