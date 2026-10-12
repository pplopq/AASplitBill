package com.zc.aasplitbill.feature.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zc.aasplitbill.core.database.entity.GroupCurrency
import com.zc.aasplitbill.core.database.entity.GroupScene
import kotlinx.coroutines.flow.collectLatest

/**
 * 创建群组表单页：群名 + 场景选择（旅行/合租/聚餐/其他）+ 默认货币。
 *
 * @param onBack 创建成功或点返回时退出（页面不持有 NavController，回调式导航）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val form by viewModel.createForm.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GroupEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                // 创建成功 → 直接返回列表页（Room Flow 会自动带上新群）
                GroupEvent.GroupCreated -> onBack()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("创建群组") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ===== 群名称 =====
            OutlinedTextField(
                value = form.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("群组名称") },
                placeholder = { Text("例如：国庆日本自由行") },
                singleLine = true,
                supportingText = { Text("${form.name.length}/20") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 场景选择 =====
            Text("场景", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GroupScene.entries.forEach { scene ->
                    FilterChip(
                        selected = form.scene == scene,
                        onClick = { viewModel.onSceneChange(scene) },
                        label = { Text("${scene.emoji} ${scene.label}") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 默认货币 =====
            Text("默认货币", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GroupCurrency.entries.forEach { currency ->
                    FilterChip(
                        selected = form.currency == currency,
                        onClick = { viewModel.onCurrencyChange(currency) },
                        label = { Text("${currency.symbol} ${currency.label}") },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== 提交 =====
            Button(
                onClick = viewModel::createGroup,
                enabled = form.isValid && !isCreating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("创建")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
