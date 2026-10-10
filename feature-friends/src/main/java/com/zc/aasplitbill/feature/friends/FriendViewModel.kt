package com.zc.aasplitbill.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.aasplitbill.core.database.entity.FriendEntity
import com.zc.aasplitbill.feature.friends.data.FriendRepository
import com.zc.aasplitbill.feature.friends.data.FriendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 添加好友对话框状态 */
data class AddFriendState(
    val isDialogVisible: Boolean = false,
    val phoneInput: String = "",
    val isSearching: Boolean = false,
    /** 搜到的陌生用户（尚未添加），null = 未搜索或无结果 */
    val foundUser: FriendEntity? = null,
)

/** 好友列表页 UI 状态 */
data class FriendsUiState(
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val addFriend: AddFriendState = AddFriendState(),
)

/** 一次性事件 */
sealed interface FriendsEvent {
    data class ShowMessage(val message: String) : FriendsEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class FriendViewModel @Inject constructor(
    private val repository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FriendsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FriendsEvent> = _events.asSharedFlow()

    /** 搜索框输入流：300ms 去抖 + 去重，避免每个字符都查一次 Room */
    private val searchQueryFlow = MutableStateFlow("")

    /**
     * 好友列表：关键字为空走全量 Flow，有关键字走 Room 模糊搜索。
     * flatMapLatest：关键字变更瞬间旧查询自动取消（Flow 天然背压，不用手写取消）。
     */
    val friendList = searchQueryFlow
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeFriends()
            else repository.searchLocalFriends(query)
        }

    init {
        // 进入页面刷新一次列表（后端未就绪时 Repository 落 mock 数据）
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repository.refreshFriends()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onSearchQueryChange(value: String) {
        _uiState.update { it.copy(searchQuery = value) }
        searchQueryFlow.value = value
    }

    // ---- 添加好友对话框 ----

    fun showAddDialog() {
        _uiState.update { it.copy(addFriend = AddFriendState(isDialogVisible = true)) }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(addFriend = AddFriendState()) }
    }

    fun onDialogPhoneChange(value: String) {
        val digits = value.filter(Char::isDigit).take(11)
        _uiState.update {
            it.copy(addFriend = it.addFriend.copy(phoneInput = digits, foundUser = null))
        }
    }

    /** 对话框里点"搜索"：按手机号查用户 */
    fun searchUserByPhone() {
        val phone = _uiState.value.addFriend.phoneInput
        if (!phone.matches(Regex("^1\\d{10}$"))) {
            viewModelScope.launch { _events.tryEmit(FriendsEvent.ShowMessage("手机号格式不正确")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(addFriend = it.addFriend.copy(isSearching = true)) }
            when (val result = repository.searchByPhone(phone)) {
                is FriendResult.Success -> _uiState.update {
                    it.copy(
                        addFriend = it.addFriend.copy(isSearching = false, foundUser = result.data)
                    )
                }

                is FriendResult.Error -> {
                    _uiState.update { s ->
                        s.copy(addFriend = s.addFriend.copy(isSearching = false, foundUser = null))
                    }
                    _events.tryEmit(FriendsEvent.ShowMessage(result.message))
                }

                is FriendResult.NetworkError -> {
                    _uiState.update { s ->
                        s.copy(addFriend = s.addFriend.copy(isSearching = false, foundUser = null))
                    }
                    _events.tryEmit(FriendsEvent.ShowMessage("网络异常，请稍后重试"))
                }
            }
        }
    }

    fun addFriend(friend: FriendEntity) {
        viewModelScope.launch {
            when (val result = repository.addFriend(friend)) {
                is FriendResult.Success -> {
                    dismissAddDialog()
                    _events.tryEmit(FriendsEvent.ShowMessage("已添加 ${friend.nickname}"))
                }

                is FriendResult.Error -> _events.tryEmit(FriendsEvent.ShowMessage(result.message))
                is FriendResult.NetworkError -> _events.tryEmit(FriendsEvent.ShowMessage("网络异常，请稍后重试"))
            }
        }
    }
}
