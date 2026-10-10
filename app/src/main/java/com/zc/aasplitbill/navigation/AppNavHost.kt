package com.zc.aasplitbill.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zc.aasplitbill.feature.auth.LoginScreen

/** 路由常量集中管理，避免散落字符串魔法值 */
object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
}

/**
 * 顶层导航：登录页 → 主界面（底部导航）。
 * 放在 app 壳模块，因为它要同时依赖 feature-auth 和主界面骨架。
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        // 登录成功把登录页从返回栈弹掉，按返回键不会退回登录页
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            MainNavigation()
        }
    }
}
