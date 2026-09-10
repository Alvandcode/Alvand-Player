package com.alvand.player.ui.components

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alvand.player.R
import com.alvand.player.data.AppLocale

/** دیالوگ انتخاب زبان (۲۰ زبان) */
@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = remember { AppLocale.currentTag() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 380.dp)) {
                items(AppLocale.all) { l ->
                    val sel = current == l.code || (l.code == "id" && current == "in")
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable {
                                AppLocale.apply(l.code)
                                (ctx as? Activity)?.recreate()
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
