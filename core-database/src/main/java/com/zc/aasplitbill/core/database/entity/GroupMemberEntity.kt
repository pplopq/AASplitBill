package com.zc.aasplitbill.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 群组成员表：一个群组多条记录，按 group_id 关联（暂不建外键，删群时手动清理成员） */
@Entity(
    tableName = "group_members",
    indices = [Index(value = ["group_id", "user_id"], unique = true)],
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "nickname")
    val nickname: String,
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    /** OWNER / MEMBER */
    @ColumnInfo(name = "role")
    val role: String,
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long,
)
