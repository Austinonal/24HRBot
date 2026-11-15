package org.openmausbot.android.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Saved pairing trust. Device bearer tokens are AES/GCM encrypted with a key
 * held by Android Keystore. The encrypted value remains in app-private
 * SharedPreferences and is excluded from Android backup by the manifest.
 */
class DeviceStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("openmausbot", Context.MODE_PRIVATE)

    var computers: List<PairedComputer>
        get() = readComputers()
        private set(value) = writeComputers(value)

    var selectedId: String?
        get() = prefs.getString(KEY_SELECTED, null)
        set(value) {
            prefs.edit().putString(KEY_SELECTED, value).apply()
        }

    var notifyWhileClosed: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_NOTIFY, value).apply()
        }

    var speakReplies: Boolean
        get() = prefs.getBoolean(KEY_SPEAK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SPEAK, value).apply()
        }

    var darkTheme: Boolean?
        get() = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, true) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_DARK) else putBoolean(KEY_DARK, value)
            }.apply()
        }

    fun upsert(computer: PairedComputer) {
        val next = readComputers().filterNot { it.id == computer.id } + computer
        writeComputers(next)
    }

    fun remove(id: String) {
        writeComputers(readComputers().filterNot { it.id == id })
        if (selectedId == id) selectedId = null
    }

    fun cursor(id: String, cursor: String?) {
        val current = readComputers().firstOrNull { it.id == id } ?: return
        writeComputers(
            readComputers().map { if (it.id == id) current.copy(lastCursor = cursor) else it }
        )
    }

    private fun readComputers(): List<PairedComputer> {
        val stored = prefs.getString(KEY_COMPUTERS, null) ?: return emptyList()
        val raw = runCatching { decryptIfNeeded(stored) }.getOrNull() ?: return emptyList()
        val parsed = runCatching {
            JSONArray(raw).let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val json = array.optJSONObject(index) ?: return@mapNotNull null
                    val base = json.optString("baseUrl").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val token = json.optString("token").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val legacyEndpoints = json.optJSONArray("endpoints")?.let { endpoints ->
                        (0 until endpoints.length()).mapNotNull { i ->
                            when (val item = endpoints.opt(i)) {
                                is JSONObject -> item.optString("url").takeIf { it.isNotBlank() }
                                is String -> item.takeIf { it.isNotBlank() }
                                else -> null
                            }
                        }
                    } ?: emptyList()
                    val routes = json.optJSONArray("routes")?.let { array ->
                        (0 until array.length()).mapNotNull { i ->
                            array.optJSONObject(i)?.let(CompanionEndpoint::fromJson)
                        }
                    } ?: emptyList()
                    val active = json.optJSONObject("activeEndpoint")?.let(CompanionEndpoint::fromJson)
                    val kinds = json.optJSONArray("allowedRouteKinds")?.let { array ->
                        (0 until array.length())
                            .mapNotNull { i -> EndpointKind.fromWire(array.optString(i)) }
                            .toSet()
                    }
                    val localUrls = json.optJSONArray("allowedLocalRouteUrls")?.let { array ->
                        (0 until array.length()).mapNotNull { i ->
                            array.optString(i).takeIf { it.isNotBlank() }
                        }.toSet()
                    }
                    PairedComputer(
                        id = json.optString("id", base),
                        serverName = json.optString("serverName", PairingInvite.hostOf(base)),
                        baseUrl = base,
                        token = token,
                        endpoints = legacyEndpoints,
                        lastCursor = json.optString("lastCursor").takeIf { it.isNotBlank() },
                        lastUsedAt = json.optLong("lastUsedAt"),
                        hosts = json.optJSONArray("hosts")?.let { hosts ->
                            (0 until hosts.length()).mapNotNull { i ->
                                hosts.optString(i).takeIf { it.isNotBlank() }
                            }
                        } ?: emptyList(),
                        endpointRoutes = routes,
                        activeEndpoint = active ?: routes.firstOrNull(),
                        allowedRouteKinds = kinds,
                        allowedLocalRouteUrls = localUrls,
                        secretPublicKey = json.optString("secretPublicKey").takeIf { it.isNotBlank() },
                        companionDeviceId = json.optString("companionDeviceId").takeIf { it.isNotBlank() },
                    )
                }
            }
        }.getOrDefault(emptyList())

        // One-time migration: old app versions stored the JSON in plaintext.
        // Rewrite only after a valid parse; never fall back to plaintext writes.
        if (!stored.startsWith(ENCRYPTED_PREFIX) && parsed.isNotEmpty()) {
            runCatching { writeComputers(parsed) }
        }
        return parsed
    }

    private fun writeComputers(computers: List<PairedComputer>) {
        val array = JSONArray()
        computers.forEach { computer ->
            array.put(
                JSONObject().apply {
                    put("id", computer.id)
                    put("serverName", computer.serverName)
                    put("baseUrl", computer.baseUrl)
                    put("token", computer.token)
                    put("endpoints", JSONArray(computer.endpoints))
                    put("hosts", JSONArray(computer.hosts))
                    put("routes", JSONArray().apply {
                        computer.endpointRoutes.forEach { put(it.toJson()) }
                    })
                    computer.activeEndpoint?.let { put("activeEndpoint", it.toJson()) }
                    computer.allowedRouteKinds?.let { kinds ->
                        put("allowedRouteKinds", JSONArray(kinds.map { it.wire }))
                    }
                    computer.allowedLocalRouteUrls?.let { urls ->
                        put("allowedLocalRouteUrls", JSONArray(urls.toList()))
                    }
                    computer.secretPublicKey?.let { put("secretPublicKey", it) }
                    computer.companionDeviceId?.let { put("companionDeviceId", it) }
                    put("lastCursor", computer.lastCursor ?: "")
                    put("lastUsedAt", computer.lastUsedAt)
                }
            )
        }
        val encrypted = encrypt(array.toString())
        prefs.edit().putString(KEY_COMPUTERS, encrypted).apply()
    }

    private fun decryptIfNeeded(value: String): String {
        if (!value.startsWith(ENCRYPTED_PREFIX)) return value
        val encoded = value.removePrefix(ENCRYPTED_PREFIX)
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > GCM_IV_BYTES) { "Encrypted device store is invalid" }
        val iv = combined.copyOfRange(0, GCM_IV_BYTES)
        val payload = combined.copyOfRange(GCM_IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(payload), Charsets.UTF_8)
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return ENCRYPTED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun encryptionKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_COMPUTERS = "computers"
        const val KEY_SELECTED = "selectedComputer"
        const val KEY_NOTIFY = "notifyWhileClosed"
        const val KEY_SPEAK = "speakReplies"
        const val KEY_DARK = "darkTheme"
        const val KEY_ALIAS = "org.openmausbot.android.device-store.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ENCRYPTED_PREFIX = "enc:v1:"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
