package com.zc.aasplitbill.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 手机号验证码登录页。
 *
 * @param onLoginSuccess 登录成功回调，由外层导航跳转到首页（页面不持有 NavController，可独立预览/测试）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 事件流：SnackBar 提示 / 跳转首页。collectLatest 保证新事件覆盖旧处理
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LoginEvent.ShowMessage ->
                    scope.launch { snackbarHostState.showSnackbar(event.message) }

                LoginEvent.NavigateToHome -> onLoginSuccess()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(96.dp))

            Text("AA 分账", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "手机号验证登录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ===== 手机号输入框 =====
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("手机号") },
                placeholder = { Text("请输入 11 位手机号") },
                isError = uiState.phoneError != null,
                supportingText = uiState.phoneError?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 6 位验证码：每个数字一个 Box =====
            VerificationCodeInput(
                code = uiState.verificationCode,
                onCodeChange = viewModel::onCodeChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== 发送验证码（倒计时按钮）=====
            OutlinedButton(
                onClick = viewModel::sendVerificationCode,
                // 倒计时中禁用，文案显示剩余秒数
                enabled = uiState.countdownSeconds == 0 && uiState.phoneNumber.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.countdownSeconds > 0) {
                        "重新发送（${uiState.countdownSeconds}s）"
                    } else {
                        "发送验证码"
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== 登录按钮 =====
            Button(
                onClick = viewModel::login,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("登 录")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "未注册的手机号验证通过后将自动创建账号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 6 位验证码输入：一个透明的 BasicTextField 负责收键盘输入，
 * decorationBox 渲染成 6 个方框，光标位置高亮当前格。
 */
@Composable
private fun VerificationCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    val char = code.getOrNull(index)?.toString() ?: ""
                    val isCurrent = index == code.length // 当前输入位高亮

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = if (isCurrent) 4.dp else 0.dp,
                        shadowElevation = 0.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent || char.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                        modifier = Modifier
                            .width(44.dp)
                            .height(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = char,
                                style = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
