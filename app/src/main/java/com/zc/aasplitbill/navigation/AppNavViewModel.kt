package com.zc.aasplitbill.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.aasplitbill.feature.auth.data.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 顶层导航 ViewModel：把 [AuthManager] 的全局登录态暴露给 [AppNavHost]。
 *
 * 为什么要有这个 ViewModel：restoreSession() 是 suspend 且有副作用（读本地 token），
 * 放在 Composable 里直接调不合适；放 ViewModel 的 init 里由 viewModelScope 承载，
 * 页面重建不会重复触发恢复（ViewModel 生命周期跟随 NavHost）。
 */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    /**
     * 导航的唯一权威：AppNavHost 直接按它决定显示启动加载页 / 主界面 / 登录页。
     * 登录成功、登出都会改写这个状态，导航随之自动切换，无需各处手动 navigate。
     */
    val authState = authManager.authState

    init {
        viewModelScope.launch {
            // 本地有 token → LoggedIn（刷新用户信息失败不阻塞判断），无 → LoggedOut
            authManager.restoreSession()
        }
    }
}