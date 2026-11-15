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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.openmausbot.android.data.Repo
import java.net.URLEncoder

private data class MobileProviderAccount(
    val instanceId: String,
    val driverKind: String,
    val displayName: String,
    val authenticated: Boolean,
    val email: String?,
    val authMethod: String?,
)

private fun parseMobileProviderAccounts(answer: JSONObject): List<MobileProviderAccount> {
    val rows = answer.optJSONArray("instances") ?: answer.optJSONArray("engines") ?: return emptyList()
    return buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val id = row.optString("instanceId").takeIf { it.isNotBlank() } ?: continue
            val snapshot = row.optJSONObject("snapshot") ?: JSONObject()
            val account = snapshot.optJSONObject("account") ?: JSONObject()
            val authentication = row.optJSONObject("authentication") ?: JSONObject()
            add(
                MobileProviderAccount(
                    instanceId = id,
                    driverKind = row.optString("driverKind", row.optString("driver")),
                    displayName = row.optString("displayName", id).ifBlank { id },
                    authenticated = snapshot.optBoolean("authenticated", false),
                    email = account.optString("email").takeIf { it.isNotBlank() && it != "null" },
                    authMethod = authentication.optString("method").takeIf { it.isNotBlank() },
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<MobileProviderAccount>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var flowId by remember { mutableStateOf<String?>(null) }
    var authorizationUrl by remember { mutableStateOf<String?>(null) }
    var userCode by remember { mutableStateOf<String?>(null) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refreshAccounts() {
        val response = withContext(Dispatchers.IO) { ApiAccess.get("/api/instances") }
        accounts = parseMobileProviderAccounts(response)
    }

    LaunchedEffect(Unit) {
        try {
            refreshAccounts()
            error = null
        } catch (cause: Exception) {
            error = cause.message ?: "อ่านสถานะบัญชีจากคอมพิวเตอร์ไม่สำเร็จ"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(flowId) {
        val currentFlow = flowId ?: return@LaunchedEffect
        while (true) {
            delay(2_000)
            try {
                val encoded = URLEncoder.encode(currentFlow, Charsets.UTF_8.name())
                val response = withContext(Dispatchers.IO) {
                    ApiAccess.get("/api/instances/codex/auth/status?flowId=$encoded")
                }
                val auth = response.optJSONObject("auth") ?: JSONObject()
                when (auth.optString("phase")) {
                    "waiting" -> {
                        authMessage = "รอการอนุญาตจากหน้า OpenAI…"
                    }
                    "succeeded" -> {
                        authMessage = "เชื่อมต่อ ChatGPT สำเร็จ"
                        flowId = null
                        authorizationUrl = null
                        userCode = null
                        refreshAccounts()
                        break
                    }
                    else -> {
                        authMessage = auth.optString("message").takeIf { it.isNotBlank() }
                            ?: "การเข้าสู่ระบบไม่สำเร็จ โปรดลองเริ่มใหม่"
                        flowId = null
                        authorizationUrl = null
                        userCode = null
                        refreshAccounts()
                        break
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (cause: Exception) {
                authMessage = cause.message ?: "ตรวจสอบสถานะการเข้าสู่ระบบไม่ได้"
                flowId = null
                authorizationUrl = null
                userCode = null
                break
            }
        }
    }

    val codex = accounts.firstOrNull { it.instanceId == "codex" }
    val hasCodexDeviceLogin = codex?.authMethod == "device-code"

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "กลับ")
                    }
                },
                title = { Text("บัญชี AI", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        enabled = !refreshing && flowId == null,
                        onClick = {
                            scope.launch {
                                refreshing = true
                                try {
                                    refreshAccounts()
                                    error = null
                                } catch (cause: Exception) {
                                    error = cause.message ?: "รีเฟรชข้อมูลไม่สำเร็จ"
                                } finally {
                                    refreshing = false
                                }
                            }
                        },
                    ) { Text(if (refreshing) "กำลังรีเฟรช…" else "รีเฟรช") }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "จัดการบัญชีที่ OpenMausBot บนคอมพิวเตอร์เชื่อมต่อไว้",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (loading) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("กำลังอ่านบัญชี…", modifier = Modifier.padding(start = 12.dp))
                }
            } else {
                AccountCard(
                    title = "ChatGPT",
                    subtitle = "เชื่อมต่อด้วยบัญชี ChatGPT ผ่าน Codex device sign-in",
                    connected = codex?.authenticated == true,
                    detail = codex?.email ?: if (codex == null) {
                        "ยังไม่พบ Codex บนคอมพิวเตอร์ที่จับคู่ไว้"
                    } else if (codex.authenticated) {
                        "เชื่อมต่อแล้ว"
                    } else {
                        "ยังไม่ได้เชื่อมต่อบัญชี"
                    },
                ) {
                    when {
                        flowId != null -> {
                            Text("เปิดหน้าอนุญาตแล้ว ใส่รหัสด้านล่างเพื่อเชื่อมต่อบัญชี", style = MaterialTheme.typography.bodySmall)
                            userCode?.let { code ->
                                Text(
                                    code,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("ChatGPT sign-in code", code))
                                            authMessage = "คัดลอกรหัสแล้ว"
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, null)
                                        Text("คัดลอกรหัส", modifier = Modifier.padding(start = 6.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            authorizationUrl?.let { url ->
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Default.OpenInNew, null)
                                        Text("เปิดหน้าอนุญาต", modifier = Modifier.padding(start = 6.dp))
                                    }
                                }
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                ApiAccess.post(
                                                    "/api/instances/codex/auth/cancel",
                                                    JSONObject().put("flowId", flowId),
                                                )
                                            }
                                        }
                                        flowId = null
                                        userCode = null
                                        authorizationUrl = null
                                        authMessage = "ยกเลิกการเชื่อมต่อแล้ว"
                                    }
                                },
                            ) { Text("ยกเลิก") }
                        }
                        starting -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                                Text("กำลังเริ่มการเข้าสู่ระบบ…", modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                        !codex?.authenticated.orFalse() && hasCodexDeviceLogin -> {
                            Button(
                                onClick = {
                                    scope.launch {
                                        starting = true
                                        error = null
                                        authMessage = null
                                        try {
                                            val response = withContext(Dispatchers.IO) {
                                                ApiAccess.post("/api/instances/codex/auth/start", JSONObject())
                                            }
                                            val auth = response.optJSONObject("auth") ?: JSONObject()
                                            if (auth.optString("phase") == "succeeded") {
                                                authMessage = "บัญชี ChatGPT เชื่อมต่ออยู่แล้ว"
                                                refreshAccounts()
                                            } else {
                                                val id = auth.optString("flowId")
                                                val code = auth.optString("userCode")
                                                val url = auth.optString("authorizationUrl")
                                                if (id.isBlank() || !code.matches(Regex("^[A-Z0-9]{4,8}-[A-Z0-9]{4,8}$")) ||
                                                    url != "https://auth.openai.com/codex/device"
                                                ) {
                                                    throw IllegalStateException("เซิร์ฟเวอร์ส่งข้อมูลยืนยันตัวตนที่ไม่ตรงกับรูปแบบของ OpenAI")
                                                }
                                                flowId = id
                                                userCode = code
                                                authorizationUrl = url
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        } catch (cause: Exception) {
                                            error = cause.message ?: "เริ่มการเข้าสู่ระบบ ChatGPT ไม่สำเร็จ"
                                        } finally {
                                            starting = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("เข้าสู่ระบบ ChatGPT")
                            }
                        }
                        codex == null -> {
                            Text(
                                "เพิ่มหรือเตรียม Codex engine จาก OpenMausBot บนคอมพิวเตอร์ก่อน แล้วกลับมารีเฟรชหน้านี้",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        !hasCodexDeviceLogin && !codex.authenticated -> {
                            Text(
                                "บัญชีนี้ใช้วิธีเข้าสู่ระบบที่ต้องเริ่มบนคอมพิวเตอร์ จึงไม่สามารถเริ่มจากโทรศัพท์ได้",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    authMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                AccountCard(
                    title = "Google · Gemini",
                    subtitle = "ข้อจำกัดการเข้าสู่ระบบด้วยบัญชี Google",
                    connected = false,
                    detail = "ไม่เปิด Google Antigravity sign-in ใน Companion นี้",
                ) {
                    Text(
                        "OpenMausBot ใช้ Google Antigravity เป็นทางล็อกอินบัญชี Google สำหรับ Gemini ใน Host แต่ Google ระบุว่า Antigravity ไม่พร้อมให้ผู้มีอายุต่ำกว่า 18 ปีใช้งาน จึงไม่มีปุ่มเข้าสู่ระบบหรือช่องติ๊กยืนยันอายุเพื่อข้ามข้อกำหนด",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AccountCard(
                    title = "Claude",
                    subtitle = "ข้อมูลข้อกำหนดบัญชี",
                    connected = false,
                    detail = "ไม่ได้เปิดการเข้าสู่ระบบ Claude ใน Companion นี้",
                ) {
                    Text(
                        "บริการ Claude สำหรับผู้ใช้ทั่วไปกำหนดอายุขั้นต่ำ 18 ปี การติ๊กยืนยันอายุในแอปอื่นไม่ใช่การตรวจสอบอายุจริง หน้านี้จึงไม่มีปุ่มหรือช่องยืนยันเพื่อข้ามข้อกำหนดดังกล่าว",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(
                "ความปลอดภัย: การอนุญาตเกิดกับผู้ให้บริการโดยตรง รหัสชั่วคราวใช้ได้ครั้งเดียว และแอปมือถือจะไม่รับหรือบันทึกรหัสผ่าน, API key หรือ session cookie",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun Boolean?.orFalse(): Boolean = this == true

@Composable
private fun AccountCard(
    title: String,
    subtitle: String,
    connected: Boolean,
    detail: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (connected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "เชื่อมต่อแล้ว", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            content()
        }
    }
}
