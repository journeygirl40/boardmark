package com.boardmark.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boardmark.app.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val PARTICLE_COUNT = 32
private const val DURATION_MS = 1800
private const val LINGER_MS = 400L

private val ConfettiColors = listOf(
    Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A),
    Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFFF7043),
)

private class ConfettiParticle(
    val xFraction: Float,
    val color: Color,
    val fallDelay: Float,
    val swayPhase: Float,
    val rotationSpeed: Float,
)

/**
 * ブックマーク件数の節目を、紙吹雪+短いバナーで一度だけ祝う。タップやスワイプを
 * 一切奪わない(pointerInputを持たせない)ため、表示中もグリッド操作を妨げない。
 * 一定時間で自動的に消える(手動での閉じる操作は不要)。
 */
@Composable
fun MilestoneCelebration(milestone: Int, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val particles = remember(milestone) {
        List(PARTICLE_COUNT) {
            ConfettiParticle(
                xFraction = Random.nextFloat(),
                color = ConfettiColors.random(),
                fallDelay = Random.nextFloat() * 0.3f,
                swayPhase = Random.nextFloat() * (2 * PI.toFloat()),
                rotationSpeed = Random.nextFloat() * 6f - 3f,
            )
        }
    }
    val progress = remember(milestone) { Animatable(0f) }

    LaunchedEffect(milestone) {
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        delay(LINGER_MS)
        onDismiss()
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val currentProgress = progress.value
            particles.forEach { particle ->
                val localProgress = ((currentProgress - particle.fallDelay) / (1f - particle.fallDelay))
                    .coerceIn(0f, 1f)
                if (localProgress <= 0f) return@forEach
                val y = size.height * localProgress
                val sway = sin(localProgress * 4f + particle.swayPhase) * (size.width * 0.04f)
                val x = size.width * particle.xFraction + sway
                val alpha = (1f - localProgress).coerceIn(0f, 1f)
                val particleSize = 8.dp.toPx()
                rotate(degrees = localProgress * particle.rotationSpeed * 180f, pivot = Offset(x, y)) {
                    drawRect(
                        color = particle.color.copy(alpha = alpha),
                        topLeft = Offset(x - particleSize / 2, y - particleSize / 2),
                        size = Size(particleSize, particleSize),
                    )
                }
            }
        }

        val bannerScale = (progress.value * 6f).coerceIn(0f, 1f)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .graphicsLayer {
                    scaleX = 0.85f + bannerScale * 0.15f
                    scaleY = 0.85f + bannerScale * 0.15f
                    alpha = bannerScale
                },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Text(
                text = if (milestone == 1) {
                    stringResource(R.string.milestone_first_message)
                } else {
                    stringResource(R.string.milestone_message, milestone)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
    }
}
