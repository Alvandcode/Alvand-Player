package com.alvand.player.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // ---- Playlists ----
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getPlaylists(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaylist(e: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String)

    // ---- Songs in playlist ----
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :pid ORDER BY position ASC, addedAt ASC")
    fun observePlaylistSongs(pid: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :pid AND songId = :sid")
    suspend fun countInPlaylist(pid: Long, sid: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(e: PlaylistSongEntity): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :pid AND songId = :sid")
    suspend fun removeFromPlaylist(pid: Long, sid: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :pid")
    suspend fun nextPosition(pid: Long): Int

    // ---- History ----
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE songId = :sid LIMIT 1")
    suspend fun historyFor(sid: Long): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(e: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE playedAt < :before")
    suspend fun pruneHistory(before: Long)

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()
}
