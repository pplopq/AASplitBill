package com.zc.aasplitbill.core.network.model

/**
 * 接口统一响应包装（约定式包装，和后端对齐字段名）。
 *
 * T 为业务数据本体。注意不要用 Kotlin 的 Result 类做网络层返回——
 * 它的泛型擦除和 inline 限制会给跨模块传递带来麻烦。
 */
data class BaseApiResponse<T>(
    val code: Int,          // 0 或 200 表示成功，具体约定跟后端确认
    val message: String,
    val data: T? = null,
) {
    val isSuccess: Boolean get() = code == 0 || code == 200
}
