package com.doomly.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.doomly.app.*
import com.doomly.app.ui.components.*
import com.doomly.app.ui.theme.*

@Composable
fun PermissionsScreen(resumeCount: Int = 0, onDone: () -> Unit = {}) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableStateOf(DoomStatsStore.DEFAULT_DAILY_TARGET.toString()) }
    var disclosure by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    fun finish() { SetupPreferences.finish(context); onDone() }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) finish() else denied = true }
    LaunchedEffect(resumeCount, step) {
        if (step == 1 && DeviceReadiness.accessibilityEnabled(context)) step = 2
        if (step == 2 && androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) finish()
    }
    val titles = listOf("Set a daily goal", "Turn on Reel tracking", "Allow notifications")
    val captions = listOf(
        "Counting continues past your goal.",
        "Optional. Doomly uses Accessibility to recognise Reels in Instagram.",
        "Optional. Milestones, reminders and updates."
    )
    val goalValid = goal.toIntOrNull() in 1..20_000
    val enterDuration = motionDuration(200)
    val exitDuration = motionDuration(120)

    Column(Modifier.fillMaxSize().background(BodyBg).safeDrawingPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.xxl),
            verticalArrangement = Arrangement.Center
        ) {
            Text("YOUR DOOMLY", style = MaterialTheme.typography.labelLarge, color = BodyMuted)
            Spacer(Modifier.height(Spacing.xxl))
            Column(
                Modifier.fillMaxWidth().background(Sheet, DoomlyShapes.extraLarge)
                    .padding(Spacing.xxl)
            ) {
                SheetContent {
                    AnimatedContent(step, transitionSpec = { fadeIn(tween(enterDuration)) togetherWith fadeOut(tween(exitDuration)) }, label = "step") { s ->
                        LabelStack(label = "Step ${s + 1} of 3", title = titles[s], meta = captions[s])
                    }
                    Spacer(Modifier.height(Spacing.xl))
                    DottedProgress((step + 1) / 3f, description = "Step ${step + 1} of 3")
                }
                Spacer(Modifier.height(Spacing.lg))
                if (step == 0) {
                    InsetCard {
                        OutlinedTextField(
                            goal, { goal = it.filter(Char::isDigit).take(5) }, label = { Text("Reels per day") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = goal.isNotEmpty() && !goalValid,
                            supportingText = { if (goal.isNotEmpty() && !goalValid) Text("1–20,000") },
                            modifier = Modifier.fillMaxWidth(), shape = DoomlyShapes.medium, colors = sheetFieldColors()
                        )
                    }
                } else {
                    SheetContent {
                        Text(
                            if (step == 1) "You stay in control. Enable tracking to count Reels automatically, or skip and set it up later in Profile."
                            else "Choose whether Doomly can send reminders. You can change this anytime in Android Settings.",
                            style = MaterialTheme.typography.bodyLarge, color = Smoke
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = when (step) { 0 -> "Continue"; 1 -> "Turn on tracking"; else -> if (denied) "Open Settings" else "Allow notifications" },
                enabled = step != 0 || goalValid,
                onClick = {
                    when (step) {
                        0 -> { DoomStatsStore.setDailyTarget(context, goal.toInt()); step = 1 }
                        1 -> disclosure = true
                        else -> if (denied || Build.VERSION.SDK_INT < 33) context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                                else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            if (step > 0) QuietButton("Not now", onClick = { if (step == 1) step = 2 else finish() })
            if (denied && step == 2) Text("Doomly works without notifications.", color = BodyMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
    if (disclosure) TrackingDisclosure(onDismiss = { disclosure = false })
}
