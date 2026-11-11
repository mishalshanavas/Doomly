package com.doomly.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.doomly.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Int.grouped(): String = String.format(Locale.getDefault(), "%,d", this)

// ── Sheet ──────────────────────────────────────────────────────────────

/** The light card at the top of a screen. Extends under the status bar; rounded at the bottom. */
@Composable
fun TopSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = SheetRadius, bottomEnd = SheetRadius))
            .background(Sheet)
            .statusBarsPadding()
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xl, bottom = Spacing.gutter),
        content = content
    )
}

/** Text area inside the sheet, aligned with inset-card text and body text. */
@Composable
fun SheetContent(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.inset), content = content)
}

/** Grey card nested in the sheet. Radius = sheet radius - gutter. */
@Composable
fun InsetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(InsetRadius)).background(SheetInset)
            .padding(horizontal = Spacing.inset, vertical = Spacing.xl),
        content = content
    )
}

/** Grey label, ink title at the same size, smaller grey meta. */
@Composable
fun LabelStack(label: String, title: String, modifier: Modifier = Modifier, meta: String? = null) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.titleLarge.regular, color = Smoke)
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
        if (meta != null) Text(meta, style = tabular(MaterialTheme.typography.bodyMedium), color = Smoke)
    }
}

/** Thick pill progress with dots marking the remaining track. Turns accent when complete. */
@Composable
fun DottedProgress(progress: Float, modifier: Modifier = Modifier, complete: Boolean = false, description: String = "") {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, tween(motionDuration(500), easing = FastOutSlowInEasing), label = "progress")
    val fill by animateColorAsState(if (complete) Accent else Ink, tween(motionDuration(300)), label = "progress-fill")
    val track = Field
    val dot = Hairline
    Canvas(
        modifier.fillMaxWidth().height(14.dp).semantics {
            if (description.isNotEmpty()) contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
        }
    ) {
        val h = size.height
        val radius = CornerRadius(h / 2f)
        drawRoundRect(track, cornerRadius = radius)
        val fillWidth = if (animated <= 0f) 0f else maxOf(h, size.width * animated)
        if (fillWidth > 0f) drawRoundRect(fill, size = Size(fillWidth, h), cornerRadius = radius)
        val dots = 12
        for (i in 1 until dots) {
            val x = size.width * i / dots
            if (x > fillWidth + h / 2f && x < size.width - h / 2f) {
                drawCircle(dot, radius = 1.5.dp.toPx(), center = Offset(x, h / 2f))
            }
        }
    }
}

/** Fields that live on the sheet: ink focus, no accent. */
@Composable
fun sheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink, unfocusedBorderColor = Hairline,
    focusedLabelColor = Ink, unfocusedLabelColor = Smoke,
    cursorColor = Ink, focusedTextColor = Ink, unfocusedTextColor = Ink,
    focusedContainerColor = Sheet, unfocusedContainerColor = Sheet
)

// ── Tabs ───────────────────────────────────────────────────────────────

private fun Modifier.underline(color: Color): Modifier = drawBehind {
    val y = size.height + 4.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx())
}

/** One text tab: white + underline when selected, grey otherwise. */
@Composable
fun TabLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val color by animateColorAsState(if (selected) BodyInk else BodyMuted, tween(motionDuration(200)), label = "tab")
    val line by animateColorAsState(if (selected) BodyInk else Color.Transparent, tween(motionDuration(200)), label = "tab-line")
    Box(
        modifier.heightIn(min = 44.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = style, color = color, modifier = Modifier.underline(line))
    }
}

/** Text tabs on the black body. */
@Composable
fun TextTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
        tabs.forEachIndexed { index, label -> TabLabel(label, index == selected, { onSelect(index) }) }
    }
}

// ── Body ───────────────────────────────────────────────────────────────

/** Small grey caption above body cards. */
@Composable
fun SectionHeading(word: String, modifier: Modifier = Modifier) {
    Text(word, style = MaterialTheme.typography.labelMedium, color = BodyMuted, modifier = modifier.padding(bottom = Spacing.sm))
}

/** Dark rounded card on the black body. */
@Composable
fun GroupedSection(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(DoomlyShapes.large).background(BodyCard), content = content)
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier.padding(start = Spacing.lg), thickness = 0.5.dp, color = BodySeparator)
}

/** Row inside a dark card: title left, value or control right. Values use tabular numerals. */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 52.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = BodyInk)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BodyMuted)
        }
        if (value != null) Text(value, style = tabular(MaterialTheme.typography.bodyLarge), color = BodyMuted)
        if (trailing != null) { Spacer(Modifier.width(Spacing.sm)); trailing() }
        if (onClick != null && showChevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = BodyMuted)
        }
    }
}

/** Checklist card: empty circle when ahead, ticked and struck through when done. */
@Composable
fun CheckRow(title: String, done: Boolean, modifier: Modifier = Modifier, meta: String? = null) {
    Row(
        modifier.fillMaxWidth().clip(DoomlyShapes.large).background(BodyCard)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (done) "Reached" else "Not reached",
            tint = BodyMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) BodyMuted else BodyInk,
                textDecoration = if (done) TextDecoration.LineThrough else null
            )
            if (meta != null) Text(meta, style = tabular(MaterialTheme.typography.bodySmall), color = BodyMuted)
        }
    }
}

/** The one emphasised action per screen: white pill on black, presses down slightly. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !LocalReduceMotion.current) 0.97f else 1f, tween(motionDuration(120)), label = "press")
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp).graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled && !loading,
        shape = CircleShape,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = BodyInk, contentColor = BodyBg,
            disabledContainerColor = BodyFill, disabledContentColor = BodyMuted
        )
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = BodyMuted, strokeWidth = 2.dp)
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action under the primary button. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = BodyMuted) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** iOS-style switch: accent when on, grey track when off. */
@Composable
fun BodySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, description: String, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = BodyAccent, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = BodyFill, uncheckedBorderColor = Color.Transparent
        )
    )
}

/** This week: seven bars, today in white. */
@Composable
fun WeekChart(history: Map<String, Int>, todayCount: Int, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val first = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val days = (0L..6L).map { first.plusDays(it) }
    val counts = days.map { if (it == today) todayCount else history[it.toString()] ?: 0 }
    val peak = counts.max().coerceAtLeast(1)
    Column(modifier.padding(Spacing.lg)) {
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            days.forEachIndexed { index, date ->
                val count = counts[index]
                val fraction by animateFloatAsState(if (count <= 0) 0.05f else 0.1f + 0.9f * count / peak, tween(motionDuration(400)), label = "bar")
                Box(
                    Modifier.width(24.dp).fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (date == today) BodyInk else BodyFill)
                        .semantics { contentDescription = "$date: $count Reels${if (date == today) ", today" else ""}" }
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { date ->
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault())),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (date == today) BodyInk else BodyMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
    }
}

/** Last 30 days as a dot grid; busier days read whiter. */
@Composable
fun DotMatrix(history: Map<String, Int>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
    val peak = days.maxOf { history[it.toString()] ?: 0 }.coerceAtLeast(1)
    val rows = days.chunked(10)
    Column(modifier.padding(Spacing.lg)) {
        rows.forEachIndexed { row, rowDays ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowDays.forEach { day ->
                    val count = history[day.toString()] ?: 0
                    Canvas(Modifier.size(14.dp).semantics { contentDescription = "$day: $count Reels" }) {
                        val color = if (count <= 0) BodyFill else lerp(BodyFill, BodyInk, 0.3f + 0.7f * count / peak)
                        drawCircle(color, radius = size.minDimension / 2f, center = center)
                    }
                }
            }
            if (row < rows.size - 1) Spacer(Modifier.height(Spacing.md))
        }
    }
}

/** Monochrome initial avatar. */
@Composable
fun DotAvatar(seed: String, modifier: Modifier = Modifier, onBody: Boolean = false) {
    Box(
        modifier.clip(CircleShape).background(if (onBody) BodyFill else SheetInset).semantics { contentDescription = "Avatar" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            seed.firstOrNull { it.isLetter() }?.uppercase() ?: "D",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
            color = if (onBody) BodyInk else Ink
        )
    }
}

/** Pulsing placeholder for the hero number while the first snapshot loads. */
@Composable
fun CounterSkeleton(modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) {
        Box(modifier.padding(vertical = Spacing.sm).width(160.dp).height(76.dp).clip(DoomlyShapes.medium).background(Field))
        return
    }
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "skeleton-alpha"
    )
    Box(modifier.padding(vertical = Spacing.sm).width(160.dp).height(76.dp).clip(DoomlyShapes.medium).background(Field.copy(alpha = alpha)))
}
