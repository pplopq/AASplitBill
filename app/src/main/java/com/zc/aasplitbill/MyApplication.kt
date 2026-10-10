package com.zc.aasplitbill

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。@HiltAndroidApp 触发 Hilt 的代码生成，
 * 生成应用级别的依赖容器（AppComponent），所有模块的注入都挂在这棵树上。
 *
 * ⚠️ 必须注册到 AndroidManifest.xml 的 <application android:name=".MyApplication">
 */
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局只初始化一次的东西放这里（崩溃上报、日志框架等）
        // 注意：不要在这里做耗时操作，会影响冷启动时间
    }
}
