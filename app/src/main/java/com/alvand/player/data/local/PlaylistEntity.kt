package com.alvand.player.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** پلی‌لیست کاربر — نام یکتا، زمان ساخت برای سورت */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
