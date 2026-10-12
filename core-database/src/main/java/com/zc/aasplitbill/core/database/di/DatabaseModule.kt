package com.zc.aasplitbill.core.database.di

import android.content.Context
import androidx.room.Room
import com.zc.aasplitbill.core.database.AppDatabase
import com.zc.aasplitbill.core.database.dao.FriendDao
import com.zc.aasplitbill.core.database.dao.GroupDao
import com.zc.aasplitbill.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库模块：向 Hilt 容器提供 AppDatabase 单例及各 DAO。
 *
 * 要点：
 * 1. @InstallIn(SingletonComponent) —— 数据库必须是进程级单例，否则连接池会重复创建。
 * 2. @ApplicationContext —— 必须用应用级 Context，不能用 Activity 的，否则会造成内存泄漏。
 * 3. DAO 的 Provide 方法不需要再标 @Singleton，Room 生成的 DAO 本来就是轻量代理，
 *    且依赖同一 database 实例；标了也无害，但属于冗余。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
        )   // 版本迁移链，逐版本追加
        // .fallbackToDestructiveMigration()         // 仅 debug 用！上线绝对禁止
        .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideFriendDao(database: AppDatabase): FriendDao = database.friendDao()

    @Provides
    fun provideGroupDao(database: AppDatabase): GroupDao = database.groupDao()
}
