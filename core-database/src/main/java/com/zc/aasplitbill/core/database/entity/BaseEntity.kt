package com.zc.aasplitbill.core.database.entity

/**
 * Entity 公共字段的"契约"定义。
 *
 * ⚠️ 为什么不写成带 @PrimaryKey/@ColumnInfo 注解的基类：
 * Room 官方不支持 Entity 类继承（KSP 下基类的注解和字段都不会被处理，
 * 编译期报 "entity must have at least 1 field annotated with @PrimaryKey"）。
 * 所以这里退而求其次，用接口约定字段名，每个 Entity 自行声明带注解的字段。
 *
 * 需要全表通用行为时，用 Repository 层封装，而不是数据库层继承。
 */
interface BaseEntity {
    val id: Long
    val createdAt: Long
    val updatedAt: Long
}
