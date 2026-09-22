package com.alvand.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, PlayHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlvandDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
