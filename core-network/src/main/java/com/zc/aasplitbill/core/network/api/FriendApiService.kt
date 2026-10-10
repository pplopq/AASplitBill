package com.zc.aasplitbill.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.zc.aasplitbill.core.network.model.BaseApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 好友相关 API。
 */
interface FriendApiService {

    @JsonClass(generateAdapter = true)
    data class FriendDto(
        @Json(name = "user_id") val userId: Long,
        @Json(name = "nickname") val nickname: String,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        @Json(name = "phone") val phone: String,
        @Json(name = "last_active_at") val lastActiveAt: Long = 0L,
    )

    @JsonClass(generateAdapter = true)
    data class SearchByPhoneRequest(
        @Json(name = "phone") val phone: String,
    )

    @JsonClass(generateAdapter = true)
    data class AddFriendRequest(
        @Json(name = "friend_user_id") val friendUserId: Long,
    )

    @GET("api/friends")
    suspend fun getFriends(): BaseApiResponse<List<FriendDto>>

    @POST("api/friends/search")
    suspend fun searchByPhone(@Body request: SearchByPhoneRequest): BaseApiResponse<FriendDto>

    @POST("api/friends/add")
    suspend fun addFriend(@Body request: AddFriendRequest): BaseApiResponse<FriendDto>
}
