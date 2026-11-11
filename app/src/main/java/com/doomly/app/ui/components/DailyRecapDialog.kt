package com.doomly.app.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.doomly.app.DailyRecap
import com.doomly.app.R
import com.doomly.app.ui.theme.*
import java.io.File
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DailyRecapDialog(recap: DailyRecap, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(recap) { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    LaunchedEffect(recap) {
        bitmap = withContext(Dispatchers.Default) { renderDailyRecap(context, recap) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = DoomlyShapes.extraLarge, color = Sheet) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Your daily wrap", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(Spacing.md))
                Box(Modifier.weight(1f, fill = false)) {
                    bitmap?.let {
                        Image(it.asImageBitmap(), contentDescription = "${recap.date}: ${recap.reels} reels, daily goal ${recap.goal}, ${recap.streak}-day streak. Seven-day recorded reel chart.",
                            modifier = Modifier.fillMaxWidth().aspectRatio(1080f / 1920f))
                    } ?: CircularProgressIndicator(Modifier.padding(Spacing.xxl))
                }
                error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(Spacing.md))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close", color = Smoke) }
                    TextButton(enabled = bitmap != null && !sharing, onClick = {
                        scope.launch {
                            sharing = true
                            error = null
                            try {
                                val image = bitmap ?: return@launch
                                val uri = withContext(Dispatchers.IO) {
                                    val directory = File(context.cacheDir, "daily-recaps")
                                    if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create recap directory")
                                    val file = File.createTempFile("doomly-${recap.date}-", ".png", directory)
                                    file.outputStream().use { if (!image.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("Cannot save recap") }
                                    FileProvider.getUriForFile(context, "${context.packageName}.share-files", file)
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TEXT, "${recap.reels} reels today. My daily Doomly wrap.")
                                    clipData = ClipData.newRawUri("Daily Doomly wrap", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share your daily wrap"))
                            } catch (_: android.content.ActivityNotFoundException) {
                                error = "No app available to share images."
                            } catch (_: IOException) {
                                error = "Couldn't save the image. Please try again."
                            } finally { sharing = false }
                        }
                    }) { Text(if (sharing) "Preparing…" else "Share image", color = Ink) }
                }
            }
        }
    }
}

internal fun renderDailyRecap(context: Context, recap: DailyRecap): Bitmap {
    val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val black = Color.BLACK
    val paper = Color.rgb(245, 244, 239)
    val lime = Color.rgb(194, 255, 92)
    fun text(value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, maxWidth: Float = 920f) {
        paint.color = color
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        paint.textSize = size
        val width = paint.measureText(value)
        if (width > maxWidth) paint.textSize *= maxWidth / width
        canvas.drawText(value, x, y, paint)
    }
    fun rect(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 0f) {
        paint.color = color
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
    }
    canvas.drawColor(paper)
    context.getDrawable(R.drawable.ic_launcher_foreground)?.let {
        it.setBounds(-6, -76, 318, 248)
        it.draw(canvas)
    }
    text("DOOMLY", 270f, 116f, 42f, black, true)
    text("DAILY WRAP / ${recap.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH)}",
        80f, 220f, 26f, black)
    text("THE DAILY", 72f, 376f, 116f, black, true)
    text("DOOM.", 72f, 498f, 116f, black, true)
    rect(64f, 558f, 1016f, 1100f, black, 40f)
    text(String.format(Locale.US, "%,d", recap.reels), 100f, 850f, 238f, paper, true, 880f)
    text("REELS TODAY", 104f, 926f, 36f, lime, true)
    text(recap.headline(), 104f, 1020f, 40f, paper)
    text("${recap.goalPercent()}% OF DAILY GOAL", 80f, 1186f, 28f, black, true, 550f)
    text("${recap.streak}-DAY STREAK", 690f, 1186f, 28f, black, true, 310f)
    rect(80f, 1220f, 1000f, 1234f, Color.rgb(217, 216, 211), 7f)
    if (recap.goalPercent() > 0) rect(80f, 1220f, 80f + 920f * recap.goalPercent() / 100f, 1234f, black, 7f)
    text("Daily goal: ${String.format(Locale.US, "%,d", recap.goal)} reels", 80f, 1292f, 28f, black)
    text("THE LAST 7 DAYS", 80f, 1400f, 30f, black, true)
    val peak = recap.weekCounts.max().coerceAtLeast(1)
    recap.weekCounts.forEachIndexed { index, count ->
        val x = 90f + index * 134f
        val height = 180f * count / peak
        rect(x, 1640f - height.coerceAtLeast(4f), x + 84f, 1640f, if (index == 6) black else Color.rgb(190, 189, 184), 12f)
        text(String.format(Locale.US, "%,d", count), x, 1618f - height, 22f, black, maxWidth = 100f)
        text(recap.date.minusDays((6 - index).toLong()).format(DateTimeFormatter.ofPattern("EE", Locale.ENGLISH)),
            x, 1690f, 23f, black, maxWidth = 100f)
    }
    rect(0f, 1770f, 1080f, 1920f, lime)
    text("YOUR SCROLL. IN PERSPECTIVE.", 80f, 1844f, 32f, black, true)
    text("Recorded with Doomly", 80f, 1892f, 25f, black)
    return bitmap
}
