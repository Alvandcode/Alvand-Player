package com.alvand.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alvand.player.R
import com.alvand.player.data.AppLocale
import com.alvand.player.ui.theme.LocalAP

/** دیالوگ انتخاب زبان (۱۷ زبان) — بدون recreate خشن؛ per-app locale خودش هندل می‌کند */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    var current by remember { mutableStateOf(AppLocale.currentTag()) }
    val pal = LocalAP.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pal.sheet,
        titleContentColor = pal.ink,
        textContentColor = pal.ink,
        title = { Text(stringResource(R.string.language)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(AppLocale.all, key = { it.code }) { l ->
                    val sel = current == l.code
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                AppLocale.apply(l.code)
                                current = l.code
                                // recreate لازم نیست؛ AppCompat خودش اکتیویتی را بازسازی می‌کند
                                // و صف پخش چون در سرویس است نمی‌پرد
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sel, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(l.nativeName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
