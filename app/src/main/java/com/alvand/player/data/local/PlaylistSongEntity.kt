package com.alvand.player.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * آهنگ داخل پلی‌لیست — songId همان Song.id است (MediaStore >=0، ریموت/فایل منفی).
 * uri/title/artist کش می‌شود تا اگر آهنگ از گوشی حذف شد، پلی‌لیست نشکند.
 */
@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val isRemote: Boolean = false,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
