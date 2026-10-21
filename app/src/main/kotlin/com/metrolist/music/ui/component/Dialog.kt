/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.LocalNavController
import com.metrolist.music.R
import com.metrolist.music.ui.screens.settings.AccountSettings
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavController

@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = horizontalAlignment,
                modifier =
                    modifier
                        .padding(24.dp),
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                        Box(
                            Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            icon()
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally),
                            ) {
                                title()
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }

                content()

                if (buttons != null) {
                    Spacer(Modifier.height(24.dp))

                    FlowRow(
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.labelLarge,
                            ) {
                                buttons()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSettingsDialog(
    onDismiss: () -> Unit,
    latestVersionName: String,
) {
    val navController = LocalNavController.current
    // Pops out of the avatar (top right) and shrinks back into it when closed
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val close = { visibleState.targetState = false }
    LaunchedEffect(visibleState.isIdle, visibleState.targetState) {
        if (!visibleState.targetState && visibleState.isIdle) onDismiss()
    }

    Dialog(
        onDismissRequest = close,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = true,
            ),
    ) {
        // Frosted blur over the app behind the menu (Android 12+)
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0.35f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.also { it.blurBehindRadius = 90 }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { close() }
                        )
                    },
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
                    initialScale = 0.6f,
                    transformOrigin = TransformOrigin(0.92f, 0f),
                ) + fadeIn(tween(160)),
                exit = scaleOut(
                    animationSpec = tween(170),
                    targetScale = 0.7f,
                    transformOrigin = TransformOrigin(0.92f, 0f),
                ) + fadeOut(tween(150)),
            ) {
                val cardShape = RoundedCornerShape(32.dp)
                Box(
                    modifier =
                        Modifier
                            .padding(top = 60.dp, start = 12.dp, end = 12.dp)
                            .widthIn(max = 440.dp)
                            .fillMaxWidth()
                            .shadow(32.dp, cardShape, ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(cardShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), cardShape)
                            // Taps on the card itself shouldn't close it
                            .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    AccountSettings(
                        navController = navController,
                        onClose = close,
                        latestVersionName = latestVersionName,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPromptDialog(
    title: String? = null,
    titleBar: @Composable (RowScope.() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReset: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title =
            if (titleBar != null) {
                { Row { titleBar() } }
            } else if (title != null) {
                {
                    Text(
                        text = title,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            } else {
                null
            },
        buttons = {
            if (onReset != null) {
                Row(modifier = Modifier.weight(1f)) {
                    TextButton(
                        onClick = { onReset() },
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                }
            }

            if (onCancel != null) {
                TextButton(
                    onClick = { onCancel() },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }

            TextButton(
                onClick = { onConfirm() },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        content()
    }
}

@Composable
fun ListDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    modifier
                        .padding(vertical = 24.dp)
                        .imePadding(),
            ) {
                LazyColumn(content = content)
            }
        }
    }
}

@Composable
fun InfoLabel(text: String) =
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(4.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    initialTextFieldValue: TextFieldValue = TextFieldValue(),
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    autoFocus: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 10,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int? = null,
    onDone: (String) -> Unit = {},
    // new multi-field support
    textFields: List<Pair<String, TextFieldValue>>? = null,
    onTextFieldsChange: ((Int, TextFieldValue) -> Unit)? = null,
    onDoneMultiple: ((List<String>) -> Unit)? = null,
    onDismiss: () -> Unit,
    autoDismiss: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val legacyFieldState = remember { mutableStateOf(initialTextFieldValue) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        icon = icon,
        title = title,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }

            val isValid =
                textFields?.all { isInputValid(it.second.text) }
                    ?: isInputValid(legacyFieldState.value.text)

            TextButton(
                enabled = isValid,
                onClick = {
                    if (autoDismiss) onDismiss()
                    if (textFields != null && onDoneMultiple != null) {
                        onDoneMultiple(textFields.map { it.second.text })
                    } else {
                        onDone(legacyFieldState.value.text)
                    }
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f, fill = false),
        ) {
            if (textFields != null) {
                textFields.forEachIndexed { index, (label, value) ->
                    TextField(
                        value = value,
                        onValueChange = { onTextFieldsChange?.invoke(index, it) },
                        placeholder = { Text(label) },
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions =
                            KeyboardOptions(
                                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                                keyboardType = keyboardType,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    if (onDoneMultiple != null) {
                                        onDoneMultiple(textFields.map { it.second.text })
                                        if (autoDismiss) onDismiss()
                                    }
                                },
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (index < textFields.size - 1) 12.dp else 0.dp)
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
            } else {
                // Wrap in Box with pointerInput to prevent double-click crashes in TextClassifier
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { /* Consume double-tap to prevent TextClassifier crash */ },
                                )
                            },
                ) {
                    TextField(
                        value = legacyFieldState.value,
                        onValueChange = {
                            legacyFieldState.value =
                                if (maxLength != null && it.text.length > maxLength) {
                                    it.copy(text = it.text.take(maxLength))
                                } else {
                                    it
                                }
                        },
                        placeholder = placeholder,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions =
                            KeyboardOptions(
                                imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                                keyboardType = keyboardType,
                            ),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    onDone(legacyFieldState.value.text)
                                    if (autoDismiss) onDismiss()
                                },
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                    )
                }
                if (maxLength != null) {
                    Text(
                        text = "${legacyFieldState.value.text.length}/$maxLength",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (legacyFieldState.value.text.length >= maxLength) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier =
                            Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp),
                    )
                }
            }

            extraContent?.invoke()
        }
    }
}
