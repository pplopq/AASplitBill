package com.zc.aasplitbill.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * 占位页面通用骨架：居中显示标题 + "页面开发中"。
 * 后续每个 feature 模块实现好后，替换这里的内容即可。
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            "页面开发中…",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HomeScreen() = PlaceholderScreen("首页")

@Composable
fun BillScreen() = PlaceholderScreen("账单")

@Composable
fun SettlementScreen() = PlaceholderScreen("结算")
