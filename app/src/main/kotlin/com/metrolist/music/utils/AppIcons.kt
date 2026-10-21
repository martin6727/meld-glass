package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.metrolist.music.R

/** Home-screen icon designs. Each icon and name pair is its own launcher entry in the manifest. */
enum class AppIconStyle(val id: String, val mipmap: Int, val labelRes: Int) {
    GLASS("Glass", R.mipmap.ic_launcher, R.string.icon_glass),
    SPLIT("Split", R.mipmap.ic_icon_split, R.string.icon_split),
    MONO("Mono", R.mipmap.ic_icon_mono, R.string.icon_mono),
    CLASSIC("Classic", R.mipmap.ic_icon_classic, R.string.icon_classic),
}

/** Home-screen names. Android can only show names built into the app, so these are presets. */
enum class AppIconName(val id: String, val labelRes: Int) {
    MELD_GLASS("MeldGlass", R.string.app_name),
    MUSIC("Music", R.string.app_label_music),
    MELD("Meld", R.string.app_label_meld),
    PLAYER("Player", R.string.app_label_player),
}

object AppIcons {
    // Launcher entries are declared under the code namespace, not the application id
    private const val NAMESPACE = "com.metrolist.music"
    private const val DEFAULT_ALIAS = "$NAMESPACE.MainActivityAlias"
    private const val LEGACY_STATIC_ALIAS = "$NAMESPACE.MainActivityStatic"

    private fun aliasFor(style: AppIconStyle, name: AppIconName): String =
        if (style == AppIconStyle.GLASS && name == AppIconName.MELD_GLASS) {
            DEFAULT_ALIAS
        } else {
            "$NAMESPACE.Icon${style.id}${name.id}"
        }

    private fun allAliases(): List<String> =
        listOf(DEFAULT_ALIAS, LEGACY_STATIC_ALIAS) +
            AppIconStyle.entries.flatMap { style -> AppIconName.entries.map { aliasFor(style, it) } }.distinct()

    /** Shows the app on the home screen with [style] and [name]. */
    fun apply(context: Context, style: AppIconStyle, name: AppIconName) {
        val pm = context.packageManager
        val chosen = aliasFor(style, name)
        // Switch the new entry on before the old ones go off, so the app never vanishes
        pm.setComponentEnabledSetting(
            ComponentName(context, chosen),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        allAliases().filter { it != chosen }.forEach { alias ->
            pm.setComponentEnabledSetting(
                ComponentName(context, alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
