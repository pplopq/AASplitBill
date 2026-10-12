package com.zc.aasplitbill.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 群组 + 成员一次查出，给列表页画头像缩略图用。
 *
 * parentColumn / entityColumn 都走业务 group_id，而不是 Room 自增主键，
 * 这样 mock 负数 ID 和线上正数 ID 都能对上。
 */
data class GroupWithMembers(
    @Embedded val group: GroupEntity,
    @Relation(
        parentColumn = "group_id",
        entityColumn = "group_id",
    )
    val members: List<GroupMemberEntity>,
)
