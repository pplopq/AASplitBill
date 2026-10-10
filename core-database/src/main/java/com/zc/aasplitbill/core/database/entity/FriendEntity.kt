package com.zc.aasplitbill.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 好友表。存"我添加的好友"的本地快照，离线也能看列表。
 *
 * 为什么不直接查 users 表：users 是"所有注册用户"，friends 是"和我有关系的人"，
 * 还挂着关系状态（已添加/待确认）、备注名等关系侧字段，混在一张表后期必然拆。
 */
@Entity(
    tableName = "friends",
    indices = [Index(value = ["friend_user_id"], unique = true)],
)
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** 对方在用户表里的业务 id */
    @ColumnInfo(name = "friend_user_id")
    val friendUserId: Long,

    @ColumnInfo(name = "nickname")
    val nickname: String,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "phone")
    val phone: String,

    /** 最后一次互动时间戳（列表按它倒序排），毫秒 */
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long = 0L,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
)
