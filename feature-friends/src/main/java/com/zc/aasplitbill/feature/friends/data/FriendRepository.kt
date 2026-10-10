package com.zc.aasplitbill.feature.friends.data

import android.util.Log
import com.zc.aasplitbill.core.database.dao.FriendDao
import com.zc.aasplitbill.core.database.entity.FriendEntity
import com.zc.aasplitbill.core.network.api.FriendApiService
import com.zc.aasplitbill.core.network.model.BaseApiResponse
import com.zc.aasplitbill.feature.friends.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 与 Auth 层保持一致的 Result 语义（各 feature 复用同一套，后续可上移到 core-common） */
sealed interface FriendResult<out T> {
    data class Success<T>(val data: T) : FriendResult<T>
    data class Error(val code: Int, val message: String) : FriendResult<Nothing>
    data class NetworkError(val cause: Throwable) : FriendResult<Nothing>
}

@Singleton
class FriendRepository @Inject constructor(
    private val api: FriendApiService,
    private val friendDao: FriendDao,
) {

    /**
     * DEBUG 兜底：后端未就绪时造一份假数据/假搜索结果，保证演示可跑。
     * 后端就绪后把此变量置 false。
     */
    private val mockOnNetworkError = true

    // ---- 列表 ----

    /** 本地好友列表（Flow，Room 自动驱动刷新） */
    fun observeFriends() = friendDao.observeAll()

    /** 搜索框实时过滤（Room 版，只搜本地已添加的好友） */
    fun searchLocalFriends(keyword: String) =
        friendDao.search("%$keyword%")

    // ---- 网络操作 ----

    /** 拉取好友列表并落库（刷新），失败返回本地缓存 */
    suspend fun refreshFriends(): FriendResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val resp = api.getFriends()
            if (resp.isSuccess) {
                friendDao.upsertAll(resp.data.orEmpty().map { it.toEntity() })
                FriendResult.Success(Unit)
            } else {
                FriendResult.Error(resp.code, resp.message)
            }
        } catch (e: IOException) {
            if (mockOnNetworkError && BuildConfig.DEBUG) {
                Log.w(TAG, "网络不可达，写入 mock 好友数据")
                seedMockFriends()
                FriendResult.Success(Unit)
            } else {
                FriendResult.NetworkError(e)
            }
        }
    }

    /** 按手机号搜索用户（添加好友的第一步） */
    suspend fun searchByPhone(phone: String): FriendResult<FriendEntity> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.searchByPhone(FriendApiService.SearchByPhoneRequest(phone))
                if (resp.isSuccess && resp.data != null) {
                    FriendResult.Success(resp.data!!.toEntity())
                } else if (resp.data == null) {
                    FriendResult.Error(resp.code, "未找到该手机号对应的用户")
                } else {
                    FriendResult.Error(resp.code, resp.message)
                }
            } catch (e: IOException) {
                if (mockOnNetworkError && BuildConfig.DEBUG) {
                    Log.w(TAG, "网络不可达，返回 mock 搜索结果")
                    // mock：138 开头返回一个假用户，否则提示未找到
                    if (phone.startsWith("138")) {
                        FriendResult.Success(mockUser(phone))
                    } else {
                        FriendResult.Error(404, "未找到该手机号对应的用户")
                    }
                } else {
                    FriendResult.NetworkError(e)
                }
            }
        }

    /** 添加好友：成功写 Room，列表自动刷新 */
    suspend fun addFriend(friend: FriendEntity): FriendResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val resp = api.addFriend(
                    FriendApiService.AddFriendRequest(friend.friendUserId)
                )
                if (resp.isSuccess) {
                    friendDao.upsert(friend)
                    FriendResult.Success(Unit)
                } else {
                    FriendResult.Error(resp.code, resp.message)
                }
            } catch (e: IOException) {
                if (mockOnNetworkError && BuildConfig.DEBUG) {
                    Log.w(TAG, "网络不可达，直接写入本地好友表")
                    friendDao.upsert(friend)
                    FriendResult.Success(Unit)
                } else {
                    FriendResult.NetworkError(e)
                }
            }
        }

    // ---- mock ----

    private suspend fun seedMockFriends() {
        val now = System.currentTimeMillis()
        friendDao.upsertAll(
            listOf(
                FriendEntity(
                    friendUserId = 9001, nickname = "阿明", phone = "13800000001",
                    lastActiveAt = now - 3_600_000, createdAt = now, updatedAt = now,
                ),
                FriendEntity(
                    friendUserId = 9002, nickname = "小华", phone = "13800000002",
                    lastActiveAt = now - 86_400_000, createdAt = now, updatedAt = now,
                ),
                FriendEntity(
                    friendUserId = 9003, nickname = "旅游小分队-群聊", phone = "13800000003",
                    lastActiveAt = now - 604_800_000, createdAt = now, updatedAt = now,
                ),
            )
        )
    }

    private fun mockUser(phone: String) = FriendEntity(
        friendUserId = phone.hashCode().toLong(),
        nickname = "用户 $phone",
        phone = phone,
    )

    private fun FriendApiService.FriendDto.toEntity() = FriendEntity(
        friendUserId = userId,
        nickname = nickname,
        avatarUrl = avatarUrl,
        phone = phone,
        lastActiveAt = lastActiveAt,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private companion object {
        const val TAG = "FriendRepository"
    }
}
