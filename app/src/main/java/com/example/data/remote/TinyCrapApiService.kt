package com.example.data.remote

import com.example.data.model.DownloadSubmitRequest
import com.example.data.model.DownloadSubmitResponse
import com.example.data.model.HealthResponse
import com.example.data.model.JobStatusResponse
import com.example.data.model.MediaInfoRequest
import com.example.data.model.MediaInfoResponse
import com.example.data.model.UpdateNoticeResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface TinyCrapApiService {

    @GET("api/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @POST("api/info")
    suspend fun getMediaInfo(
        @Body request: MediaInfoRequest,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<MediaInfoResponse>

    @POST("api/download")
    suspend fun submitDownload(
        @Body request: DownloadSubmitRequest,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<DownloadSubmitResponse>

    @GET("api/status/{job_id}")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<JobStatusResponse>

    @Streaming
    @GET("api/file/{job_id}")
    suspend fun downloadFile(
        @Path("job_id") jobId: String,
        @Header("X-API-Key") apiKey: String? = null
    ): Response<ResponseBody>

    @GET("api/check_update")
    suspend fun checkUpdate(): Response<UpdateNoticeResponse>
}
