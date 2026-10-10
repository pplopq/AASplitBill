package com.zc.aasplitbill.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 公共请求头拦截器：所有请求统一带上 Content-Type / Accept。
 * 这类"每个请求都一样的静态头"放拦截器里最干净，业务代码里看不到它们。
 */
class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
