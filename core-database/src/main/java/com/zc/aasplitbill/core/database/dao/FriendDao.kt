package com.zc.aasplitbill.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zc.aasplitbill.core.database.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(friend: FriendEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(friends: List<FriendEntity>)

    @Query("SELECT * FROM friends ORDER BY last_active_at DESC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends ORDER BY last_active_at DESC")
    suspend fun getAll(): List<FriendEntity>

    /** 搜索框实时过滤：昵称或手机号模糊匹配（SQL LIKE，% 在调用方拼） */
    @Query("SELECT * FROM friends WHERE nickname LIKE :keyword OR phone LIKE :keyword ORDER BY last_active_at DESC")
    fun search(keyword: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE friend_user_id = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): FriendEntity?

    @Query("DELETE FROM friends WHERE friend_user_id = :userId")
    suspend fun deleteByUserId(userId: Long)
}
