package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'completed' ORDER BY createdAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('queued', 'downloading') ORDER BY createdAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    fun getDownloadFlowById(id: String): Flow<DownloadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, speed = :speed, queuePosition = :queuePosition, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, speed: String, queuePosition: Int, status: String)

    @Query("UPDATE downloads SET status = :status, localFilePath = :path, localUri = :uri, fileSize = :fileSize, progress = 100.0 WHERE id = :id")
    suspend fun markCompleted(id: String, path: String, uri: String?, fileSize: Long, status: String = "completed")

    @Query("UPDATE downloads SET status = 'failed', errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
