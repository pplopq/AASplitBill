package com.zc.aasplitbill.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.aasplitbill.core.database.dao.UserDao
import com.zc.aasplitbill.feature.auth.data.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** "我的"页 UI 状态（单一数据源，Compose 直接 collect）。 */
data class ProfileUiState(
    val phone: String = "",
    val createdAt: Long = 0L,
    val isLoading: Boolean = true,
)

/**
 * "我的"页 ViewModel。
 *
 * 账号信息来自 Room 本地缓存（登录时 AuthRepository 已写入 users 表），
 * 登出统一委托 [AuthManager.logout()]（清 token + 置全局 LoggedOut 态）。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    userDao: UserDao,
) : ViewModel() {

    // 按 updated_at 倒序取第一条 = 最近登录/活跃的账号
    val uiState = userDao.observeAll()
        .map { users ->
            val current = users.sortedByDescending { it.updatedAt }.firstOrNull()
            ProfileUiState(
                phone = current?.phone.orEmpty(),
                createdAt = current?.createdAt ?: 0L,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState(),
        )

    /**
     * 登出：清 token 并把全局 authState 置为 LoggedOut。
     * 页面无需关心跳转——AppNavHost 监听 authState 会自动切回登录页。
     */
    fun logout() {
        viewModelScope.launch { authManager.logout() }
    }
}