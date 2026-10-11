package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TinyCrapApp
import com.example.data.local.DownloadEntity
import com.example.data.model.AudioFormat
import com.example.data.model.MediaInfoResponse
import com.example.data.model.UpdateNoticeResponse
import com.example.data.model.VideoQuality
import com.example.service.DownloadForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class InspectState {
    object Idle : InspectState()
    object Loading : InspectState()
    data class Success(val mediaInfo: MediaInfoResponse) : InspectState()
    data class Error(val message: String) : InspectState()
}

enum class NavTab {
    HOME, DOWNLOADED, SETTINGS
}

enum class MediaTypeSelection {
    VIDEO, AUDIO
}

enum class DownloadFilter {
    ALL, VIDEOS, AUDIOS, ACTIVE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as TinyCrapApp).repository
    private val prefs = (application as TinyCrapApp).preferences

    // Navigation
    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    // Home Tab Link & Inspection
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _inspectState = MutableStateFlow<InspectState>(InspectState.Idle)
    val inspectState: StateFlow<InspectState> = _inspectState.asStateFlow()

    private val _selectedMediaType = MutableStateFlow(MediaTypeSelection.VIDEO)
    val selectedMediaType: StateFlow<MediaTypeSelection> = _selectedMediaType.asStateFlow()

    private val _selectedVideoQuality = MutableStateFlow<VideoQuality?>(null)
    val selectedVideoQuality: StateFlow<VideoQuality?> = _selectedVideoQuality.asStateFlow()

    private val _selectedAudioFormat = MutableStateFlow<AudioFormat?>(null)
    val selectedAudioFormat: StateFlow<AudioFormat?> = _selectedAudioFormat.asStateFlow()

    // Downloads
    val allDownloads = repo.allDownloads.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val activeDownloads = repo.activeDownloads.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val completedDownloads = repo.completedDownloads.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Downloaded tab filters & search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()

    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        allDownloads,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.uploader.contains(query, ignoreCase = true) ||
                item.platform.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.VIDEOS -> item.mediaType == "video"
                DownloadFilter.AUDIOS -> item.mediaType == "audio"
                DownloadFilter.ACTIVE -> item.status in listOf("queued", "downloading")
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings
    val serverUrl = prefs.serverUrlFlow.stateIn(viewModelScope, SharingStarted.Lazily, "http://10.0.2.2:5000")
    val apiKey = prefs.apiKeyFlow.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val themeMode = prefs.themeModeFlow.stateIn(viewModelScope, SharingStarted.Lazily, "SYSTEM")
    val dynamicColor = prefs.dynamicColorFlow.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val splashAnimation = prefs.splashAnimationFlow.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val wifiOnly = prefs.wifiOnlyFlow.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val maxDownloads = prefs.maxDownloadsFlow.stateIn(viewModelScope, SharingStarted.Lazily, 2)
    val notificationsEnabled = prefs.notificationsEnabledFlow.stateIn(viewModelScope, SharingStarted.Lazily, true)
    val defaultVideoQuality = prefs.defaultVideoQualityFlow.stateIn(viewModelScope, SharingStarted.Lazily, "1080p")
    val defaultAudioFormat = prefs.defaultAudioFormatFlow.stateIn(viewModelScope, SharingStarted.Lazily, "MP3 (320 kbps)")
    val saveLocation = prefs.saveLocationFlow.stateIn(viewModelScope, SharingStarted.Lazily, "MediaStore")

    // Update Notice & Wall
    private val _updateNotice = MutableStateFlow<UpdateNoticeResponse?>(null)
    val updateNotice: StateFlow<UpdateNoticeResponse?> = _updateNotice.asStateFlow()

    // Server test status message
    private val _testConnectionStatus = MutableStateFlow<String?>(null)
    val testConnectionStatus: StateFlow<String?> = _testConnectionStatus.asStateFlow()

    init {
        checkForUpdate()
    }

    fun setTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setUrlInput(url: String) {
        _urlInput.value = url
    }

    fun inspectUrl(targetUrl: String? = null) {
        val url = (targetUrl ?: _urlInput.value).trim()
        if (url.isBlank()) return

        _urlInput.value = url
        _inspectState.value = InspectState.Loading

        viewModelScope.launch {
            val result = repo.inspectUrl(url)
            result.onSuccess { info ->
                _inspectState.value = InspectState.Success(info)
                // Pick default formats
                _selectedVideoQuality.value = info.video_qualities.firstOrNull()
                _selectedAudioFormat.value = info.audio_formats.firstOrNull()
            }.onFailure { error ->
                _inspectState.value = InspectState.Error(error.message ?: "Failed to inspect link")
            }
        }
    }

    fun selectMediaType(type: MediaTypeSelection) {
        _selectedMediaType.value = type
    }

    fun selectVideoQuality(quality: VideoQuality) {
        _selectedVideoQuality.value = quality
    }

    fun selectAudioFormat(format: AudioFormat) {
        _selectedAudioFormat.value = format
    }

    fun startDownload(targetUrl: String? = null, customInfo: MediaInfoResponse? = null) {
        val info = customInfo ?: (_inspectState.value as? InspectState.Success)?.mediaInfo ?: return
        val url = targetUrl ?: info.url

        val isVideo = _selectedMediaType.value == MediaTypeSelection.VIDEO
        val mediaType = if (isVideo) "video" else "audio"
        val qualityOrFormat: String
        val ext: String
        val formatId: String
        val bitrate: String

        if (isVideo) {
            val q = _selectedVideoQuality.value ?: info.video_qualities.firstOrNull()
            qualityOrFormat = q?.resolution ?: "1080p"
            ext = q?.ext ?: "mp4"
            formatId = q?.format_id ?: "best"
            bitrate = "0"
        } else {
            val a = _selectedAudioFormat.value ?: info.audio_formats.firstOrNull()
            qualityOrFormat = a?.label ?: "MP3 320k"
            ext = a?.ext ?: "mp3"
            formatId = a?.format_id ?: "bestaudio"
            bitrate = a?.bitrate ?: "320"
        }

        viewModelScope.launch {
            val jobId = repo.startDownload(
                url = url,
                title = info.title,
                thumbnail = info.thumbnail,
                platform = info.platform,
                mediaType = mediaType,
                qualityOrFormat = qualityOrFormat,
                ext = ext,
                formatId = formatId,
                bitrate = bitrate
            )

            // Start foreground service
            DownloadForegroundService.start(getApplication(), jobId)

            // Switch to Downloaded tab or reset URL input
            _urlInput.value = ""
            _inspectState.value = InspectState.Idle
        }
    }

    fun cancelDownload(jobId: String) {
        DownloadForegroundService.cancel(getApplication(), jobId)
    }

    fun deleteDownload(entity: DownloadEntity) {
        viewModelScope.launch {
            repo.deleteDownload(entity)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
    }

    // Settings modifiers
    fun updateServerUrl(url: String) = viewModelScope.launch { prefs.setServerUrl(url) }
    fun updateApiKey(key: String) = viewModelScope.launch { prefs.setApiKey(key) }
    fun updateThemeMode(mode: String) = viewModelScope.launch { prefs.setThemeMode(mode) }
    fun updateDynamicColor(enabled: Boolean) = viewModelScope.launch { prefs.setDynamicColor(enabled) }
    fun updateSplashAnimation(enabled: Boolean) = viewModelScope.launch { prefs.setSplashAnimation(enabled) }
    fun updateWifiOnly(enabled: Boolean) = viewModelScope.launch { prefs.setWifiOnly(enabled) }
    fun updateMaxDownloads(max: Int) = viewModelScope.launch { prefs.setMaxDownloads(max) }
    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setNotificationsEnabled(enabled) }
    fun updateDefaultVideoQuality(q: String) = viewModelScope.launch { prefs.setDefaultVideoQuality(q) }
    fun updateDefaultAudioFormat(f: String) = viewModelScope.launch { prefs.setDefaultAudioFormat(f) }

    fun testServerConnection() {
        _testConnectionStatus.value = "Testing server..."
        viewModelScope.launch {
            val result = repo.testConnection()
            result.onSuccess {
                _testConnectionStatus.value = "🟢 $it"
            }.onFailure {
                _testConnectionStatus.value = "🔴 Connection failed: ${it.message}"
            }
        }
    }

    fun clearTestStatus() {
        _testConnectionStatus.value = null
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            val result = repo.checkAppUpdate()
            result.onSuccess { notice ->
                // Version code in app is 1. If notice has version_code > 1 or force_update is true
                if (notice.force_update || notice.version_code > 1) {
                    _updateNotice.value = notice
                }
            }
        }
    }
}
