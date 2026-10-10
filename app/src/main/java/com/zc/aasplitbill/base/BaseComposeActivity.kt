package com.zc.aasplitbill.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.zc.aasplitbill.core.common.BaseActivity
import com.zc.aasplitbill.ui.theme.AASplitBillTheme

/**
 * Compose 专用 Activity 基类：统一接管 setContent + MaterialTheme。
 *
 * 子类只需要实现 [Content]，返回界面 Composable，
 * 主题包裹、深浅色切换、动态取色都在这里一次完成，业务代码零样板。
 *
 * 为什么不放 core-common：主题定义在 app 模块（ui/theme），
 * core-common 又不该依赖 Compose——分层错了会产生循环依赖。
 */
abstract class BaseComposeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AASplitBillTheme {
                Content()
            }
        }
    }

    @Composable
    abstract fun Content()
}
