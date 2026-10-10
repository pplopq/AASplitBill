package com.zc.aasplitbill.feature.auth.data

import android.util.Log
import com.zc.aasplitbill.core.database.dao.UserDao
import com.zc.aasplitbill.core.database.entity.UserEntity
import com.zc.aasplitbill.core.network.api.AuthApiService
import com.zc.aasplitbill.core.network.model.BaseApiResponse
import com.zc.aasplitbill.core.network.token.TokenManager
import com.zc.aasplitbill.feature.auth.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 认证数据层：网络 + 本地存储的唯一出入口，ViewModel 不直接接触 ApiService/TokenManager。
 *
 * 返回统一的 [AuthResult]，业务层不用关心异常类型细节。
 */
sealed interface AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>
    data class Error(val code: Int, val message: String) : AuthResult<Nothing>
    data class NetworkError(val cause: Throwable) : AuthResult<Nothing>
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
) {

    /**
     * DEBUG 兜底开关：后端没起时让登录流程可演示。
     * 后端就绪后把这个变量改成 false，Repository 保持纯粹。
     */
    private var debugMockOnNetworkError = true
    private val isDebug get() = debugMockOnNetworkError && BuildConfig.DEBUG

    suspend fun sendVerificationCode(phoneNumber: String): AuthResult<Unit> =
        safeApiCall { api.sendCode(AuthApiService.SendCodeRequest(phoneNumber)) }.map { }

    /**
     * 登录：成功后把 token 落加密存储、把用户写进 Room（本地缓存）。
     * 返回 token 本身（调用方若要做跳转动画等可用）。
     */
    suspend fun login(phoneNumber: String, code: String): AuthResult<String> {
        val result = safeApiCall {
            api.login(AuthApiService.LoginRequest(phoneNumber, code))
        }
        return when (result) {
            is AuthResult.Success -> {
                val login = result.data
                    ?: return AuthResult.Error(-1, "登录成功但数据为空")

                tokenManager.saveToken(login.token)

                val now = System.currentTimeMillis()
                val existing = userDao.getByPhone(phoneNumber)
                userDao.insert(
                    UserEntity(
                        id = existing?.id ?: 0L,
                        phone = phoneNumber,
                        token = login.token,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                )
                AuthResult.Success(login.token)
            }

            is AuthResult.Error -> result

            // DEBUG 兜底：后端未就绪时走 mock 登录（验证码 123456 放行，与页面提示一致）
            is AuthResult.NetworkError -> if (isDebug) {
                Log.w(TAG, "网络不可达，启用 debug mock 登录: ${result.cause.message}")
                if (code != "123456") {
                    AuthResult.Error(-1, "验证码错误（mock 规则：123456 才能登录）")
                } else {
                    val mockToken = "mock-token-$phoneNumber"
                    tokenManager.saveToken(mockToken)
                    val now = System.currentTimeMillis()
                    val existing = userDao.getByPhone(phoneNumber)
                    userDao.insert(
                        UserEntity(
                            id = existing?.id ?: 0L,
                            phone = phoneNumber,
                            token = mockToken,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                        )
                    )
                    AuthResult.Success(mockToken)
                }
            } else {
                result
            }
        }
    }

    /** 从 API 拉用户信息并缓存到 Room；离线时返回 Room 里最后一份缓存 */
    suspend fun getUserInfo(): AuthResult<AuthApiService.UserInfoResponse> {
        return when (val remote = safeApiCall { api.getUserInfo() }) {
            is AuthResult.Success -> {
                val info = remote.data ?: return AuthResult.Error(-1, "用户信息为空")
                val existing = userDao.getByPhone(info.phone)
                val now = System.currentTimeMillis()
                userDao.insert(
                    UserEntity(
                        id = existing?.id ?: 0L,
                        phone = info.phone,
                        token = tokenManager.getToken(),
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )
                )
                AuthResult.Success(info)
            }

            // 网络挂了读本地缓存兜底（手机号当昵称占位，真实场景应缓存完整用户信息表）
            is AuthResult.NetworkError -> {
                val cached = userDao.getAll().firstOrNull()
                if (cached != null) {
                    AuthResult.Success(
                        AuthApiService.UserInfoResponse(
                            userId = cached.id,
                            phone = cached.phone,
                            nickname = cached.phone,
                        )
                    )
                } else {
                    remote
                }
            }

            is AuthResult.Error -> remote
        }
    }

    /**
     * 刷新 Token。⚠️ 真实项目里这个方法应只被 OkHttp Authenticator 调用
     * （401 时自动刷新重试），业务代码不要直接调。
     */
    suspend fun refreshToken(refreshToken: String): AuthResult<AuthApiService.LoginResponse> =
        safeApiCall { api.refresh(AuthApiService.RefreshRequest(refreshToken)) }

    /** 本地是否有 token。⚠️ 这只是"存不存在"，不等于"服务端没过期"；严格校验要调 /user-info 看是否 401 */
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        !tokenManager.getToken().isNullOrBlank()
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        tokenManager.clearToken()
    }

    // ---- 内部工具 ----

    /**
     * 统一异常收敛：IO 异常（超时/断网）→ NetworkError；
     * 业务错误码 → Error；正常 → Success。
     */
    private suspend fun <T> safeApiCall(
        block: suspend () -> BaseApiResponse<T>,
    ): AuthResult<T> = withContext(Dispatchers.IO) {
        try {
            val resp = block()
            if (resp.isSuccess) {
                AuthResult.Success(resp.data as T)
            } else {
                AuthResult.Error(resp.code, resp.message)
            }
        } catch (e: IOException) {
            AuthResult.NetworkError(e)
        } catch (e: Exception) {
            AuthResult.Error(-1, e.message ?: "未知错误")
        }
    }

    private fun <T> AuthResult<T>.map(transform: (T) -> Unit): AuthResult<Unit> = when (this) {
        is AuthResult.Success -> AuthResult.Success(Unit)
        is AuthResult.Error -> this
        is AuthResult.NetworkError -> this
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
