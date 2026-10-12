package com.zc.aasplitbill.feature.group

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * 群组模块内部导航图。
 *
 * 挂在"账单"tab 下面：底部导航只管 4 个顶层目的地，
 * 群组自己的列表/创建/详情用嵌套 NavHost 管理，互不干扰。
 */
object GroupRoutes {
    const val LIST = "group_list"
    const val CREATE = "group_create"
    const val DETAIL = "group_detail/{groupId}"

    fun detail(groupId: Long) = "group_detail/$groupId"
}

@Composable
fun GroupNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = GroupRoutes.LIST) {
        composable(GroupRoutes.LIST) {
            GroupScreen(
                onCreateClick = { navController.navigate(GroupRoutes.CREATE) },
                onGroupClick = { groupId -> navController.navigate(GroupRoutes.detail(groupId)) },
            )
        }
        composable(GroupRoutes.CREATE) {
            CreateGroupScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = GroupRoutes.DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType }),
        ) { backStackEntry ->
            GroupDetailScreen(
                groupId = backStackEntry.arguments?.getLong("groupId") ?: return@composable,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
