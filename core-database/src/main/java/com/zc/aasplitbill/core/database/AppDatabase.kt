package com.zc.aasplitbill.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zc.aasplitbill.core.database.dao.FriendDao
import com.zc.aasplitbill.core.database.dao.GroupDao
import com.zc.aasplitbill.core.database.dao.UserDao
import com.zc.aasplitbill.core.database.entity.FriendEntity
import com.zc.aasplitbill.core.database.entity.GroupEntity
import com.zc.aasplitbill.core.database.entity.GroupMemberEntity
import com.zc.aasplitbill.core.database.entity.UserEntity

/**
 * 应用数据库。
 *
 * version 从 2 开始：v1 是空库占位，v2 才是第一个有真实表结构的版本。
 * 每次加表/改列都要升 version 并写对应的 Migration（或声明 @DestructiveMigration 仅用于 debug）。
 */
@Database(
    entities = [UserEntity::class, FriendEntity::class, GroupEntity::class, GroupMemberEntity::class],
    version = 4,
    exportSchema = false, // 建议开发期导出 schema 做版本管理；项目初期先关掉
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao

    companion object {
        const val DATABASE_NAME = "aasplitbill.db"

        /**
         * 版本 1 → 2：空迁移占位。
         * 真实场景 v1 是空库、v2 新增 users 表，直接建表即可；
         * 这里显式写出来是为了给你演示 Migration 的标准写法。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `token` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_phone` ON `users` (`phone`)")
            }
        }

        // 后续版本在这里追加：
        // val MIGRATION_3_4 = object : Migration(3, 4) { ... }

        /** 版本 2 → 3：新增好友表 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `friends` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `friend_user_id` INTEGER NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `avatar_url` TEXT,
                        `phone` TEXT NOT NULL,
                        `last_active_at` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_friends_friend_user_id` ON `friends` (`friend_user_id`)")
            }
        }

        /** 版本 3 → 4：新增群组表和群组成员表 */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `group_id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `scene` TEXT NOT NULL,
                        `currency` TEXT NOT NULL,
                        `owner_id` INTEGER NOT NULL,
                        `owner_nickname` TEXT NOT NULL,
                        `member_count` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_groups_group_id` ON `groups` (`group_id`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `group_members` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `group_id` INTEGER NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `avatar_url` TEXT,
                        `role` TEXT NOT NULL,
                        `joined_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_group_members_group_id_user_id` ON `group_members` (`group_id`, `user_id`)")
            }
        }
    }
}
