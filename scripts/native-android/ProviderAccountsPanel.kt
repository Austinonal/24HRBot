package org.openmausbot.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class MobileProviderAccount(
    val instanceId: String,
    val driverKind: String,
    val displayName: String,
    val installed: Boolean,
    val authenticated: Boolean,
    val authMethod: String,
    val email: String,
)

@Composable
fun ProviderAccountsPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<MobileProviderAccount>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var flowId by remember { mutableStateOf<String?>(null) }
    var userCode by remember { mutableStateOf<String?>(null) }
    var authorizationUrl by remember { mutableStateOf<String?>(null) }
    var pollJob by remember { mutableStateOf<Job?>(null) }

    suspend fun refreshAccounts() {
        val parsed = withContext(Dispatchers.IO) {
            val response = ApiAccess.get("/api/instances")
            val rows = response.optJSONArray("instances") ?: return@withContext emptyList()
            buildList {
                for (index in 0 until rows.length()) {
                    val row = rows.optJSONObject(index) ?: continue
                    val snapshot = row.optJSONObject("snapshot") ?: JSONObject()
                    val authentication = row.optJSONObject("authentication") ?: JSONObject()
                    val account = snapshot.optJSONObject("account") ?: JSONObject()
                    add(
                        MobileProviderAccount(
                            instanceId = row.optString("instanceId"),
                            driverKind = row.optString("driverKind"),
                            displayName = row.optString("displayName"),
                            installed = snapshot.optString("state") == "available",
                            authenticated = snapshot.optBoolean("authenticated"),
                            authMethod = authentication.optString("method"),
                            email = account.optString("email"),
                        ),
                    )
                }
            }
        }
        accounts = parsed
        loading = false
    }

    LaunchedEffect(Unit) {
        try {
            refreshAccounts()
            error = null
        } catch (cause: Exception) {
            error = cause.message ?: "อ่านสถานะบัญชีจากคอมพิวเตอร์ไม่สำเร็จ"
            loading = false
        }
    }

    val codex = accounts.firstOrNull {
        it.instanceId == "codex" && it.driverKind == "codex"
    }
    val google = accounts.firstOrNull { it.driverKind == "antigravityAgent" }
    val geminiCli = accounts.firstOrNull { it.driverKind == "geminiAgent" }

    fun openProviderPage(url: String) {
        try {
            if (!url.startsWith("https://auth.openai.com/codex/device")) {
                throw IllegalArgumentException("ลิงก์ยืนยันบัญชีไม่ตรงกับหน้าล็อกอินที่รองรับ")
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (cause: Exception) {
            error = cause.message ?: "เปิดหน้าเข้าสู่ระบบไม่ได้"
        }
    }

    fun startChatGptSignIn() {
        if (busy || pollJob?.isActive == true) return
        busy = true
        error = null
        info = null
        scope.launch {
            try {
                val start = withContext(Dispatchers.IO) {
                    ApiAccess.post("/api/instances/codex/auth/start", JSONObject())
                        .optJSONObject("auth") ?: throw IllegalStateException("คอมพิวเตอร์ไม่ได้ส่งข้อมูลเริ่มเข้าสู่ระบบกลับมา")
                }
                if (start.optString("phase") == "succeeded") {
                    info = "บัญชี ChatGPT เชื่อมต่ออยู่แล้ว"
                    refreshAccounts()
                    return@launch
                }
                val nextFlowId = start.optString("flowId").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("ไม่มีรหัสขั้นตอนเข้าสู่ระบบ")
                val nextCode = start.optString("userCode").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Codex ไม่ได้ส่งรหัสยืนยันกลับมา ตรวจสอบการติดตั้งบนคอมพิวเตอร์")
                val nextUrl = start.optString("authorizationUrl").takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("ไม่มีลิงก์ยืนยันบัญชี")
                flowId = nextFlowId
                userCode = nextCode
                authorizationUrl = nextUrl
                openProviderPage(nextUrl)
                busy = false
                pollJob?.cancel()
                pollJob = scope.launch {
                    try {
                        for (attempt in 0 until 450) {
                            delay(2_000)
                            val status = withContext(Dispatchers.IO) {
                                ApiAccess.get(
                                    "/api/instances/codex/auth/status?flowId=" +
                                        Uri.encode(nextFlowId),
                                ).optJSONObject("auth")
                            } ?: throw IllegalStateException("อ่านสถานะการยืนยันบัญชีไม่ได้")
                            val phase = status.optString("phase")
                            if (phase != "waiting") {
                                val message = status.optString("message").takeIf { it.isNotBlank() }
                                flowId = null
                                userCode = null
                                authorizationUrl = null
                                if (phase == "succeeded") {
                                    info = "เชื่อมต่อบัญชี ChatGPT แล้ว"
                                    error = null
                                    refreshAccounts()
                                } else {
                                    info = null
                                    error = message ?: "การเข้าสู่ระบบไม่สำเร็จ กรุณาเริ่มใหม่"
                                }
                                return@launch
                            }
                        }
                        error = "รหัสยืนยันหมดเวลารอแล้ว กรุณาเริ่มใหม่"
                        flowId = null
                        userCode = null
                        authorizationUrl = null
                    } catch (cause: Exception) {
                        error = cause.message ?: "ตรวจสอบสถานะการเข้าสู่ระบบไม่ได้"
                        flowId = null
                        userCode = null
                        authorizationUrl = null
                    }
                }
            } catch (cause: Exception) {
                error = cause.message ?: "เริ่มเข้าสู่ระบบ ChatGPT ไม่สำเร็จ"
            } finally {
                busy = false
            }
        }
    }

    fun cancelChatGptSignIn() {
        val currentFlow = flowId ?: return
        pollJob?.cancel()
        pollJob = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiAccess.post(
                        "/api/instances/codex/auth/cancel",
                        JSONObject().put("flowId", currentFlow),
                    )
                }
                info = "ยกเลิกการเข้าสู่ระบบแล้ว"
                error = null
            } catch (cause: Exception) {
                error = cause.message ?: "ยกเลิกขั้นตอนเข้าสู่ระบบไม่ได้"
            } finally {
                flowId = null
                userCode = null
                authorizationUrl = null
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccountCard(
            title = "ChatGPT",
            subtitle = when {
                codex?.authenticated == true -> "เชื่อมต่อแล้ว" + codex.email.takeIf { it.isNotBlank() }?.let { " · " + it }.orEmpty()
                codex?.installed == true && codex.authMethod == "device-code" -> "ใช้บัญชี ChatGPT ผ่าน Codex"
                codex == null -> "ไม่พบ Codex บนคอมพิวเตอร์ที่จับคู่"
                else -> "ต้องติดตั้งหรือเตรียม Codex บนคอมพิวเตอร์ก่อน"
            },
            connected = codex?.authenticated == true,
        ) {
            when {
                codex?.authenticated == true -> Text(
                    "บัญชี ChatGPT พร้อมใช้งานผ่าน Codex",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                codex?.installed == true && codex.authMethod == "device-code" -> {
                    if (flowId != null && userCode != null) {
                        Text(
                            userCode!!,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT sign-in code", userCode))
                                info = "คัดลอกรหัสแล้ว"
                            }) { Text("คัดลอกรหัส") }
                            OutlinedButton(onClick = {
                                authorizationUrl?.let(::openProviderPage)
                            }) { Text("เปิดหน้าอนุญาต") }
                        }
                        TextButton(onClick = ::cancelChatGptSignIn) { Text("ยกเลิก") }
                    } else {
                        OutlinedButton(onClick = ::startChatGptSignIn, enabled = !busy) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("เข้าสู่ระบบด้วยบัญชี ChatGPT")
                            }
                        }
                    }
                }
                else -> Text(
                    "เปิด OpenMausBot บนคอมพิวเตอร์และติดตั้ง/ตั้งค่า Codex ก่อน จากนั้นกลับมารีเฟรชสถานะ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AccountCard(
            title = "Google · Gemini",
            subtitle = when {
                google?.authenticated == true -> "เชื่อมต่อ Google ผ่าน Antigravity แล้ว"
                google != null -> "ต้องอนุญาต Google บนคอมพิวเตอร์"
                geminiCli?.authenticated == true -> "Gemini CLI เชื่อมต่อแล้ว"
                geminiCli != null -> "Gemini CLI ใช้การตั้งค่า API บน Host"
                else -> "ยังไม่พบ Google/Gemini account บน Host"
            },
            connected = google?.authenticated == true || geminiCli?.authenticated == true,
        ) {
            Text(
                "การเข้าสู่ระบบ Google เพื่อใช้ Gemini ผ่าน Antigravity ต้องเปิดหน้าอนุญาตในเบราว์เซอร์บนคอมพิวเตอร์ที่รัน OpenMausBot ไม่สามารถจบ OAuth ผ่าน Companion ได้",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Gemini CLI ใช้ API key ที่ตั้งค่าไว้บน Host ไม่ใช่การล็อกอิน Google ในมือถือ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AccountCard(
            title = "Claude",
            subtitle = "ไม่ได้เปิดขั้นตอนล็อกอินใน Companion นี้",
            connected = false,
        ) {
            Text(
                "ให้ใช้บัญชีตามข้อกำหนดอายุและสิทธิ์ของผู้ให้บริการโดยตรง เราจะไม่ใช้ช่องติ๊กยืนยันอายุแทนการตรวจสอบคุณสมบัติจริง",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = !loading,
                onClick = {
                    loading = true
                    scope.launch {
                        try {
                            refreshAccounts()
                            error = null
                        } catch (cause: Exception) {
                            error = cause.message ?: "รีเฟรชสถานะบัญชีไม่สำเร็จ"
                        } finally {
                            loading = false
                        }
                    }
                },
            ) {
                if (loading) CircularProgressIndicator(Modifier.height(14.dp), strokeWidth = 2.dp)
                else Text("รีเฟรชสถานะบัญชี")
            }
        }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text(
            "รหัสยืนยันเป็นรหัสชั่วคราว ใช้บนหน้า OpenAI ที่เปิดจากปุ่มด้านบนเท่านั้น อย่าส่งรหัสให้ผู้อื่น",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountCard(
    title: String,
    subtitle: String,
    connected: Boolean,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
