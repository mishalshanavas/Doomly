package com.doomly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*

@Composable
fun MoodFace(progress: Float, modifier: Modifier = Modifier) {
    val mood = progress.coerceIn(0f, 1f)
    Canvas(modifier.aspectRatio(1.55f)) {
        val eyeY = size.height * .32f
        val eyeR = size.minDimension * .12f
        listOf(size.width * .35f, size.width * .65f).forEach { x ->
            drawCircle(Color.White, eyeR, Offset(x, eyeY))
            drawCircle(Ink, eyeR * .43f, Offset(x - eyeR * .14f + mood * eyeR * .25f, eyeY))
            drawCircle(Color.White, eyeR * .12f, Offset(x - eyeR * .28f + mood * eyeR * .25f, eyeY - eyeR * .2f))
        }
        val mouthY = size.height * .68f
        drawOval(Ink, Offset(size.width * .32f, mouthY - size.height * .08f), Size(size.width * .36f, size.height * .16f))
        val smile = Path().apply {
            moveTo(size.width * .38f, size.height * .69f)
            quadraticTo(size.width * .5f, size.height * (.76f + mood * .04f), size.width * .62f, size.height * .69f)
            quadraticTo(size.width * .5f, size.height * .8f, size.width * .38f, size.height * .69f)
        }
        drawPath(smile, Coral)
    }
}

@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Card).padding(20.dp), content = content)
}

@Composable
fun StatusDot(done: Boolean) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(if (done) Mint else Line), contentAlignment = Alignment.Center) {
        if (done) Canvas(Modifier.size(12.dp)) {
            val path = Path().apply { moveTo(1f, size.height * .55f); lineTo(size.width * .4f, size.height - 1f); lineTo(size.width - 1f, 1f) }
            drawPath(path, Ink, style = Stroke(width = 2.3f))
        }
    }
}
