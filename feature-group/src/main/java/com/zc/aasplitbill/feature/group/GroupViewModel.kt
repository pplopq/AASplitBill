package com.zc.aasplitbill.feature.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zc.aasplitbill.core.database.entity.GroupCurrency
import com.zc.aasplitbill.core.database.entity.GroupEntity
import com.zc.aasplitbill.core.database.entity.GroupMemberEntity
import com.zc.aasplitbill.core.database.entity.GroupScene
import com.zc.aasplitbill.feature.group.data.GroupRepository
import com.zc.aasplitbill.feature.group.data.GroupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ===== 创建群组表单状态 =====

data class CreateGroupForm(
    val name: String = "",
    val scene: GroupScene = GroupScene.TRAVEL,
    val currency: GroupCurrency = GroupCurrency.CNY,
) {
    /** 群名 1~20 字才允许提交 */
    val isValid: Boolean get() = name.isNotBlank() && name.length <= 20
}

// ===== 群组详情页状态 =====

data class GroupDetailUiState(
    val group: GroupEntity? = null,
    val members: List<GroupMemberEntity> = emptyList(),
    val inviteLink: String? = null,   // 生成过链接就显示出来，可复制
    val isGeneratingLink: Boolean = false,
)

/** 一次性事件 */
sealed interface GroupEvent {
    data class ShowMessage(val message: String) : GroupEvent
    /** 创建成功，通知页面返回上一页 */
    data object GroupCreated : GroupEvent
}

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val repository: GroupRepository,
) : ViewModel() {

    // ---- 列表页 ----

    val groupList = repository.observeGroups()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ---- 创建表单 ----

    private val _createForm = MutableStateFlow(CreateGroupForm())
    val createForm: StateFlow<CreateGroupForm> = _createForm.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    // ---- 详情页 ----

    private val _detailState = MutableStateFlow(GroupDetailUiState())
    val detailState: StateFlow<GroupDetailUiState> = _detailState.asStateFlow()

    private val _events = MutableSharedFlow<GroupEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GroupEvent> = _events.asSharedFlow()

    init {
        // 进页面拉一次群组列表（mock 环境会落种子数据）
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshGroups()
            _isRefreshing.value = false
        }
    }

    // ---- 创建表单操作 ----

    fun onNameChange(value: String) {
        _createForm.update { it.copy(name = value.take(20)) }
    }

    fun onSceneChange(scene: GroupScene) {
        _createForm.update { it.copy(scene = scene) }
    }

    fun onCurrencyChange(currency: GroupCurrency) {
        _createForm.update { it.copy(currency = currency) }
    }

    fun createGroup() {
        val form = _createForm.value
        if (!form.isValid) {
            _events.tryEmit(GroupEvent.ShowMessage("请填写 1-20 字的群名称"))
            return
        }
        viewModelScope.launch {
            _isCreating.value = true
            when (val result = repository.createGroup(form.name.trim(), form.scene, form.currency)) {
                is GroupResult.Success -> {
                    _createForm.value = CreateGroupForm() // 重置表单
                    _events.tryEmit(GroupEvent.GroupCreated)
                }

                is GroupResult.Error -> _events.tryEmit(GroupEvent.ShowMessage(result.message))
                is GroupResult.NetworkError -> _events.tryEmit(GroupEvent.ShowMessage("网络异常，请稍后重试"))
            }
            _isCreating.value = false
        }
    }

    // ---- 详情页操作 ----

    /** 进入详情页时调用，挂接群组 + 成员的 Flow */
    fun loadGroupDetail(groupId: Long) {
        viewModelScope.launch {
            repository.observeGroup(groupId).collect { group ->
                _detailState.update { it.copy(group = group) }
            }
        }
        viewModelScope.launch {
            repository.observeMembers(groupId).collect { members ->
                _detailState.update { it.copy(members = members) }
            }
        }
    }

    fun generateInviteLink() {
        val group = _detailState.value.group ?: return
        viewModelScope.launch {
            _detailState.update { it.copy(isGeneratingLink = true) }
            val link = repository.generateInviteLink(group.groupId)
            _detailState.update { it.copy(isGeneratingLink = false, inviteLink = link) }
            _events.tryEmit(GroupEvent.ShowMessage("邀请链接已生成，可复制分享"))
        }
    }
}
