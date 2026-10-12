package com.zc.aasplitbill

import androidx.compose.runtime.Composable
import com.zc.aasplitbill.base.BaseComposeActivity
import com.zc.aasplitbill.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * 壳模块的启动 Activity。
 *
 * 继承 BaseComposeActivity：主题包裹由基类完成，这里只声明导航骨架。
 */
@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        AppNavHost()
    }
}