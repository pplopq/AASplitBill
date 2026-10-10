package com.zc.aasplitbill.core.network.interceptor

import com.zc.aasplitbill.core.network.token.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Token 拦截器：已登录请求自动带上 Authorization 头。
 *
 * 设计决策：
 * 1. 无 token 时直接放行——登录/注册接口本身就不该带 token，不能拦截；
 * 2. token 刷新逻辑不在这里做。401 统一交给 Authenticator（ OkHttp 原语），
 *    拦截器里做刷新会引入嵌套请求和死锁风险，这是新手最常踩的坑。
 */
class TokenInterceptor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val original = chain.request()

        if (token.isNullOrBlank()) {
            return chain.proceed(original) // 未登录，原样放行
        }

        val request = original.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}
