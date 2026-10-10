package com.zc.aasplitbill.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zc.aasplitbill.feature.friends.FriendsScreen
import com.zc.aasplitbill.ui.screen.BillScreen
import com.zc.aasplitbill.ui.screen.ProfileScreen
import com.zc.aasplitbill.ui.screen.SettlementScreen

/**
 * 应用主界面骨架：Scaffold（顶部留白 + 底部导航栏 + 中间内容区）。
 *
 * 对应需求里的 "main_navigation.xml 导航图" —— Compose 世界里导航图就是下面的 NavHost 代码块，
 * 类型安全（route 是编译期常量）、支持传参，比 XML 导航图更强。
 */
@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    // 当前路由，用于高亮底部导航选中项
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 对应需求里的 BottomNavigationView —— Material3 的 NavigationBar
            NavigationBar {
                BottomNavItem.items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                // 回退到栈底的首页，避免点 4 个 tab 后按返回键要退 4 次
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true   // 切回来时恢复页面状态（如滚动位置）
                                }
                                launchSingleTop = true // 重复点同一 tab 不压新实例
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        // ===== 导航图（NavHost）=====
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding), // 关键：内容区避开底部导航栏高度
        ) {
            composable(BottomNavItem.Home.route) { FriendsScreen() }
            composable(BottomNavItem.Bill.route) { BillScreen() }
            composable(BottomNavItem.Settlement.route) { SettlementScreen() }
            composable(BottomNavItem.Profile.route) { ProfileScreen() }
        }
    }
}
