package com.alvand.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.AppViewModel
import com.alvand.player.R
import com.alvand.player.data.ThemeMode
import com.alvand.player.ui.theme.LocalAP

/** دیالوگ انتخاب تم: سیستم / روشن / تیره */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val pal = LocalAP.current
    val mode by vm.themeMode.collectAsState()
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
        }
    }
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
