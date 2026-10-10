package com.zc.aasplitbill.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户表。
 *
 * phone 加唯一索引：AA 分账场景下手机号即账号，注册/登录时靠它查重。
 * token 是登录态凭证，退出登录时清空即可，不用建索引（只按 id/phone 查）。
 *
 * 字段注解必须写在本类里（见 BaseEntity 的注释，Room 不支持实体继承）。
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["phone"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long = 0L,

    @ColumnInfo(name = "phone")
    val phone: String,

    @ColumnInfo(name = "token")
    val token: String? = null, // 未登录时为空，登录后写入

    @ColumnInfo(name = "created_at")
    override val createdAt: Long = 0L,

    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long = 0L,
) : BaseEntity
