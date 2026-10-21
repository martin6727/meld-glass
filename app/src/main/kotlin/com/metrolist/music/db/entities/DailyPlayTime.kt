package com.metrolist.music.db.entities

/** Listening time for one calendar day; [day] counts days since 1970-01-01. Query result, not a table. */
data class DailyPlayTime(
    val day: Long,
    val playTime: Long,
)
