package com.zc.aasplitbill.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zc.aasplitbill.feature.auth.LoginScreen
import com.zc.aasplitbill.feature.auth.data.AuthManager.AuthState

/** 路由常量集中管理，避免散落字符串魔法值 */
object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
}

/**
 * 顶层导航：登录页 ↔ 主界面（底部导航）。
 * 放在 app 壳模块，因为它要同时依赖 feature-auth 和主界面骨架。
 *
 * ## 为什么是"状态驱动"而不是手动 navigate
 *
 * 登录成功、退出登录都会改写 [AuthManager.authState]。这里直接按状态决定显示哪棵树：
 * - [AuthState.Loading]   → 启动加载页（App 启动正在读本地 token，别急着跳）
 * - [AuthState.LoggedIn]  → 主界面（自动登录：本地有 token 就直接进，不经过登录页）
 * - [AuthState.LoggedOut] → 登录页
 *
 * 好处：
 * 1. 自动登录天然成立——启动读 token 的结果直接决定起点，不需要"先显示登录页再跳走"，
 *    避免用户看到登录页一闪而过；
 * 2. 登录成功/登出无需各处手动 navigate，状态一变界面自动切到对应分支，
 *    也就不会出现"跳了但没弹栈"之类的边界问题；
 * 3. 认证判断集中在一处（AuthManager），导航只负责渲染。
 */
@Composable
fun AppNavHost(
    viewModel: AppNavViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsState()

    when (val state = authState) {
        // 启动读取本地 token 中：给个转圈，避免直接闪一下登录页
        AuthState.Loading -> LoadingScreen()

        AuthState.LoggedOut -> {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Routes.LOGIN,
            ) {
                composable(Routes.LOGIN) {
                    // 登录成功后 AuthManager 会把状态改成 LoggedIn，这里无需手动跳首页
                    LoginScreen(onLoginSuccess = {})
                }
            }
        }

        is AuthState.LoggedIn -> {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Routes.MAIN,
            ) {
                composable(Routes.MAIN) {
                    // 退出登录后 AuthManager 会把状态改成 LoggedOut，这里无需手动跳登录页
                    MainNavigation()
                }
            }
        }
    }
}

/** 启动加载页：读本地 token 判断登录态的短暂过渡 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}