package com.alvand.player.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** تاریخچه پخش — برای Recently Played و Most Played */
@Entity(tableName = "play_history", indices = [Index("songId"), Index("playedAt")])
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val title: String,
    val artist: String,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)
