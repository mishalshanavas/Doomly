package com.doomly.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.doomly.app.R
import com.doomly.app.ui.theme.*
import kotlinx.coroutines.delay

/** A tiny, user-triggered high five. No idle loop competing with the main content. */
@Composable
fun PlayfulPawn() {
    var taps by remember { mutableIntStateOf(0) }
    val wiggle = remember { Animatable(0f) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        wiggle.snapTo(0f)
        wiggle.animateTo(0f, keyframes {
            durationMillis = 420
            -1f at 85
            1f at 185
            -.45f at 290
            0f at 420
        })
    }
    Box(Modifier.size(56.dp).clickable(role = Role.Button, onClickLabel = "High-five your procrastination pal") { taps++ },
        Alignment.Center) {
        BoardArt(R.drawable.board_pawn, Modifier.size(48.dp).graphicsLayer {
            rotationZ = wiggle.value * 12f
            translationY = -kotlin.math.abs(wiggle.value) * 5.dp.toPx()
        }, "Your procrastination pal")
    }
}

/** A short celebration driven by a newly crossed milestone, not by recomposition. */
@Composable
fun MilestonePop(milestone: Int, event: Int) {
    var visible by remember { mutableStateOf(false) }
    val burst = remember { Animatable(0f) }
    val mint = Mint
    val ink = Frost
    var handledEvent by rememberSaveable { mutableIntStateOf(event) }
    LaunchedEffect(event) {
        if (event == handledEvent) return@LaunchedEffect
        handledEvent = event
        if (event == 0) { visible = false; return@LaunchedEffect }
        visible = true
        burst.snapTo(0f)
        burst.animateTo(1f, androidx.compose.animation.core.tween(650))
        delay(2_000)
        visible = false
    }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp), Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    repeat(10) { index ->
                        val angle = index * kotlin.math.PI * 2 / 10
                        val distance = size.minDimension * .46f * burst.value
                        drawCircle(
                            color = (if (index % 2 == 0) mint else ink).copy(alpha = 1f - burst.value),
                            radius = 2.dp.toPx(),
                            center = center + Offset(kotlin.math.cos(angle).toFloat() * distance,
                                kotlin.math.sin(angle).toFloat() * distance)
                        )
                    }
                }
                BoardArt(R.drawable.board_trophy, Modifier.size(30.dp).graphicsLayer {
                    scaleX = .8f + burst.value * .2f
                    scaleY = scaleX
                })
            }
            Spacer(Modifier.width(10.dp))
            Text("$milestone Reels. Elite thumb work.", style = MaterialTheme.typography.bodySmall, color = Frost)
        }
    }
}
