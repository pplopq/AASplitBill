package com.zc.aasplitbill.core.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 所有 Activity 的公共基类。
 *
 * @AndroidEntryPoint 让 Hilt 能向本 Activity（及其持有的 @HiltViewModel）注入依赖。
 *
 * 两条硬性规则：
 *   1. 依赖注入发生在 super.onCreate() 内部，子类必须调用 super.onCreate(savedInstanceState)，
 *      否则会抛 "super.onCreate(savedInstanceState) must be called" 之类的异常。
 *   2. 子类若自己也标了 @AndroidEntryPoint，仍是合法且常见的写法（Hilt 支持继承链上重复标注）。
 *
 * 注意：本基类不直接调用 setContent——那样会强制所有子类都变成 Compose。
 * Compose 子类用 BaseComposeActivity（内部包了 MaterialTheme），
 * 万一以后有 WebView/相机等必须用 View 的场景，继承 BaseActivity 不受影响。
 */
@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)   // ← Hilt 注入时机在这行，不能漏
    }
}
