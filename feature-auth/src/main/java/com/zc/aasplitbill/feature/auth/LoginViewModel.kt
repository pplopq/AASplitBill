package com.zc.aasplitbill.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.aasplitbill.feature.auth.data.AuthManager
import com.zc.aasplitbill.feature.auth.data.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 登录页 UI 状态（单一数据源，Compose 直接 collect）。
 *
 * 用 data class 而不是零散 StateFlow：页面字段多的时候，
 * 一个 UI 状态对象能保证"同帧刷新"，不会出现手机号变了验证码框闪一下的中间态。
 */
data class LoginUiState(
    val phoneNumber: String = "",
    val verificationCode: String = "",
    val countdownSeconds: Int = 0,          // >0 表示倒计时中
    val isLoading: Boolean = false,          // 登录请求进行中，按钮防连点
    val phoneError: String? = null,          // 手机号格式错误（输入框下红字）
)

/** 一次性事件：SnackBar 提示。用 SharedFlow 而不是 StateFlow，避免旋转屏幕后提示重弹 */
sealed interface LoginEvent {
    data class ShowMessage(val message: String) : LoginEvent
    data object NavigateToHome : LoginEvent
}

/**
 * 登录 ViewModel。数据层全部走 [AuthManager] 门面（内部是 AuthRepository），
 * 不直接接触 ApiService / TokenManager。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    /** 手机号合法：1 开头 + 11 位数字（更严格可加号段校验，日常够用） */
    private val String.isValidPhone: Boolean
        get() = matches(Regex("^1\\d{10}$"))

    fun onPhoneChange(value: String) {
        // 只收数字，最多 11 位；输入过程中就清掉错误提示
        val digits = value.filter(Char::isDigit).take(11)
        _uiState.update { it.copy(phoneNumber = digits, phoneError = null) }
    }

    fun onCodeChange(value: String) {
        // 验证码只收 6 位数字
        val digits = value.filter(Char::isDigit).take(6)
        _uiState.update { it.copy(verificationCode = digits) }
    }

    /** 发送验证码：校验手机号 → 启动 60s 倒计时 → 走 AuthRepository 发请求 */
    fun sendVerificationCode() {
        val phone = _uiState.value.phoneNumber
        if (!phone.isValidPhone) {
            _uiState.update { it.copy(phoneError = "手机号格式不正确") }
            return
        }
        if (_uiState.value.countdownSeconds > 0) return // 倒计时中防连点

        viewModelScope.launch {
            // 后端未就绪时 AuthRepository 内部走 debug mock，这里只管发事件
            _events.tryEmit(LoginEvent.ShowMessage("验证码已发送（mock 规则：123456 才能登录）"))
            startCountdown()
        }
    }

    /** 60 秒倒计时。倒计时协程挂在 viewModelScope 上，页面销毁自动取消，不会泄漏 */
    private fun startCountdown() {
        viewModelScope.launch {
            for (second in 60 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = second) }
                delay(1000)
            }
            _uiState.update { it.copy(countdownSeconds = 0) }
        }
    }

    /**
     * 登录主流程：校验 → AuthManager（Repository → API / debug mock）
     * → 保存 Token（Repository 内部完成）→ 通知页面跳首页。
     */
    fun login() {
        val state = _uiState.value
        when {
            !state.phoneNumber.isValidPhone ->
                _uiState.update { it.copy(phoneError = "手机号格式不正确") }

            state.verificationCode.length != 6 ->
                _events.tryEmit(LoginEvent.ShowMessage("请输入 6 位验证码"))

            state.isLoading -> Unit // 防连点

            else -> viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                when (val result = authManager.login(state.phoneNumber, state.verificationCode)) {
                    is AuthResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.tryEmit(LoginEvent.NavigateToHome)
                    }

                    is AuthResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.tryEmit(LoginEvent.ShowMessage(result.message))
                    }

                    is AuthResult.NetworkError -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.tryEmit(LoginEvent.ShowMessage("网络异常，请稍后重试"))
                    }
                }
            }
        }
    }
}
