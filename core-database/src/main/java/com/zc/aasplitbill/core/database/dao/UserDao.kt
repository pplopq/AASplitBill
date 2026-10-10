package com.zc.aasplitbill.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.zc.aasplitbill.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    /**
     * 插入用户。冲突（手机号已存在）时替换旧记录。
     * 返回新行的 rowId，-1 表示发生冲突被替换（具体语义以 OnConflictStrategy 为准）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    /** 更新整行；返回受影响行数，0 表示没有这条记录 */
    @Update
    suspend fun update(user: UserEntity): Int

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    /** Flow 版本：表数据变化时自动重发，给 Compose 界面做响应式刷新 */
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    suspend fun getAll(): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): UserEntity?
}
