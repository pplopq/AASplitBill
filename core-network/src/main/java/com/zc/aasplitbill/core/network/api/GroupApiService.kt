package com.zc.aasplitbill.core.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.zc.aasplitbill.core.network.model.BaseApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 群组相关接口。后端未就绪，DEBUG 下 Repository 会走 mock 兜底。
 */
interface GroupApiService {

    @GET("api/groups")
    suspend fun getGroups(): BaseApiResponse<List<GroupDto>>

    @POST("api/groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): BaseApiResponse<GroupDto>

    @GET("api/groups/{groupId}/members")
    suspend fun getMembers(
        @Path("groupId") groupId: Long,
    ): BaseApiResponse<List<GroupMemberDto>>

    @JsonClass(generateAdapter = true)
    data class CreateGroupRequest(
        val name: String,
        val scene: String,
        val currency: String,
    )

    @JsonClass(generateAdapter = true)
    data class GroupDto(
        @Json(name = "group_id") val groupId: Long,
        val name: String,
        val scene: String,
        val currency: String,
        @Json(name = "owner_id") val ownerId: Long,
        @Json(name = "owner_nickname") val ownerNickname: String,
        @Json(name = "member_count") val memberCount: Int,
        @Json(name = "created_at") val createdAt: Long,
        @Json(name = "updated_at") val updatedAt: Long,
    )

    @JsonClass(generateAdapter = true)
    data class GroupMemberDto(
        @Json(name = "user_id") val userId: Long,
        val nickname: String,
        @Json(name = "avatar_url") val avatarUrl: String? = null,
        val role: String,
        @Json(name = "joined_at") val joinedAt: Long,
    )
}
