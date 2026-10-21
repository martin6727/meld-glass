package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.metrolist.music.R
import com.metrolist.music.constants.AppIconNameKey
import com.metrolist.music.constants.AppIconStyleKey
import com.metrolist.music.ui.theme.pressScale
import com.metrolist.music.utils.AppIconName
import com.metrolist.music.utils.AppIconStyle
import com.metrolist.music.utils.AppIcons
import com.metrolist.music.utils.rememberPreference

/** Pick the home-screen icon and name, with a live preview. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppIconPickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val (savedStyle, onSavedStyleChange) = rememberPreference(AppIconStyleKey, AppIconStyle.GLASS.name)
    val (savedName, onSavedNameChange) = rememberPreference(AppIconNameKey, AppIconName.MELD_GLASS.name)
    var style by remember { mutableStateOf(AppIconStyle.entries.find { it.name == savedStyle } ?: AppIconStyle.GLASS) }
    var name by remember { mutableStateOf(AppIconName.entries.find { it.name == savedName } ?: AppIconName.MELD_GLASS) }
    val changedText = stringResource(R.string.app_icon_changed)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.app_icon), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(20.dp))

                // How it will look on the home screen
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AppIconImage(style, 76.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(name.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.icon_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    AppIconStyle.entries.forEach { option ->
                        IconOption(option, selected = option == style, onClick = { style = option })
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.name_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIconName.entries.forEach { option ->
                        FilterChip(
                            selected = option == name,
                            onClick = { name = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_icon_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            AppIcons.apply(context, style, name)
                            onSavedStyleChange(style.name)
                            onSavedNameChange(name.name)
                            Toast.makeText(context, changedText, Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun IconOption(
    style: AppIconStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "iconRing",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pressScale(interaction, 0.92f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .border(2.dp, ring, RoundedCornerShape(20.dp))
                .padding(4.dp),
        ) {
            AppIconImage(style, 54.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(style.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Renders the real launcher icon (adaptive icons draw already masked to the system shape)
@Composable
private fun AppIconImage(
    style: AppIconStyle,
    size: Dp,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap =
        remember(style, px) {
            runCatching { ContextCompat.getDrawable(context, style.mipmap)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
        }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(style.labelRes),
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f)),
        )
    } else {
        Spacer(Modifier.size(size))
    }
}
