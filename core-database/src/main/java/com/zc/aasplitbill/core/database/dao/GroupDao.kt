package com.zc.aasplitbill.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zc.aasplitbill.core.database.entity.GroupEntity
import com.zc.aasplitbill.core.database.entity.GroupMemberEntity
import com.zc.aasplitbill.core.database.entity.GroupWithMembers
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    // ---- 群组 ----

    /** 群组列表：Room Flow，数据一变 UI 自动刷新 */
    @Query("SELECT * FROM `groups` ORDER BY updated_at DESC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Transaction
    @Query("SELECT * FROM `groups` ORDER BY updated_at DESC")
    fun observeGroupsWithMembers(): Flow<List<GroupWithMembers>>

    @Query("SELECT * FROM `groups` WHERE group_id = :groupId")
    fun observeGroup(groupId: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM `groups` WHERE group_id = :groupId")
    suspend fun getGroup(groupId: Long): GroupEntity?

    /**
     * REPLACE 走 unique(group_id)，不能用 @Upsert（它只认主键 id）。
     * mock 写入时 id 恒为 0，@Upsert 会反复插入重复行。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroup(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Query("DELETE FROM `groups` WHERE group_id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    // ---- 成员 ----

    @Query("SELECT * FROM group_members WHERE group_id = :groupId ORDER BY CASE `role` WHEN 'OWNER' THEN 0 ELSE 1 END, joined_at ASC")
    fun observeMembers(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT COUNT(*) FROM group_members WHERE group_id = :groupId")
    fun observeMemberCount(groupId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE group_id = :groupId")
    suspend fun deleteMembersOf(groupId: Long)
}
