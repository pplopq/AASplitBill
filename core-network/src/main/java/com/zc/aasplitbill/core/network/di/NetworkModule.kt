package com.zc.aasplitbill.core.network.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zc.aasplitbill.core.network.api.AuthApiService
import com.zc.aasplitbill.core.network.api.FriendApiService
import com.zc.aasplitbill.core.network.api.GroupApiService
import com.zc.aasplitbill.core.network.interceptor.HeaderInterceptor
import com.zc.aasplitbill.core.network.interceptor.TokenInterceptor
import com.zc.aasplitbill.core.network.token.EncryptedPrefsTokenManager
import com.zc.aasplitbill.core.network.token.TokenManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * 网络层 DI 模块。
 *
 * 对应需求里的 "RetrofitClient 单例"——在 Hilt 项目里，单例由
 * @Singleton + SingletonComponent 保证，不需要也不应该有静态 getInstance()。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 后端地址。开发期连本机，上线前挪到 BuildConfig 按 buildType 注入 */
    private const val BASE_URL = "http://10.2:8080/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // 没有它 data class 的默认值/可空字段处理会有坑
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenManager: TokenManager,
        @ApplicationContext context: Context,
    ): OkHttpClient {
        // 日志拦截器：只打 BODY 级别，且只在 Debug 包启用，防止抓包泄露 token
        val logging = HttpLoggingInterceptor().apply {
            level = if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            // 拦截器顺序即添加顺序：先加日志方便看完整链，再加头，最后加 token
            .addInterceptor(logging)
            .addInterceptor(HeaderInterceptor())
            .addInterceptor(TokenInterceptor(tokenManager))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideFriendApiService(retrofit: Retrofit): FriendApiService =
        retrofit.create(FriendApiService::class.java)

    @Provides
    @Singleton
    fun provideGroupApiService(retrofit: Retrofit): GroupApiService =
        retrofit.create(GroupApiService::class.java)
}

/**
 * TokenManager 的绑定：接口 → SharedPreferences 实现。
 * 单独用 @Binds 模块是 Hilt 惯例，比 @Provides 少一次对象创建。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenManagerModule {
    @Binds
    abstract fun bindTokenManager(impl: EncryptedPrefsTokenManager): TokenManager
}
