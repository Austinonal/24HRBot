package org.openmausbot.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.openmausbot.android.data.ApiClient
import org.openmausbot.android.data.Bot
import org.openmausbot.android.data.Repo
import org.openmausbot.android.data.currentBaseUrl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class RoutineItem(val id: String, val name: String, val prompt: String, val enabled: Boolean, val schedule: String, val next: Long?)
private data class RoutineRunItem(val id: String, val name: String, val status: String, val at: Long?, val detail: String)

private fun routinesApi(): ApiClient? {
    val computer = Repo.computer ?: return null
    return ApiClient(computer.currentBaseUrl(), computer.token)
}
private fun scheduleLabel(value: JSONObject?): String = when (value?.optString("type")) {
    "daily" -> "ทุกวัน · " + value.optString("time", "--:--")
    "once" -> "ครั้งเดียว"
    "interval" -> "ทุก " + value.optInt("everyMinutes") + " นาที"
    "cron" -> "ตาราง Cron"
    else -> "กำหนดเวลาเอง"
}
private fun parseRoutines(snapshot: JSONObject): Pair<List<RoutineItem>, List<RoutineRunItem>> {
    val raw = snapshot.optJSONArray("routines") ?: JSONArray()
    val rawRuns = snapshot.optJSONArray("runs") ?: JSONArray()
    val routines = buildList {
        for (i in 0 until raw.length()) {
            val row = raw.optJSONObject(i) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            val next = if (row.isNull("nextRunAt")) null else row.optLong("nextRunAt").takeIf { it > 0 }
            add(RoutineItem(id, row.optString("name", "งาน"), row.optString("prompt"),
                row.optBoolean("enabled", true), scheduleLabel(row.optJSONObject("schedule")), next))
        }
    }
    val runs = buildList {
        for (i in 0 until rawRuns.length()) {
            val row = rawRuns.optJSONObject(i) ?: continue
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: continue
            val at = if (row.isNull("startedAt")) null else row.optLong("startedAt").takeIf { it > 0 }
            add(RoutineRunItem(id, row.optString("routineName", "งาน"), row.optString("status", "unknown"),
                at, row.optString("output").ifBlank { row.optString("error") }))
        }
    }
    return routines to runs
}
private fun statusLabel(status: String) = when (status) {
    "queued" -> "รอคิว"; "running" -> "กำลังทำงาน"; "waiting" -> "รอการตอบกลับ"
    "completed" -> "เสร็จแล้ว"; "failed" -> "ผิดพลาด"; "cancelled" -> "ยกเลิกแล้ว"; "missed" -> "พลาดรอบ"
    else -> status
}
private fun timeLabel(value: Long?): String = if (value == null) "ยังไม่มีกำหนด" else
    runCatching { SimpleDateFormat("d MMM · HH:mm", Locale.getDefault()).format(Date(value)) }.getOrDefault("—")

@Composable
fun RoutinesScreen() {
    val scope = rememberCoroutineScope()
    val computer = Repo.computer
    var routines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var runs by remember { mutableStateOf<List<RoutineRunItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RoutineItem?>(null) }

    suspend fun refresh() {
        val api = routinesApi()
        if (api == null) {
            routines = emptyList(); runs = emptyList()
            error = "ต้องเชื่อมต่อคอมพิวเตอร์ก่อนจึงจะดูงานตามกำหนดได้"
            return
        }
        loading = true; error = null
        try {
            val snapshot = withContext(Dispatchers.IO) { api.get("/api/routines") }
            val parsed = parseRoutines(snapshot)
            routines = parsed.first; runs = parsed.second
        } catch (e: Exception) { error = e.message ?: "โหลดงานไม่สำเร็จ" }
        finally { loading = false }
    }
    LaunchedEffect(computer?.id) { refresh() }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("งานที่ทำซ้ำ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("ตรวจตารางและประวัติการทำงานของ Bot", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = { scope.launch { refresh() } }, enabled = !loading) { Icon(Icons.Default.Refresh, "รีเฟรช") }
            IconButton(onClick = { creating = true }, enabled = Repo.bots.any { !it.hidden }) { Icon(Icons.Default.Add, "เพิ่มงาน") }
        }
        when {
            loading && routines.isEmpty() -> Column(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                Text("กำลังโหลด…", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            routines.isEmpty() -> {
                RoutineEmptyState("ยังไม่มีงานตามกำหนด", error ?: "เพิ่มงานเพื่อให้ Bot ทำงานซ้ำตามตารางที่คุณตั้งไว้")
                if (error != null) TextButton(onClick = { scope.launch { refresh() } }) { Text("ลองโหลดอีกครั้ง") }
            }
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("ROUTINES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(routines, key = { it.id }) { row ->
                    RoutineCard(row,
                        onToggle = { enabled ->
                            scope.launch {
                                val api = routinesApi() ?: return@launch
                                try {
                                    withContext(Dispatchers.IO) { api.patch("/api/routines/" + row.id, JSONObject().put("enabled", enabled)) }
                                    refresh()
                                } catch (e: Exception) { error = e.message ?: "เปลี่ยนสถานะไม่สำเร็จ" }
                            }
                        },
                        onRun = {
                            scope.launch {
                                val api = routinesApi() ?: return@launch
                                try { withContext(Dispatchers.IO) { api.post("/api/routines/" + row.id + "/run") }; refresh() }
                                catch (e: Exception) { error = e.message ?: "เริ่มงานไม่สำเร็จ" }
                            }
                        },
                        onDelete = { deleting = row },
                    )
                }
                if (runs.isNotEmpty()) {
                    item { Text("RUN HISTORY", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)) }
                    items(runs.take(20), key = { "run:" + it.id }) { run -> RunRow(run) }
                }
                if (error != null) item { Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    if (creating) NewRoutineDialog(
        bots = Repo.bots.filterNot { it.hidden },
        onClose = { creating = false },
        onCreate = { name, prompt, botId, time ->
            creating = false
            scope.launch {
                val api = routinesApi()
                if (api == null) { error = "ยังไม่ได้เชื่อมต่อคอมพิวเตอร์"; return@launch }
                try {
                    val schedule = JSONObject().put("type", "daily").put("time", time)
                        .put("weekdays", JSONArray().put(1).put(2).put(3).put(4).put(5))
                    val body = JSONObject().put("name", name).put("prompt", prompt).put("botId", botId)
                        .put("schedule", schedule).put("durationMinutes", 30)
                    withContext(Dispatchers.IO) { api.post("/api/routines", body) }
                    refresh()
                } catch (e: Exception) { error = e.message ?: "สร้างงานไม่สำเร็จ" }
            }
        },
    )
    deleting?.let { row ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("ลบงานนี้หรือไม่?") },
            text = { Text("งาน " + row.name + " จะไม่ทำงานตามกำหนดอีก") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        val api = routinesApi() ?: return@launch
                        try { withContext(Dispatchers.IO) { api.delete("/api/routines/" + row.id) }; refresh() }
                        catch (e: Exception) { error = e.message ?: "ลบงานไม่สำเร็จ" }
                    }
                }) { Text("ลบ") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("ยกเลิก") } },
        )
    }
}

@Composable
private fun RoutineEmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RoutineCard(row: RoutineItem, onToggle: (Boolean) -> Unit, onRun: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(row.schedule, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(row.enabled, onToggle)
            }
            if (row.prompt.isNotBlank()) Text(row.prompt, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp))
            Text("รอบถัดไป: " + timeLabel(row.next), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, null); Text("ลบ") }
                TextButton(onClick = onRun, enabled = row.enabled) { Icon(Icons.Default.PlayArrow, null); Text("รันทันที") }
            }
        }
    }
}

@Composable
private fun RunRow(row: RoutineRunItem) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(statusLabel(row.status), style = MaterialTheme.typography.labelSmall,
                    color = if (row.status == "failed" || row.status == "missed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(timeLabel(row.at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (row.detail.isNotBlank()) Text(row.detail, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
        }
    }
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}

@Composable
private fun NewRoutineDialog(bots: List<Bot>, onClose: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var prompt by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("09:00") }
    var botId by rememberSaveable { mutableStateOf(bots.firstOrNull()?.id.orEmpty()) }
    var menu by remember { mutableStateOf(false) }
    val chosen = bots.firstOrNull { it.id == botId } ?: bots.firstOrNull()
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("สร้างงานตามกำหนด") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(name, { name = it.take(60) }, label = { Text("ชื่องาน") }, singleLine = true)
                OutlinedTextField(prompt, { prompt = it.take(3000) }, label = { Text("คำสั่งที่ให้ Bot ทำ") }, minLines = 3)
                OutlinedTextField(time, { time = it.take(5) }, label = { Text("เวลา (HH:mm)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bot", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { menu = true }, enabled = bots.isNotEmpty()) { Text(chosen?.name ?: "ไม่มี Bot") }
                    DropdownMenu(menu, { menu = false }) {
                        bots.forEach { bot -> DropdownMenuItem(text = { Text(bot.name) }, onClick = { botId = bot.id; menu = false }) }
                    }
                }
                Text("ตั้งให้ทำงานวันจันทร์–ศุกร์ ตามเวลาที่ระบุ", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && prompt.isNotBlank() && chosen != null && Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(time),
                onClick = { chosen?.let { onCreate(name.trim(), prompt.trim(), it.id, time) } },
            ) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("ยกเลิก") } },
    )
}
