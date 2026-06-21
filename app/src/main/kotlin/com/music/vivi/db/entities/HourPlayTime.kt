package com.music.vivi.db.entities

import androidx.room.ColumnInfo

data class HourPlayTime(
    @ColumnInfo(name = "hour") val hour: Int,
    @ColumnInfo(name = "totalMs") val totalMs: Long
)
