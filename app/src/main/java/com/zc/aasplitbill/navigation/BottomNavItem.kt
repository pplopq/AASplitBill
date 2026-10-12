package com.zc.aasplitbill.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航的 4 个目的地定义。
 * route 是 NavHost 里的唯一路由标识，也是 SavedStateHandle 取参数的 key。
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Home : BottomNavItem("home", "首页", Icons.Filled.Home)
    data object Bill : BottomNavItem("bill", "群组", Icons.Filled.Groups)
    data object Settlement : BottomNavItem("settlement", "结算", Icons.Filled.AddCircle)
    data object Profile : BottomNavItem("profile", "我的", Icons.Filled.Person)

    companion object {
        /** 导航栏展示顺序即此列表顺序 */
        val items = listOf(Home, Bill, Settlement, Profile)
    }
}
