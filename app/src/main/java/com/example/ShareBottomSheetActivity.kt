package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AudioFormat
import com.example.data.model.MediaInfoResponse
import com.example.data.model.VideoQuality
import com.example.service.DownloadForegroundService
import com.example.ui.theme.TinyCrapTheme
import kotlinx.coroutines.launch

class ShareBottomSheetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val urlRegex = Regex("""https?://[^\s]+""")
        val match = urlRegex.find(sharedText)
        val targetUrl = match?.value ?: sharedText.trim()

        if (targetUrl.isBlank()) {
            Toast.makeText(this, "No valid media URL found in share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            TinyCrapTheme {
                ShareSheetContent(
                    url = targetUrl,
                    onDismiss = { finish() },
                    onStartDownload = { info, isVideo, quality, audio ->
                        val repo = (application as TinyCrapApp).repository
                        val scope = (application as TinyCrapApp)
                        // Trigger download
                        Thread {
                            kotlinx.coroutines.runBlocking {
                                val qualityOrFormat = if (isVideo) quality?.resolution ?: "1080p" else audio?.label ?: "MP3"
                                val ext = if (isVideo) quality?.ext ?: "mp4" else audio?.ext ?: "mp3"
                                val formatId = if (isVideo) quality?.format_id ?: "best" else audio?.format_id ?: "bestaudio"
                                val bitrate = if (isVideo) "0" else audio?.bitrate ?: "320"

                                val jobId = repo.startDownload(
                                    url = targetUrl,
                                    title = info.title,
                                    thumbnail = info.thumbnail,
                                    platform = info.platform,
                                    mediaType = if (isVideo) "video" else "audio",
                                    qualityOrFormat = qualityOrFormat,
                                    ext = ext,
                                    formatId = formatId,
                                    bitrate = bitrate
                                )
                                DownloadForegroundService.start(this@ShareBottomSheetActivity, jobId)
                            }
                        }.start()

                        Toast.makeText(this, "Downloading in background via Tiny Crap", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShareSheetContent(
    url: String,
    onDismiss: () -> Unit,
    onStartDownload: (MediaInfoResponse, Boolean, VideoQuality?, AudioFormat?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var mediaInfo by remember { mutableStateOf<MediaInfoResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isVideoSelected by remember { mutableStateOf(true) }
    var selectedVideoQuality by remember { mutableStateOf<VideoQuality?>(null) }
    var selectedAudioFormat by remember { mutableStateOf<AudioFormat?>(null) }

    LaunchedEffect(url) {
        val repo = TinyCrapApp.instance.repository
        val result = repo.inspectUrl(url)
        result.onSuccess { info ->
            mediaInfo = info
            selectedVideoQuality = info.video_qualities.firstOrNull()
            selectedAudioFormat = info.audio_formats.firstOrNull()
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.message
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("share_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tiny Crap Downloader",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analyzing link formats...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = url,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (mediaInfo != null) {
                val info = mediaInfo!!

                // Media Preview
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!info.thumbnail.isNullOrBlank()) {
                            AsyncImage(
                                model = info.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${info.platform} · ${info.uploader}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs: Video / Audio
                TabRow(
                    selectedTabIndex = if (isVideoSelected) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .height(40.dp)
                ) {
                    Tab(
                        selected = isVideoSelected,
                        onClick = { isVideoSelected = true },
                        text = { Text("Video (MP4)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = !isVideoSelected,
                        onClick = { isVideoSelected = false },
                        text = { Text("Audio (MP3)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Formats Chips
                if (isVideoSelected) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        info.video_qualities.forEach { q ->
                            val isSelected = selectedVideoQuality == q
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedVideoQuality = q },
                                label = { Text("${q.resolution} (${q.ext.uppercase()})", fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        info.audio_formats.forEach { a ->
                            val isSelected = selectedAudioFormat == a
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAudioFormat = a },
                                label = { Text(a.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onStartDownload(info, isVideoSelected, selectedVideoQuality, selectedAudioFormat)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_download_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideoSelected) {
                            "Download ${selectedVideoQuality?.resolution ?: "Video"}"
                        } else {
                            "Download ${selectedAudioFormat?.ext?.uppercase() ?: "Audio"}"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else {
                Text(
                    text = errorMessage ?: "Could not process media link",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        }
    }
}
