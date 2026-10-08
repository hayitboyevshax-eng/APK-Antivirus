package uz.apk.antivirus.security

import android.util.LruCache
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.apk.antivirus.security.engine.AppScanResult
import uz.apk.antivirus.security.engine.ThreatLevel
import uz.apk.antivirus.security.ui.theme.CyberCardSurface
import uz.apk.antivirus.security.ui.theme.CyberDangerRed
import uz.apk.antivirus.security.ui.theme.CyberNeonCyan
import uz.apk.antivirus.security.ui.theme.CyberNeonGreen
import uz.apk.antivirus.security.ui.theme.CyberTextMuted
import uz.apk.antivirus.security.ui.theme.CyberTextPrimary
import uz.apk.antivirus.security.ui.theme.CyberTextSecondary
import uz.apk.antivirus.security.ui.theme.CyberWarningOrange
import uz.apk.antivirus.security.ui.theme.CyberWarningYellow

/** Ro'yxatni toifalash uchun filtr turlari. */
enum class AppFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    RISKY(R.string.filter_risky),
    USER(R.string.filter_user),
    SYSTEM(R.string.filter_system)
}

@Composable
fun CyberTopBar(isRealTimeActive: Boolean) {
    val statusColor = if (isRealTimeActive) CyberNeonGreen else CyberDangerRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.top_title),
            color = CyberNeonGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (isRealTimeActive) R.string.realtime_on else R.string.realtime_off
                ),
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

/** Faqat skanerlash paytida aylanadi, shuning uchun bo'sh turganda batareya sarflamaydi. */
@Composable
private fun RadarSweep(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "sweep"
    )
    Canvas(modifier = modifier) {
        val c = center
        val radius = size.minDimension / 2f
        rotate(degrees = angle, pivot = c) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, color.copy(alpha = 0.45f)),
                    center = c
                ),
                radius = radius,
                center = c
            )
            drawLine(
                color = color,
                start = c,
                end = Offset(c.x + radius, c.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun CyberShieldIndicator(
    isScanning: Boolean,
    threatsFound: Int,
    onStartDeepScan: () -> Unit,
    modifier: Modifier = Modifier,
    radarSize: Dp = 180.dp
) {
    val haptic = LocalHapticFeedback.current

    val statusColor = when {
        isScanning -> CyberNeonCyan
        threatsFound > 0 -> CyberDangerRed
        else -> CyberNeonGreen
    }
    val statusText = when {
        isScanning -> stringResource(R.string.status_scanning)
        threatsFound > 0 -> stringResource(R.string.status_threats, threatsFound)
        else -> stringResource(R.string.status_protected)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(radarSize)
                .clip(CircleShape)
                .background(CyberCardSurface)
                .border(2.dp, statusColor.copy(alpha = 0.6f), CircleShape)
                .clickable(
                    enabled = !isScanning,
                    onClickLabel = stringResource(R.string.radar_action),
                    role = Role.Button
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartDeepScan()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = center
                val radius = size.minDimension / 2f
                val ringStroke = Stroke(width = 1.dp.toPx())
                drawCircle(statusColor.copy(alpha = 0.18f), radius * 0.33f, c, style = ringStroke)
                drawCircle(statusColor.copy(alpha = 0.18f), radius * 0.66f, c, style = ringStroke)
                drawLine(statusColor.copy(alpha = 0.12f), Offset(c.x - radius, c.y), Offset(c.x + radius, c.y))
                drawLine(statusColor.copy(alpha = 0.12f), Offset(c.x, c.y - radius), Offset(c.x, c.y + radius))
            }
            if (isScanning) {
                RadarSweep(color = statusColor, modifier = Modifier.fillMaxSize())
            }
            Icon(
                imageVector = if (!isScanning && threatsFound > 0) Icons.Default.Warning else Icons.Default.Security,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(radarSize * 0.31f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        if (!isScanning) {
            Text(
                text = stringResource(R.string.rescan_hint),
                color = CyberTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(CyberCardSurface)
            .border(1.dp, accentColor.copy(alpha = 0.4f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = CyberTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppFilterRow(
    selected: AppFilter,
    onSelected: (AppFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(filter) },
                label = { Text(text = stringResource(filter.labelRes), fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = CyberCardSurface,
                    labelColor = CyberTextSecondary,
                    selectedContainerColor = CyberNeonGreen.copy(alpha = 0.18f),
                    selectedLabelColor = CyberNeonGreen
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) CyberNeonGreen.copy(alpha = 0.6f) else CyberTextMuted.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CyberTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

private val iconCache = LruCache<String, ImageBitmap>(256)

/** Ilovaning haqiqiy ikonkasini fon oqimida yuklaydi va keshlaydi. */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(iconCache.get(packageName), packageName) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName)
                        .toBitmap(width = 96, height = 96)
                        .asImageBitmap()
                }.getOrNull()?.also { iconCache.put(packageName, it) }
            }
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            tint = CyberTextMuted,
            modifier = modifier.padding(8.dp)
        )
    }
}

@Composable
fun AppThreatCard(
    app: AppScanResult,
    onUninstall: () -> Unit
) {
    val levelColor = when (app.threatLevel) {
        ThreatLevel.SAFE -> CyberNeonGreen
        ThreatLevel.LOW -> CyberNeonCyan
        ThreatLevel.MEDIUM -> CyberWarningYellow
        ThreatLevel.HIGH -> CyberWarningOrange
        ThreatLevel.CRITICAL -> CyberDangerRed
    }
    val levelLabel = stringResource(
        when (app.threatLevel) {
            ThreatLevel.SAFE -> R.string.level_safe
            ThreatLevel.LOW -> R.string.level_low
            ThreatLevel.MEDIUM -> R.string.level_medium
            ThreatLevel.HIGH -> R.string.level_high
            ThreatLevel.CRITICAL -> R.string.level_critical
        }
    )
    val systemBadge = stringResource(R.string.badge_system)
    val permBadge = if (app.dangerousPermissions.isNotEmpty()) {
        stringResource(R.string.badge_permissions, app.dangerousPermissions.size)
    } else null
    val canUninstall = !app.isSystemApp &&
        (app.threatLevel == ThreatLevel.MEDIUM ||
            app.threatLevel == ThreatLevel.HIGH ||
            app.threatLevel == ThreatLevel.CRITICAL)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CyberCardSurface)
            .border(1.dp, levelColor.copy(alpha = 0.3f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ilova ikonkasi + xavf balli nishoni
        Box(modifier = Modifier.size(52.dp)) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier
                    .size(46.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, levelColor, RoundedCornerShape(12.dp))
            )
            Text(
                text = app.threatScore.toString(),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(50))
                    .background(levelColor)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = CyberTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = CyberTextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.reason,
                color = CyberTextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(levelLabel)
                    if (app.isSystemApp) append("  •  ").append(systemBadge)
                    if (permBadge != null) append("  •  ").append(permBadge)
                },
                color = levelColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (canUninstall) {
            // IconButton standart 48dp teginish maydonini beradi
            IconButton(onClick = onUninstall) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.uninstall_cd, app.appName),
                    tint = CyberDangerRed
                )
            }
        }
    }
}
