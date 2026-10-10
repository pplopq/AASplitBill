package com.zc.aasplitbill.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.zc.aasplitbill.core.network.model.BaseApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 认证相关 API。所有方法 suspend，Retrofit 自动切 IO 线程。
 *
 * ⚠️ baseUrl 是 "http://10.2:8080/"，这里的路径已是完整相对路径（含 /api 前缀）。
 */
interface AuthApiService {

    // ---- DTO ----

    @JsonClass(generateAdapter = true)
    data class SendCodeRequest(
        @Json(name = "phone") val phone: String,
    )

    @JsonClass(generateAdapter = true)
    data class LoginRequest(
        @Json(name = "phone") val phone: String,
        @Json(name = "code") val code: String,
    )

    @JsonClass(generateAdapter = true)
    data class LoginResponse(
        @Json(name = "token") val token: String,
        @Json(name = "refresh_token") val refreshToken: String? = null,
        @Json(name = "user_id") val userId: Long,
    )

    @JsonClass(generateAdapter = true)
    data class RefreshRequest(
        @Json(name = "refresh_token") val refreshToken: String,
    )

    @JsonClass(generateAdapter = true)
    data class UserInfoResponse(
        @Json(name = "user_id") val userId: Long,
        @Json(name = "phone") val phone: String,
        @Json(name = "nickname") val nickname: String? = null,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
    )

    // ---- API ----

    @POST("api/auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): BaseApiResponse<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): BaseApiResponse<LoginResponse>

    @GET("api/auth/user-info")
    suspend fun getUserInfo(): BaseApiResponse<UserInfoResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): BaseApiResponse<LoginResponse>
}
