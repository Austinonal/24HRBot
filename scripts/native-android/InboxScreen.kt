package org.openmausbot.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.openmausbot.android.data.ApiClient
import org.openmausbot.android.data.Repo
import org.openmausbot.android.data.currentBaseUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxFilter(val wire: String, val label: String) {
    NEEDS_YOU("needs-you", "รอคุณ"),
    INFO("info", "ข้อมูล"),
    DONE("done", "เสร็จแล้ว"),
}
private data class InboxItem(
    val id: String,
    val kind: String,
    val tab: String,
    val sender: String,
    val via: String,
    val text: String,
    val timestamp: Long,
    val unread: Boolean,
    val botId: String?,
    val threadId: String?,
    val groupId: String?,
    val title: String?,
    val needsReply: Boolean,
)
private fun inboxApi(): ApiClient? {
    val pc = Repo.computer ?: return null
    return ApiClient(pc.currentBaseUrl(), pc.token)
}
private fun parseInbox(json: JSONObject): List<InboxItem> {
    val rows = json.optJSONArray("rows") ?: return emptyList()
    return buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            val from = row.optJSONObject("from")
            val via = row.optJSONObject("via")
            val origin = row.optJSONObject("origin")
            add(
                InboxItem(
                    id = id,
                    kind = row.optString("kind", "message"),
                    tab = row.optString("tab", "info"),
                    sender = from?.optString("name")?.takeIf { it.isNotBlank() } ?: "OpenMausBot",
                    via = via?.optString("name").orEmpty(),
                    text = row.optString("text"),
                    timestamp = row.optLong("at"),
                    unread = row.optBoolean("unread"),
                    botId = origin?.optString("botId")?.takeIf { it.isNotBlank() },
                    threadId = origin?.optString("threadId")?.takeIf { it.isNotBlank() },
                    groupId = origin?.optString("groupId")?.takeIf { it.isNotBlank() },
                    title = origin?.optString("title")?.takeIf { it.isNotBlank() },
                    needsReply = row.optBoolean("replies") || row.optJSONObject("card") != null ||
                        row.optJSONObject("secret") != null || row.optJSONObject("signIn") != null ||
                        row.optJSONObject("connector") != null,
                )
            )
        }
    }
}
private fun inboxTime(at: Long): String = runCatching {
    SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(at))
}.getOrDefault("")

@Composable
fun InboxScreen(onOpen: (botId: String?, groupId: String?, threadId: String) -> Unit) {
    val scope = rememberCoroutineScope()
    val computer = Repo.computer
    var filter by remember { mutableStateOf(InboxFilter.NEEDS_YOU) }
    var rows by remember { mutableStateOf<List<InboxItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        val api = inboxApi()
        if (api == null) {
            rows = emptyList()
            error = "ต้องเชื่อมต่อ MausBot ก่อน"
            return
        }
        loading = true
        error = null
        try {
            val snapshot = withContext(Dispatchers.IO) { api.get("/api/inbox") }
            rows = parseInbox(snapshot)
        } catch (e: Exception) {
            error = e.message ?: "โหลดกล่องเข้าไม่สำเร็จ"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(computer?.id) { refresh() }

    val filtered = rows.filter { it.tab == filter.wire }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("กล่องเข้า", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("รายการที่รอคุณตอบกลับและผลลัพธ์จาก Bot",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { scope.launch { refresh() } }, enabled = !loading) {
                Icon(Icons.Default.Refresh, "รีเฟรช")
            }
            TextButton(onClick = {
                scope.launch {
                    val api = inboxApi() ?: return@launch
                    try {
                        withContext(Dispatchers.IO) { api.post("/api/inbox/read-all") }
                        refresh()
                    } catch (e: Exception) { error = e.message ?: "ทำเครื่องหมายว่าอ่านแล้วไม่สำเร็จ" }
                }
            }, enabled = rows.any { it.unread }) { Text("อ่านทั้งหมด") }
        }
        TabRow(selectedTabIndex = filter.ordinal, containerColor = MaterialTheme.colorScheme.background) {
            InboxFilter.values().forEach { item ->
                Tab(selected = filter == item, onClick = { filter = item }, text = { Text(item.label) })
            }
        }
        when {
            loading && rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && rows.isEmpty() -> Column(
                Modifier.fillMaxWidth().padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ยังเปิดกล่องเข้าไม่ได้", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(error ?: "", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = { scope.launch { refresh() } }) { Text("ลองอีกครั้ง") }
            }
            filtered.isEmpty() -> EmptyState(
                when (filter) {
                    InboxFilter.NEEDS_YOU -> "ไม่มีรายการที่รอคุณ"
                    InboxFilter.INFO -> "ยังไม่มีข้อมูลใหม่"
                    InboxFilter.DONE -> "ยังไม่มีรายการที่เสร็จแล้ว"
                },
                "เมื่อมีข้อความ คำขออนุมัติ หรือรายงานจาก Bot จะปรากฏที่นี่",
            )
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { row ->
                    InboxCard(
                        row = row,
                        onOpen = {
                            val thread = row.threadId
                            if (thread != null) {
                                onOpen(row.botId, row.groupId, thread)
                            } else {
                                error = "รายการนี้ยังไม่มีลิงก์กลับไปยังบทสนทนาต้นทาง"
                            }
                        },
                        onRead = {
                            scope.launch {
                                val api = inboxApi() ?: return@launch
                                try {
                                    withContext(Dispatchers.IO) { api.post("/api/inbox/" + row.id + "/read") }
                                    refresh()
                                } catch (e: Exception) { error = e.message ?: "ทำเครื่องหมายว่าอ่านแล้วไม่สำเร็จ" }
                            }
                        },
                        onDone = {
                            scope.launch {
                                val api = inboxApi() ?: return@launch
                                try {
                                    withContext(Dispatchers.IO) { api.post("/api/inbox/" + row.id + "/done") }
                                    refresh()
                                } catch (e: Exception) { error = e.message ?: "ปิดรายการไม่สำเร็จ" }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxCard(row: InboxItem, onOpen: () -> Unit, onRead: () -> Unit, onDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.sender, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (row.unread) Text("ใหม่", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(inboxTime(row.timestamp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                when (row.kind) {
                    "ask" -> "คำถาม / คำขออนุมัติ"
                    "result" -> "รายงานจาก Bot"
                    "notice" -> "การแจ้งเตือน"
                    else -> "ข้อความ"
                },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(row.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp),
                maxLines = 5, overflow = TextOverflow.Ellipsis)
            if (row.via.isNotBlank()) Text("ส่งต่อผ่าน " + row.via,
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                if (row.unread) TextButton(onClick = onRead) { Text("อ่านแล้ว") }
                if (!row.needsReply) TextButton(onClick = onDone) { Text("เสร็จแล้ว") }
                TextButton(onClick = onOpen, enabled = row.threadId != null) { Text("เปิดบทสนทนา") }
            }
        }
    }
}
