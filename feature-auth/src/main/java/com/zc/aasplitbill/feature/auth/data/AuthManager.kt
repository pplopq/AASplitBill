package com.zc.aasplitbill.feature.auth.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局认证状态管理（Hilt 单例，全进程一份）。
 *
 * 两个职责：
 * 1. 以 [AuthState] 的形式把登录态广播给所有监听者（启动页决定跳登录还是跳首页、
 *    "我的"页面显示登录/未登录，都靠它）；
 * 2. 聚合 [AuthRepository] 的认证操作，UI 层只面对 AuthManager 一个门面。
 *
 * "单例"靠 @Singleton 保证，不要写伴生对象 static —— 那是 Hilt 之前的写法，
 * 会造成测试无法替换实现。
 */
@Singleton
class AuthManager @Inject constructor(
    private val repository: AuthRepository,
) {

    /** 认证状态：UI 层 collect 它做条件渲染 */
    sealed interface AuthState {
        data object Loading : AuthState          // 启动时还在读本地 token
        data object LoggedOut : AuthState
        data class LoggedIn(val token: String) : AuthState
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** App 启动时调用一次：本地有 token 就认为已登录（严格校验走 /user-info 的 401 判断） */
    suspend fun restoreSession() {
        _authState.value = if (repository.isLoggedIn()) {
            // 顺手刷新一次用户信息缓存，失败不阻塞登录态判断
            repository.getUserInfo()
            AuthState.LoggedIn("restored")
        } else {
            AuthState.LoggedOut
        }
    }

    suspend fun login(phoneNumber: String, code: String): AuthResult<String> {
        return when (val result = repository.login(phoneNumber, code)) {
            is AuthResult.Success -> {
                _authState.value = AuthState.LoggedIn(result.data)
                result
            }

            is AuthResult.Error -> result
            is AuthResult.NetworkError -> result
        }
    }

    suspend fun logout() {
        repository.logout()
        _authState.value = AuthState.LoggedOut
    }
}
