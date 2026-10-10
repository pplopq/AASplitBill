package com.zc.aasplitbill

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.zc.aasplitbill.base.BaseComposeActivity
import com.zc.aasplitbill.core.database.dao.UserDao
import com.zc.aasplitbill.core.database.entity.UserEntity
import com.zc.aasplitbill.core.network.api.AuthApiService
import com.zc.aasplitbill.core.network.token.TokenManager
import com.zc.aasplitbill.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 壳模块的启动 Activity。
 *
 * 继承 BaseComposeActivity：主题包裹由基类完成，这里只声明导航骨架。
 *
 * 末尾的 userDao / apiService 冒烟测试：验证 Room + Retrofit + Hilt 注入链路可用，
 * 正式开发时删除。
 */
@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {

    @Inject
    lateinit var userDao: UserDao // ← DatabaseModule 提供，验证 Room + Hilt

    @Inject
    lateinit var apiService: AuthApiService // ← NetworkModule 提供，验证 Retrofit + Hilt

    @Inject
    lateinit var tokenManager: TokenManager // ← TokenManagerModule 提供

    @Composable
    override fun Content() {
        AppNavHost()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== Room 冒烟测试（验证用，可删）=====
        lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val rowId = userDao.insert(
                UserEntity(
                    phone = "13800000000",
                    token = "smoke-test-token",
                    createdAt = now,
                    updatedAt = now,
                )
            )
            val user = userDao.getById(rowId)
            Log.i("SmokeTest", "[Room] insert rowId=$rowId, readBack=${user?.phone}")

            // ===== 网络层冒烟：验证 DI 图构建 + Token 存取链路（不发真实请求）=====
            tokenManager.saveToken("smoke-token")
            Log.i("SmokeTest", "[Network] apiService=$apiService")
            Log.i("SmokeTest", "[Network] token readBack=${tokenManager.getToken()}")
            tokenManager.clearToken()
        }
    }
}
