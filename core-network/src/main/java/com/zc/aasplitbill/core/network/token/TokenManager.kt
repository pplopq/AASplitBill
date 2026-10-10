package com.zc.aasplitbill.core.network.token

/**
 * Token 存取的抽象。core-network 不直接依赖 Android 存储实现，
 * 由 di 层注入具体实现（当前是 SharedPreferences，后续可无痛换成 DataStore）。
 *
 * 为什么抽接口：网络层要做单元测试时，直接 fake 这个接口即可，不用 Mock SharedPreferences。
 */
interface TokenManager {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}
