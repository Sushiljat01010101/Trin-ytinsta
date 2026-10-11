package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaInfoRequest(
    val url: String
)

@JsonClass(generateAdapter = true)
data class VideoQuality(
    val format_id: String,
    val resolution: String,
    val height: Int? = null,
    val ext: String = "mp4",
    val filesize: Long = 0,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class AudioFormat(
    val format_id: String,
    val ext: String,
    val bitrate: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class MediaInfoResponse(
    val title: String = "",
    val uploader: String = "",
    val duration: Long? = null,
    val thumbnail: String? = null,
    val platform: String = "Web",
    val url: String = "",
    val video_qualities: List<VideoQuality> = emptyList(),
    val audio_formats: List<AudioFormat> = emptyList(),
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class DownloadSubmitRequest(
    val url: String,
    val title: String = "",
    val type: String = "video",
    val format_id: String = "best",
    val quality: String = "Best",
    val ext: String = "mp4",
    val bitrate: String = "320"
)

@JsonClass(generateAdapter = true)
data class DownloadSubmitResponse(
    val job_id: String,
    val status: String,
    val queue_position: Int = 0,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class JobStatusResponse(
    val job_id: String,
    val status: String,
    val progress: Float = 0f,
    val speed: String = "",
    val eta: String = "",
    val queue_position: Int = 0,
    val filename: String? = null,
    val file_size: Long = 0,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateNoticeResponse(
    val version_code: Int = 1,
    val version_name: String = "1.0.0",
    val apk_url: String = "",
    val changelog: String = "",
    val force_update: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String,
    val service: String? = null
)
