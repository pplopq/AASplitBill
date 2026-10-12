package com.zc.aasplitbill.feature.group.data

import android.util.Log
import com.zc.aasplitbill.core.database.dao.GroupDao
import com.zc.aasplitbill.core.database.entity.GroupCurrency
import com.zc.aasplitbill.core.database.entity.GroupEntity
import com.zc.aasplitbill.core.database.entity.GroupMemberEntity
import com.zc.aasplitbill.core.database.entity.GroupScene
import com.zc.aasplitbill.core.network.api.GroupApiService
import com.zc.aasplitbill.feature.group.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 与 Auth/Friend 层保持同一套 Result 语义 */
sealed interface GroupResult<out T> {
    data class Success<T>(val data: T) : GroupResult<T>
    data class Error(val code: Int, val message: String) : GroupResult<Nothing>
    data class NetworkError(val cause: Throwable) : GroupResult<Nothing>
}

@Singleton
class GroupRepository @Inject constructor(
    private val api: GroupApiService,
    private val groupDao: GroupDao,
) {

    /** DEBUG 兜底：后端未就绪时造 mock 数据，保证演示可跑 */
    private val mockOnNetworkError = true

    fun observeGroups() = groupDao.observeGroups()

    fun observeGroupsWithMembers() = groupDao.observeGroupsWithMembers()

    fun observeGroup(groupId: Long) = groupDao.observeGroup(groupId)

    fun observeMembers(groupId: Long) = groupDao.observeMembers(groupId)

    /** 拉取群组列表，失败返回本地缓存（mock 环境直接落种子数据） */
    suspend fun refreshGroups(): GroupResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getGroups()
            if (resp.isSuccess) {
                groupDao.upsertGroups(resp.data.orEmpty().map { it.toEntity() })
                GroupResult.Success(Unit)
            } else {
                GroupResult.Error(resp.code, resp.message)
            }
        } catch (e: Exception) {
            if (mockOnNetworkError && BuildConfig.DEBUG) {
                Log.w(TAG, "网络不可达，写入 mock 群组数据", e)
                seedMockGroups()
                GroupResult.Success(Unit)
            } else if (e is IOException) {
                GroupResult.NetworkError(e)
            } else {
                throw e
            }
        }
    }

    /** 创建群组：本地生成负数 ID（mock 段），后端就绪后由服务端返回真实 ID */
    suspend fun createGroup(
        name: String,
        scene: GroupScene,
        currency: GroupCurrency,
    ): GroupResult<GroupEntity> = withContext(Dispatchers.IO) {
        try {
            val resp = api.createGroup(
                GroupApiService.CreateGroupRequest(name, scene.name, currency.name)
            )
            if (resp.isSuccess && resp.data != null) {
                val entity = resp.data!!.toEntity()
                persistCreatedGroup(entity)
                GroupResult.Success(entity)
            } else {
                GroupResult.Error(resp.code, resp.message)
            }
        } catch (e: Exception) {
            if (mockOnNetworkError && BuildConfig.DEBUG) {
                Log.w(TAG, "网络不可达，本地创建 mock 群组", e)
                val now = System.currentTimeMillis()
                val entity = GroupEntity(
                    groupId = -(now % 1_000_000_000L),
                    name = name,
                    scene = scene.name,
                    currency = currency.name,
                    ownerId = 8888,
                    ownerNickname = "我",
                    memberCount = 1,
                    createdAt = now,
                    updatedAt = now,
                )
                persistCreatedGroup(entity)
                GroupResult.Success(entity)
            } else if (e is IOException) {
                GroupResult.NetworkError(e)
            } else {
                throw e
            }
        }
    }

    /**
     * 邀请链接：真实场景应由后端签发带签名的一次性 code。
     * 后端未就绪时在本地拼一条可分享的 join URL。
     */
    suspend fun generateInviteLink(groupId: Long): String = withContext(Dispatchers.IO) {
        val code = (groupId.toString() + System.currentTimeMillis().toString())
            .takeLast(8)
            .map { ('A'..'Z').elementAt((it.digitToIntOrNull() ?: 0) % 26) }
            .joinToString("")
        "https://aasplitbill.app/join/$groupId?code=$code"
    }

    private suspend fun persistCreatedGroup(entity: GroupEntity) {
        groupDao.upsertGroup(entity)
        groupDao.upsertMember(
            GroupMemberEntity(
                groupId = entity.groupId,
                userId = entity.ownerId,
                nickname = entity.ownerNickname,
                role = "OWNER",
                joinedAt = System.currentTimeMillis(),
            )
        )
    }

    private suspend fun seedMockGroups() {
        val now = System.currentTimeMillis()

        val travel = GroupEntity(
            groupId = -9001,
            name = "周末川西自驾",
            scene = GroupScene.TRAVEL.name,
            currency = GroupCurrency.CNY.name,
            ownerId = 8888,
            ownerNickname = "我",
            memberCount = 4,
            createdAt = now - 7L * 86_400_000,
            updatedAt = now - 86_400_000,
        )
        val rental = GroupEntity(
            groupId = -9002,
            name = "阳光公寓 3 号合租",
            scene = GroupScene.RENTAL.name,
            currency = GroupCurrency.CNY.name,
            ownerId = 8888,
            ownerNickname = "我",
            memberCount = 3,
            createdAt = now - 30L * 86_400_000,
            updatedAt = now - 3L * 86_400_000,
        )
        groupDao.upsertGroups(listOf(travel, rental))

        groupDao.upsertMembers(
            listOf(
                GroupMemberEntity(groupId = -9001, userId = 8888, nickname = "我", role = "OWNER", joinedAt = now - 7L * 86_400_000),
                GroupMemberEntity(groupId = -9001, userId = 9001, nickname = "阿明", role = "MEMBER", joinedAt = now - 6L * 86_400_000),
                GroupMemberEntity(groupId = -9001, userId = 9002, nickname = "小华", role = "MEMBER", joinedAt = now - 6L * 86_400_000),
                GroupMemberEntity(groupId = -9001, userId = 9003, nickname = "大刘", role = "MEMBER", joinedAt = now - 5L * 86_400_000),
                GroupMemberEntity(groupId = -9002, userId = 8888, nickname = "我", role = "OWNER", joinedAt = now - 30L * 86_400_000),
                GroupMemberEntity(groupId = -9002, userId = 9001, nickname = "阿明", role = "MEMBER", joinedAt = now - 29L * 86_400_000),
                GroupMemberEntity(groupId = -9002, userId = 9004, nickname = "房东阿姨", role = "MEMBER", joinedAt = now - 29L * 86_400_000),
            )
        )
    }

    private fun GroupApiService.GroupDto.toEntity() = GroupEntity(
        groupId = groupId,
        name = name,
        scene = scene,
        currency = currency,
        ownerId = ownerId,
        ownerNickname = ownerNickname,
        memberCount = memberCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private companion object {
        const val TAG = "GroupRepository"
    }
}
