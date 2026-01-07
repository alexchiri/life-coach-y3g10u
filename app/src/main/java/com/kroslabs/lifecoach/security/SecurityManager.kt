package com.kroslabs.lifecoach.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

class SecurityManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_ALIAS = "lifecoach_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val AUTH_SETUP_COMPLETE_KEY = booleanPreferencesKey("auth_setup_complete")
        private val DB_KEY_IV = stringPreferencesKey("db_key_iv")
        private val DB_KEY_ENCRYPTED = stringPreferencesKey("db_key_encrypted")
        private val API_KEY_IV = stringPreferencesKey("api_key_iv")
        private val API_KEY_ENCRYPTED = stringPreferencesKey("api_key_encrypted")
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    val isAuthSetupComplete: Flow<Boolean> = context.securityDataStore.data
        .map { prefs -> prefs[AUTH_SETUP_COMPLETE_KEY] ?: false }

    val isBiometricEnabled: Flow<Boolean> = context.securityDataStore.data
        .map { prefs -> prefs[BIOMETRIC_ENABLED_KEY] ?: false }

    val isApiKeySet: Flow<Boolean> = context.securityDataStore.data
        .map { prefs -> prefs[API_KEY_ENCRYPTED] != null }

    fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun setupPin(pin: String) {
        val pinHash = hashPin(pin)
        context.securityDataStore.edit { prefs ->
            prefs[PIN_HASH_KEY] = pinHash
            prefs[AUTH_SETUP_COMPLETE_KEY] = true
        }
        generateAndStoreDatabaseKey()
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = context.securityDataStore.data.first()[PIN_HASH_KEY] ?: return false
        return hashPin(pin) == storedHash
    }

    suspend fun enableBiometric(enable: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED_KEY] = enable
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Life Coach")
            .setSubtitle("Use your biometric to access the app")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    suspend fun getDatabasePassphrase(): ByteArray {
        val prefs = context.securityDataStore.data.first()
        val ivString = prefs[DB_KEY_IV]
        val encryptedKey = prefs[DB_KEY_ENCRYPTED]

        if (ivString == null || encryptedKey == null) {
            return generateAndStoreDatabaseKey()
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            val encrypted = Base64.decode(encryptedKey, Base64.DEFAULT)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            generateAndStoreDatabaseKey()
        }
    }

    suspend fun setClaudeApiKey(apiKey: String) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(apiKey.toByteArray())
        val iv = cipher.iv

        context.securityDataStore.edit { prefs ->
            prefs[API_KEY_IV] = Base64.encodeToString(iv, Base64.DEFAULT)
            prefs[API_KEY_ENCRYPTED] = Base64.encodeToString(encrypted, Base64.DEFAULT)
        }
    }

    suspend fun getClaudeApiKey(): String? {
        val prefs = context.securityDataStore.data.first()
        val ivString = prefs[API_KEY_IV]
        val encryptedKey = prefs[API_KEY_ENCRYPTED]

        if (ivString == null || encryptedKey == null) {
            return null
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val iv = Base64.decode(ivString, Base64.DEFAULT)
            val encrypted = Base64.decode(encryptedKey, Base64.DEFAULT)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun generateAndStoreDatabaseKey(): ByteArray {
        val dbKey = ByteArray(32).apply {
            java.security.SecureRandom().nextBytes(this)
        }

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(dbKey)
        val iv = cipher.iv

        context.securityDataStore.edit { prefs ->
            prefs[DB_KEY_IV] = Base64.encodeToString(iv, Base64.DEFAULT)
            prefs[DB_KEY_ENCRYPTED] = Base64.encodeToString(encrypted, Base64.DEFAULT)
        }

        return dbKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun clearAllSecurityData() {
        context.securityDataStore.edit { prefs ->
            prefs.clear()
        }
        try {
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (_: Exception) { }
    }
}
