package com.zc.aasplitbill.feature.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zc.aasplitbill.core.database.entity.GroupCurrency
import com.zc.aasplitbill.core.database.entity.GroupEntity
import com.zc.aasplitbill.core.database.entity.GroupScene

/**
 * 群组列表页：群名 + 场景图标 + 成员数/头像缩略 + 默认货币。
 *
 * @param onCreateClick 点 FAB 进创建表单
 * @param onGroupClick 点群条目进详情
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onCreateClick: () -> Unit,
    onGroupClick: (Long) -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val groups by viewModel.groupList.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("群组") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Filled.Add, contentDescription = "创建群组")
            }
        },
    ) { innerPadding ->
        when {
            isRefreshing && groups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            groups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无群组",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onCreateClick) { Text("创建第一个群组") }
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(groups, key = { it.groupId }) { group ->
                    GroupItem(
                        group = group,
                        onClick = { onGroupClick(group.groupId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupItem(
    group: GroupEntity,
    onClick: () -> Unit,
) {
    val scene = runCatching { GroupScene.valueOf(group.scene) }.getOrDefault(GroupScene.OTHER)
    val currency = runCatching { GroupCurrency.valueOf(group.currency) }.getOrDefault(GroupCurrency.CNY)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 场景图标：emoji 大圆底，代替真实图标库
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(scene.emoji, style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                scene.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            MemberAvatarRow(
                groupName = group.name,
                memberCount = group.memberCount,
            )
        }

        // 默认货币角标
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                currency.symbol,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * 成员头像缩略图 + 成员数。
 *
 * 列表页只有 memberCount 数字、没有成员表数据，这里用"重叠首字圆"占位表达
 * "展示成员头像缩略图"的需求意图：群名首字 + 通用头像占位，最多叠 3 个。
 */
@Composable
private fun MemberAvatarRow(
    groupName: String,
    memberCount: Int,
) {
    val placeholders = listOf(groupName.take(1).ifEmpty { "👤" }, "👤", "👤")
    val visibleCount = when {
        memberCount <= 0 -> 0
        memberCount == 1 -> 1
        memberCount == 2 -> 2
        else -> 3
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        placeholders.take(visibleCount).forEachIndexed { index, initial ->
            Surface(
                shape = CircleShape,
                color = when (index) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                },
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = (-6 * index).dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            1 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "${memberCount} 名成员",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
