package com.jaydorah.alzhimers.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jaydorah.alzhimers.R
import com.jaydorah.alzhimers.core.AppPreferences
import com.jaydorah.alzhimers.core.LanguageOptions
import com.jaydorah.alzhimers.core.ThemePref
import com.jaydorah.alzhimers.notifications.ReminderScheduler
import com.jaydorah.alzhimers.notifications.ReminderWorker
import com.jaydorah.alzhimers.ui.components.YButton
import com.jaydorah.alzhimers.ui.components.YButtonVariant
import com.jaydorah.alzhimers.ui.components.YCard
import com.jaydorah.alzhimers.ui.components.YOptionRow
import com.jaydorah.alzhimers.ui.components.YSeparator

@Composable
fun SettingsScreen(
    themePref: ThemePref,
    fontScale: Float,
    notificationsEnabled: Boolean,
    languageCode: String,
    onSetTheme: (ThemePref) -> Unit,
    onSetFontScale: (Float) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = AppPreferences.get(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        prefs.notificationsEnabled = granted
        onSetNotifications(granted)
        if (granted) ReminderWorker.schedule(context) else ReminderWorker.cancel(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            stringResource(R.string.settings_appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ThemePref.entries.forEach { pref ->
                        YButton(
                            text = when (pref) {
                                ThemePref.System -> stringResource(R.string.theme_system)
                                ThemePref.Light -> stringResource(R.string.theme_light)
                                ThemePref.Dark -> stringResource(R.string.theme_dark)
                            },
                            onClick = { onSetTheme(pref) },
                            variant = if (themePref == pref) YButtonVariant.Default else YButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                LanguageOptions.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        row.forEach { option ->
                            FilterChip(
                                selected = languageCode == option.code,
                                onClick = { onSetLanguage(option.code) },
                                label = {
                                    Text(
                                        if (option.code == "system") stringResource(R.string.lang_follow_system) else option.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_text_size, (fontScale * 100).toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Slider(
                    value = fontScale,
                    onValueChange = { onSetFontScale(it) },
                    valueRange = AppPreferences.MIN_FONT_SCALE..AppPreferences.MAX_FONT_SCALE,
                    steps = 5,
                )
                Text(
                    stringResource(R.string.settings_text_size_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            stringResource(R.string.settings_notifications),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        YCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                YOptionRow(
                    title = stringResource(R.string.daily_reminder_title),
                    subtitle = stringResource(R.string.daily_reminder_subtitle),
                    checked = notificationsEnabled,
                    onToggle = {
                        if (!notificationsEnabled) {
                            val canPost = Build.VERSION.SDK_INT < 33 ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                            if (canPost) {
                                prefs.notificationsEnabled = true
                                onSetNotifications(true)
                                ReminderWorker.schedule(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            prefs.notificationsEnabled = false
                            onSetNotifications(false)
                            ReminderWorker.cancel(context)
                        }
                    },
                )
                YSeparator()
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}
