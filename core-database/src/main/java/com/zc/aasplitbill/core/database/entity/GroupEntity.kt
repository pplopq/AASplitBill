package com.zc.aasplitbill.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 群组表。
 *
 * scene/currency 存枚举名字符串（而非 ordinal），原因：
 * 1. 数据库可读（"TRAVEL" 比 "0" 好排查）；
 * 2. 插枚举不会把后续新增项的顺序耦合进旧数据（ordinal 插值就错位）。
 */
@Entity(
    tableName = "groups",
    indices = [Index(value = ["group_id"], unique = true)],
)
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    override val id: Long = 0L,
    /** 服务端群组 ID；本地 mock 数据用负数段（-1xxxxx），避免与线上 ID 冲突 */
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    /** [GroupScene] 的 name */
    @ColumnInfo(name = "scene")
    val scene: String,
    /** [GroupCurrency] 的 name */
    @ColumnInfo(name = "currency")
    val currency: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: Long,
    @ColumnInfo(name = "owner_nickname")
    val ownerNickname: String,
    @ColumnInfo(name = "member_count")
    val memberCount: Int,
    @ColumnInfo(name = "created_at")
    override val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    override val updatedAt: Long,
) : BaseEntity

/** 场景 */
enum class GroupScene(val label: String, val emoji: String) {
    TRAVEL("旅行", "✈️"),
    RENTAL("合租", "🏠"),
    DINNER("聚餐", "🍽️"),
    OTHER("其他", "📦"),
}

/** 默认货币 */
enum class GroupCurrency(val label: String, val symbol: String) {
    CNY("人民币", "¥"),
    USD("美元", "$"),
    JPY("日元", "JP¥"),
    EUR("欧元", "€"),
}
