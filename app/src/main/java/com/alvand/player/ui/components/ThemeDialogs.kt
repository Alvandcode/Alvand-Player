package com.alvand.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.data.AccentThemeMode
import com.alvand.player.data.ThemeMode
import com.alvand.player.ui.theme.AccentThemes
import com.alvand.player.ui.theme.LocalAP

/** دیالوگ انتخاب تم: حالت روشن/تیره + تم رنگی */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val pal = LocalAP.current
    val mode by vm.themeMode.collectAsState()
    val accentId by vm.accentTheme.collectAsState()
    val options = listOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = pal.sheet) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, null, tint = pal.ink)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.theme),
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            options.forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { vm.setThemeMode(value); onDismiss() }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = mode == value, onClick = { vm.setThemeMode(value); onDismiss() })
                    Spacer(Modifier.width(10.dp))
                    Text(label, color = pal.ink, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = pal.line)
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = pal.ink)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.theme_color),
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(12.dp))

            // چهار تم رنگی به‌صورت شبکهٔ ۲×۲ با پیش‌نمایش گرادیان
            AccentThemes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { theme ->
                        val label = stringResource(accentLabelRes(theme.id))
                        val selected = theme.id == accentId
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(theme.accent(pal.isDark), theme.deep(pal.isDark))
                                        )
                                    )
                                    .clickable { vm.setAccentTheme(theme.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = label,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                label,
                                color = if (selected) pal.ink else pal.sub,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.close), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** نام نمایشی هر تم رنگی */
private fun accentLabelRes(id: Int): Int = when (id) {
    AccentThemeMode.ROYAL -> R.string.accent_royal
    AccentThemeMode.SUNSET -> R.string.accent_sunset
    AccentThemeMode.OCEAN -> R.string.accent_ocean
    else -> R.string.accent_mono
}

/** دیالوگ بکگراند دلخواه: انتخاب عکس / حذف و برگشت به پیش‌فرض */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundDialog(
    vm: AppViewModel,
    onPickImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val pal = LocalAP.current
    val current by vm.backgroundUri.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = pal.sheet) {
        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 36.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Image, null, tint = pal.ink)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.background),
                    color = pal.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (current != null) stringResource(R.string.background_set)
                else stringResource(R.string.background_desc),
                color = pal.sub, fontSize = 13.sp
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { onPickImage(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pal.ink, contentColor = pal.bg
                )
            ) { Text(stringResource(R.string.background_pick)) }
            if (current != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { vm.setBackground(null); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) { Text(stringResource(R.string.background_remove)) }
            }
        }
    }
}
