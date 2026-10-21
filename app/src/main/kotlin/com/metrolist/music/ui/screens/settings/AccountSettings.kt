/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import timber.log.Timber
import com.metrolist.music.utils.reportException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.InfoLabel
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.HomeViewModel

@Composable
fun AccountSettings(
    navController: NavController,
    onClose: () -> Unit,
    latestVersionName: String
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (authUser, onAuthUserChange) = rememberPreference(InnerTubeAuthUserKey, "0")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val showUpdate = BuildConfig.UPDATER_AVAILABLE && latestVersionName != BuildConfig.BASE_VERSION_NAME

    // Logout confirmation dialog
    if (showLogoutDialog) {
        DefaultDialog(
            onDismiss = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_dialog_title)) },
            content = {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        Timber.d("[LOGOUT_CLEAR] User chose to clear data")
                        scope.launch {
                            try {
                                Timber.d("[LOGOUT_CLEAR] Starting clear and logout process")
                                // Forget account first (stops all sync), then clear data.
                                // This prevents background syncs from re-adding songs.
                                accountSettingsViewModel.logoutAndClearLibraryData(context)
                                Timber.d("[LOGOUT_CLEAR] Library data cleared and account forgotten")
                            } catch (e: Exception) {
                                Timber.e(e, "[LOGOUT_CLEAR] Error clearing library data, proceeding with logout")
                                reportException(e)
                            }
                            onInnerTubeCookieChange("")
                            Timber.d("[LOGOUT_CLEAR] Logout complete")
                            showLogoutDialog = false
                            onClose()
                        }
                    }
                ) {
                    Text(stringResource(R.string.logout_clear))
                }
                TextButton(
                    onClick = {
                        Timber.d("[LOGOUT_KEEP] User chose to keep data")
                        scope.launch {
                            Timber.d("[LOGOUT_KEEP] Starting logout process (keeping data)")
                            accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                            Timber.d("[LOGOUT_KEEP] Logout complete")
                            showLogoutDialog = false
                            onClose()
                        }
                    }
                ) {
                    Text(stringResource(R.string.logout_keep))
                }
            }
        )
    }

    if (showTokenEditor) {
        val text = """
            ***INNERTUBE COOKIE*** =$innerTubeCookie
            ***VISITOR DATA*** =$visitorData
            ***DATASYNC ID*** =$dataSyncId
            ***AUTH USER*** =$authUser
            ***ACCOUNT NAME*** =$accountNamePref
            ***ACCOUNT EMAIL*** =$accountEmail
            ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
        """.trimIndent()

        TextFieldDialog(
            initialTextFieldValue = TextFieldValue(text),
            onDone = { data ->
                var cookie = ""
                var visitorDataValue = ""
                var dataSyncIdValue = ""
                var authUserValue = "0"
                var accountNameValue = ""
                var accountEmailValue = ""
                var accountChannelHandleValue = ""

                data.split("\n").forEach {
                    when {
                        it.startsWith("***INNERTUBE COOKIE*** =") -> cookie = it.substringAfter("=")
                        it.startsWith("***VISITOR DATA*** =") -> visitorDataValue = it.substringAfter("=")
                        it.startsWith("***DATASYNC ID*** =") -> dataSyncIdValue = it.substringAfter("=")
                        it.startsWith("***AUTH USER*** =") -> authUserValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT NAME*** =") -> accountNameValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT EMAIL*** =") -> accountEmailValue = it.substringAfter("=")
                        it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> accountChannelHandleValue = it.substringAfter("=")
                    }
                }
                // Write all credentials atomically to DataStore and wait for completion
                // before restarting, preventing the race condition where the process
                // would be killed before async DataStore coroutines finished writing.
                accountSettingsViewModel.saveTokenAndRestart(
                    context = context,
                    cookie = cookie,
                    visitorData = visitorDataValue,
                    dataSyncId = dataSyncIdValue,
                    authUser = authUserValue,
                    accountName = accountNameValue,
                    accountEmail = accountEmailValue,
                    accountChannelHandle = accountChannelHandleValue,
                )
            },
            onDismiss = { showTokenEditor = false },
            singleLine = false,
            maxLines = 20,
            isInputValid = { fullText ->
                // Extract the cookie value from the formatted template line,
                // then validate it separately — avoids the bug where parseCookieString
                // received the entire multi-line template and failed to find "SAPISID"
                // as a key because the "***INNERTUBE COOKIE*** =" prefix shadowed it.
                val cookieLine = fullText.lines()
                    .find { it.startsWith("***INNERTUBE COOKIE*** =") }
                val cookieValue = cookieLine?.substringAfter("***INNERTUBE COOKIE*** =")?.trim() ?: ""
                cookieValue.isNotEmpty() && "SAPISID" in parseCookieString(cookieValue)
            },
            extraContent = {
                Spacer(Modifier.height(8.dp))
                InfoLabel(text = stringResource(R.string.token_adv_login_description))
            }
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header: avatar, name and email
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable(enabled = isLoggedIn) {
                    onClose()
                    navController.navigate("account")
                }
                .padding(8.dp),
        ) {
            AccountAvatar(
                imageUrl = if (isLoggedIn) accountImageUrl else null,
                name = if (isLoggedIn) accountName else "",
                size = 60.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) accountName else stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when {
                    !isLoggedIn -> null
                    accountEmail.isNotBlank() -> accountEmail
                    accountChannelHandle.isNotBlank() -> accountChannelHandle
                    else -> null
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (!isLoggedIn) {
            // One big, obvious way in
            Button(
                onClick = {
                    onClose()
                    navController.navigate("login")
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(painterResource(R.drawable.login), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.login), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
        }

        MenuGroup {
            if (isLoggedIn) {
                MenuRow(
                    icon = R.drawable.account,
                    title = stringResource(R.string.switch_youtube_channel),
                    showChevron = true,
                    onClick = {
                        onClose()
                        navController.navigate("switch_channel")
                    },
                )
                MenuDivider()
            }
            MenuRow(
                icon = R.drawable.integration,
                title = stringResource(R.string.integrations),
                showChevron = true,
                onClick = {
                    onClose()
                    navController.navigate("settings/integrations")
                },
            )
            MenuDivider()
            MenuRow(
                icon = R.drawable.settings,
                title = stringResource(R.string.settings),
                showChevron = true,
                showBadge = showUpdate,
                onClick = {
                    onClose()
                    navController.navigate("settings")
                },
            )
            if (showUpdate) {
                val releaseInfo = Updater.getCachedLatestRelease()
                val downloadUrl = releaseInfo?.let { Updater.getDownloadUrlForCurrentVariant(it) }
                if (downloadUrl != null) {
                    MenuDivider()
                    MenuRow(
                        icon = R.drawable.update,
                        title = stringResource(R.string.new_version_available),
                        subtitle = latestVersionName,
                        showBadge = true,
                        onClick = { uriHandler.openUri(downloadUrl) },
                    )
                }
            }
        }

        if (isLoggedIn) {
            Spacer(Modifier.height(12.dp))
            MenuGroup {
                MenuRow(
                    icon = R.drawable.cached,
                    title = stringResource(R.string.more_content),
                    onClick = {
                        YouTube.useLoginForBrowse = !useLoginForBrowse
                        onUseLoginForBrowseChange(!useLoginForBrowse)
                    },
                    trailing = {
                        Switch(
                            checked = useLoginForBrowse,
                            onCheckedChange = {
                                YouTube.useLoginForBrowse = it
                                onUseLoginForBrowseChange(it)
                            },
                        )
                    },
                )
                MenuDivider()
                MenuRow(
                    icon = R.drawable.sync,
                    title = stringResource(R.string.yt_sync),
                    onClick = { onYtmSyncChange(!ytmSync) },
                    trailing = {
                        Switch(checked = ytmSync, onCheckedChange = onYtmSyncChange)
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Rarely needed, so it sits quietly at the bottom
        MenuGroup {
            MenuRow(
                icon = R.drawable.token,
                title = when {
                    !isLoggedIn -> stringResource(R.string.advanced_login)
                    showToken -> stringResource(R.string.token_shown)
                    else -> stringResource(R.string.token_hidden)
                },
                compact = true,
                onClick = {
                    if (!isLoggedIn) showTokenEditor = true
                    else if (!showToken) showToken = true
                    else showTokenEditor = true
                },
            )
            if (isLoggedIn) {
                MenuDivider()
                MenuRow(
                    icon = R.drawable.logout,
                    title = stringResource(R.string.action_logout),
                    tint = MaterialTheme.colorScheme.error,
                    compact = true,
                    onClick = {
                        Timber.d("[LOGOUT] User clicked logout button, showing dialog")
                        showLogoutDialog = true
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountAvatar(imageUrl: String?, name: String, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (name.isNotBlank()) {
            Text(
                text = name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.account),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
private fun MenuGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        content = content,
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 56.dp),
    )
}

@Composable
private fun MenuRow(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    showChevron: Boolean = false,
    showBadge: Boolean = false,
    compact: Boolean = false,
    tint: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
) {
    val contentColor = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurface
    val iconColor = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 50.dp else 58.dp)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showBadge) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
            )
        }
        if (trailing != null) {
            trailing()
        } else if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
