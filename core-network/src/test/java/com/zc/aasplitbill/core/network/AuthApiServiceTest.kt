package com.zc.aasplitbill.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zc.aasplitbill.core.network.api.AuthApiService
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * AuthApiService 的 MockWebServer 测试：
 * 不起真后端、不用模拟器，在 JVM 上验证「请求拼装 + 响应解析」。
 */
class AuthApiServiceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildApi(): AuthApiService {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl(server.url("/"))   // MockWebServer 动态端口
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }

    @Test
    fun `login 解析成功响应`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"code": 0, "message": "ok", "data": {"token": "t-abc", "user_id": 42}}"""
            )
        )

        val resp = buildApi().login(AuthApiService.LoginRequest("13800000000", "123456"))

        assertTrue(resp.isSuccess)
        assertEquals("t-abc", resp.data!!.token)
        assertEquals(42L, resp.data!!.userId)

        // 校验请求拼装：方法、路径、请求体
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/auth/login", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("13800000000"))
    }

    @Test
    fun `login 解析业务错误码`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"code": 1001, "message": "验证码错误", "data": null}""")
        )

        val resp = buildApi().login(AuthApiService.LoginRequest("13800000000", "000000"))

        assertEquals(1001, resp.code)
        assertEquals("验证码错误", resp.message)
        assertTrue(resp.data == null)
    }
}
