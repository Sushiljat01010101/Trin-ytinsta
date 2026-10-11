package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String, // Job ID or unique UUID
    val title: String,
    val sourceUrl: String,
    val uploader: String = "",
    val durationText: String = "",
    val thumbnail: String? = null,
    val platform: String = "Web",
    val mediaType: String = "video", // "video" or "audio"
    val qualityOrFormat: String = "",
    val ext: String = "mp4",
    val localFilePath: String? = null,
    val localUri: String? = null,
    val mimeType: String = "video/mp4",
    val fileSize: Long = 0L,
    val status: String = "queued", // "queued", "downloading", "completed", "failed", "cancelled"
    val progress: Float = 0f,
    val queuePosition: Int = 0,
    val speed: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
