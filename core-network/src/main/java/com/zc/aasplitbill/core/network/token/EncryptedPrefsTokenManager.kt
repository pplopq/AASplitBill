package com.zc.aasplitbill.core.network.token

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenManager 的 EncryptedSharedPreferences 实现。
 *
 * 加密原理：MasterKey（Android Keystore 保护的 AES 密钥）加密整个文件，
 * 即使 adb pull 拿到 xml 也是密文。
 *
 * ⚠️ 风险提示：security-crypto 库官方已停止积极维护（1.1.0-alpha 状态），
 * 已知问题：部分 ROM 的 Keystore 实现有兼容 bug 会导致数据读不出来。
 * 更稳的方案是 DataStore + 自管 Keystore 密钥（或 Tink）。本项目按需求用它，
 * 接入层有 TokenManager 接口，将来替换实现不影响任何业务代码。
 *
 * 已有线上数据注意：从普通 SharedPreferences 迁移到 EncryptedSharedPreferences
 * 必须先读旧值→写新文件→删旧文件，不能直接换文件名，否则老用户全部登出。
 */
@Singleton
class EncryptedPrefsTokenManager @Inject constructor(
    @ApplicationContext context: Context,
) : TokenManager {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS_NAME = "aa_split_bill_secure_prefs"
        const val KEY_TOKEN = "auth_token"
    }
}
