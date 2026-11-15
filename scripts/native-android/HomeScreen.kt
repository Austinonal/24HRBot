package org.openmausbot.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import org.openmausbot.android.R
import org.openmausbot.android.data.Bot
import org.openmausbot.android.data.ConnectionState
import org.openmausbot.android.data.Group
import org.openmausbot.android.data.Repo

private enum class MainPage { CHATS, INBOX, ROUTINES, SEARCH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selection by remember { mutableStateOf<Selection?>(null) }
    var page by rememberSaveable { mutableStateOf(MainPage.CHATS) }
    var settings by remember { mutableStateOf(false) }
    var accountCenter by remember { mutableStateOf(false) }
    var newBot by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { if (Repo.computer == null) Repo.connect() }
    LaunchedEffect(Repo.lastError) {
        val value = Repo.lastError ?: return@LaunchedEffect
        snack.showSnackbar(value, actionLabel = "ปิด", withDismissAction = true)
        Repo.clearError()
    }
    BackHandler(selection != null) { selection = null }
    BackHandler(accountCenter) { accountCenter = false }
    BackHandler(selection == null && drawer.isOpen) { scope.launch { drawer.close() } }

    if (settings) { SettingsScreen(onClose = { settings = false }); return }
    if (accountCenter) { AccountsScreen(onClose = { accountCenter = false }); return }
    selection?.let { current ->
        ChatScreen(
            selection = current,
            onBack = { selection = null },
            onOpenSettings = { settings = true },
            onChangeThread = { threadId ->
                selection = when (val target = selection) {
                    is Selection.BotTarget -> target.copy(threadId = threadId)
                    is Selection.GroupTarget -> target.copy(threadId = threadId)
                    null -> null
                }
                Repo.openThread(threadId)
            },
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.mausbot_logo), "OpenMausBot",
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("OpenMausBot", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(connectionLabel(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Text("พื้นที่ทำงาน", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp))
                DrawerItem("แชตและทีม Bot", page == MainPage.CHATS, "C") {
                    page = MainPage.CHATS; scope.launch { drawer.close() }
                }
                DrawerItem("กล่องเข้า", page == MainPage.INBOX, "!") {
                    page = MainPage.INBOX; scope.launch { drawer.close() }
                }
                DrawerItem("งานตามกำหนด", page == MainPage.ROUTINES, "R") {
                    page = MainPage.ROUTINES; scope.launch { drawer.close() }
                }
                DrawerItem("ค้นหา", page == MainPage.SEARCH, "⌕") {
                    page = MainPage.SEARCH; scope.launch { drawer.close() }
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                DrawerItem("บัญชี AI", false, "A") {
                    scope.launch { drawer.close() }; accountCenter = true
                }
                DrawerItem("ตั้งค่า", false, "⚙") {
                    scope.launch { drawer.close() }; settings = true
                }
                Text("การเข้าสู่ระบบและสถานะบัญชี AI",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Default.Menu, "เปิดเมนู") }
                    },
                    title = {
                        Column {
                            Text(when (page) {
                                MainPage.CHATS -> "แชต"
                                MainPage.INBOX -> "กล่องเข้า"
                                MainPage.ROUTINES -> "งานตามกำหนด"
                                MainPage.SEARCH -> "ค้นหา"
                            }, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            Text(connectionLabel(), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { runCatching { Repo.refreshAll() } } }) {
                            Icon(Icons.Default.Refresh, "รีเฟรช")
                        }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "เมนู") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("ตั้งค่า") }, onClick = { menu = false; settings = true })
                            DropdownMenuItem(text = { Text("เชื่อมต่ออีกครั้ง") }, onClick = { menu = false; Repo.connect() })
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(page == MainPage.CHATS, { page = MainPage.CHATS },
                        icon = { Text("C", style = MaterialTheme.typography.titleMedium) }, label = { Text("แชต") })
                    NavigationBarItem(page == MainPage.INBOX, { page = MainPage.INBOX },
                        icon = { Text("!", style = MaterialTheme.typography.titleMedium) }, label = { Text("กล่อง") })
                    NavigationBarItem(page == MainPage.ROUTINES, { page = MainPage.ROUTINES },
                        icon = { Text("R", style = MaterialTheme.typography.titleMedium) }, label = { Text("งาน") })
                    NavigationBarItem(page == MainPage.SEARCH, { page = MainPage.SEARCH },
                        icon = { Icon(Icons.Default.Search, null) }, label = { Text("ค้นหา") })
                }
            },
            floatingActionButton = {
                if (page == MainPage.CHATS) FloatingActionButton(onClick = { newBot = true }) {
                    Icon(Icons.Default.Add, "สร้าง Bot ใหม่")
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    MainPage.CHATS -> ChatsPage(
                        onBot = { bot -> selection = Selection.BotTarget(bot); Repo.openThread(bot.activeTask?.threadId ?: bot.threadId) },
                        onGroup = { group -> selection = Selection.GroupTarget(group); Repo.openThread(group.threadId) },
                    )
                    MainPage.INBOX -> InboxScreen { botId, groupId, threadId ->
                        if (groupId != null) {
                            Repo.groups.firstOrNull { it.id == groupId }?.let { group ->
                                selection = Selection.GroupTarget(group)
                                Repo.openThread(threadId)
                            }
                        } else {
                            Repo.bots.firstOrNull { it.id == botId }?.let { bot ->
                                selection = Selection.BotTarget(bot)
                                Repo.openThread(threadId)
                            }
                        }
                    }
                    MainPage.ROUTINES -> RoutinesScreen()
                    MainPage.SEARCH -> SearchPage(
                        onBot = { bot, threadId ->
                            selection = Selection.BotTarget(bot, threadId)
                            Repo.openThread(threadId)
                        },
                        onGroup = { group, threadId ->
                            selection = Selection.GroupTarget(group, threadId)
                            Repo.openThread(threadId)
                        },
                    )
                }
            }
        }
    }

    if (newBot) NewBotDialog(
        onDismiss = { newBot = false },
        onCreate = { name ->
            newBot = false
            if (Repo.createBot(name) != null) scope.launch { Repo.refreshAll() }
        },
    )
}

@Composable
private fun DrawerItem(title: String, selected: Boolean, glyph: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(title) }, selected = selected, onClick = onClick,
        icon = { Text(glyph, style = MaterialTheme.typography.titleMedium) }, modifier = Modifier.padding(horizontal = 10.dp),
    )
}

private fun connectionLabel(): String = when (Repo.connection.phase) {
    ConnectionState.Phase.ONLINE -> Repo.computer?.serverName?.let { "เชื่อมต่อแล้ว · $it" } ?: "เชื่อมต่อแล้ว"
    ConnectionState.Phase.CONNECTING -> "กำลังเชื่อมต่อ…"
    ConnectionState.Phase.ERROR -> "ยังไม่เชื่อมต่อ"
    ConnectionState.Phase.OFFLINE -> "ออฟไลน์"
    else -> "ยังไม่ได้จับคู่"
}

@Composable
private fun ChatsPage(onBot: (Bot) -> Unit, onGroup: (Group) -> Unit) {
    if (Repo.connection.phase == ConnectionState.Phase.CONNECTING && Repo.bots.isEmpty()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
            Text("กำลังเชื่อมต่อกับคอมพิวเตอร์…", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp))
        }
        return
    }
    if (Repo.connection.phase == ConnectionState.Phase.ERROR && Repo.bots.isEmpty()) {
        ConnectionError(Repo.connection.detail); return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            Text("ทีม Bot ของคุณ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("เลือกผู้ช่วยเพื่อกลับไปทำงานต่อ หรือสร้าง Bot ใหม่", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
        }
        item { SectionHeading("BOTS", Repo.bots.count { !it.hidden }) }
        val visible = Repo.bots.filterNot { it.hidden }
        if (visible.isEmpty()) item { EmptyState("ยังไม่มี Bot", "แตะปุ่ม + เพื่อสร้าง Bot ตัวแรกของคุณ") }
        else items(visible, key = { "bot:" + it.id }) { BotRow(it) { onBot(it) } }
        if (Repo.groups.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)); SectionHeading("GROUP CHATS", Repo.groups.size) }
            items(Repo.groups, key = { "group:" + it.id }) { GroupRow(it) { onGroup(it) } }
        }
    }
}

private data class MobileSearchHit(
    val threadId: String,
    val messageId: String,
    val at: Long,
    val role: String,
    val kind: String,
    val snippet: String,
    val name: String,
    val task: String?,
    val botId: String?,
    val groupId: String?,
    val onActivePath: Boolean,
)

private fun parseMobileSearchHits(answer: org.json.JSONObject): List<MobileSearchHit> {
    val rows = answer.optJSONArray("hits") ?: return emptyList()
    return buildList {
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val threadId = row.optString("threadId").takeIf { it.isNotBlank() } ?: continue
            val messageId = row.optString("messageId").takeIf { it.isNotBlank() } ?: continue
            add(
                MobileSearchHit(
                    threadId = threadId,
                    messageId = messageId,
                    at = row.optLong("at"),
                    role = row.optString("role", "assistant"),
                    kind = row.optString("kind", "text"),
                    snippet = row.optString("snippet"),
                    name = row.optString("name", "OpenMausBot"),
                    task = row.optString("task").takeIf { it.isNotBlank() && it != "null" },
                    botId = row.optString("botId").takeIf { it.isNotBlank() && it != "null" },
                    groupId = row.optString("groupId").takeIf { it.isNotBlank() && it != "null" },
                    onActivePath = row.optBoolean("onActivePath", true),
                )
            )
        }
    }
}

@Composable
private fun SearchPage(
    onBot: (Bot, String) -> Unit,
    onGroup: (Group, String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<MobileSearchHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val term = query.trim()
    val computerId = Repo.computer?.id

    LaunchedEffect(term, computerId) {
        if (term.length < 2) {
            hits = emptyList()
            searched = false
            error = null
            loading = false
            return@LaunchedEffect
        }
        if (computerId == null) {
            hits = emptyList()
            searched = true
            error = "เชื่อมต่อคอมพิวเตอร์ก่อนจึงจะค้นหาประวัติแชตได้"
            return@LaunchedEffect
        }
        delay(250)
        loading = true
        error = null
        try {
            val response = withContext(Dispatchers.IO) {
                val encoded = URLEncoder.encode(term, Charsets.UTF_8.name())
                ApiAccess.get("/api/search?q=$encoded&limit=40")
            }
            hits = parseMobileSearchHits(response)
            searched = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            hits = emptyList()
            searched = true
            error = e.message ?: "ค้นหาข้อความไม่สำเร็จ"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("ค้นหาบทสนทนา", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "ค้นหาข้อความในแชตของ Bot และห้องที่คุณมีสิทธิ์เข้าถึง",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("ค้นหาคำหรือวลีในข้อความ") },
            shape = RoundedCornerShape(24.dp),
        )
        when {
            loading -> Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("กำลังค้นหา…", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 9.dp))
            }
            error != null -> Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            term.length < 2 -> Text(
                "พิมพ์อย่างน้อย 2 ตัวอักษรเพื่อค้นหาข้อความ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            searched && hits.isEmpty() -> EmptyState("ไม่พบข้อความ", "ลองใช้คำค้นหาอื่น หรือค้นหาด้วยวลีที่สั้นลง")
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(hits, key = { it.threadId + ":" + it.messageId }) { hit ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val bot = hit.botId?.let { id -> Repo.bots.firstOrNull { it.id == id && !it.hidden } }
                            val group = hit.groupId?.let { id -> Repo.groups.firstOrNull { it.id == id } }
                            when {
                                bot != null -> onBot(bot, hit.threadId)
                                group != null -> onGroup(group, hit.threadId)
                                else -> error = "ไม่พบบทสนทนาต้นทาง อาจถูกซ่อนหรือลบไปแล้ว"
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(hit.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    runCatching { java.text.SimpleDateFormat("d MMM · HH:mm", java.util.Locale.getDefault()).format(java.util.Date(hit.at)) }.getOrDefault(""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!hit.task.isNullOrBlank()) {
                                Text(hit.task, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 3.dp))
                            }
                            Text(
                                hit.snippet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hit.role == "user") MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 7.dp),
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (hit.role == "user") "ข้อความของคุณ" else if (hit.kind == "activity") "กิจกรรมของ Bot" else "ข้อความจาก Bot",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.weight(1f))
                                if (!hit.onActivePath) Text(
                                    "ข้อความจากเวอร์ชันก่อนหน้า",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text("เปิดแชต  ›", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 8.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
        Spacer(Modifier.weight(1f))
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BotRow(bot: Bot, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            BotAvatar(bot.name, bot.color, working = bot.busy, unread = bot.unread)
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(bot.name, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (bot.pinned) Text("  · ปักหมุด", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(bot.subtitle.ifBlank { "พร้อมสนทนา" }, style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        bot.activity == "waiting-on-you" -> MaterialTheme.colorScheme.error
                        bot.busy -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (bot.unread) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun GroupRow(group: Group, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center) { Text("G", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold) }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(group.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (group.unread) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ConnectionError(detail: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("เชื่อมต่อคอมพิวเตอร์ไม่ได้", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(detail.ifBlank { "ตรวจสอบว่า OpenMausBot เปิดอยู่ และโทรศัพท์เชื่อมต่อผ่านเครือข่ายที่อนุญาต" },
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        TextButton(onClick = { Repo.connect() }, modifier = Modifier.padding(top = 12.dp)) { Text("ลองอีกครั้ง") }
    }
}

@Composable
private fun NewBotDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("สร้าง Bot ใหม่") },
        text = {
            Column {
                Text("Bot แต่ละตัวมีบทสนทนาและการตั้งค่าของตัวเอง", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it.take(60) }, label = { Text("ชื่อ Bot") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("สร้าง") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } },
    )
}

sealed interface Selection {
    data class BotTarget(val bot: Bot, val threadId: String? = null) : Selection
    data class GroupTarget(val group: Group, val threadId: String? = null) : Selection
}

@Composable
fun BotAvatar(name: String, color: String, working: Boolean = false, unread: Boolean = false) {
    val tint = botColor(color, isDark())
    Box(Modifier.size(46.dp).clip(CircleShape).background(tint.copy(alpha = if (isDark()) 0.18f else 0.16f)),
        contentAlignment = Alignment.Center) {
        if (working) CircularProgressIndicator(Modifier.size(46.dp), strokeWidth = 2.dp, color = tint)
        Text(name.trim().take(1).uppercase(), color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (unread) Box(Modifier.size(9.dp).align(Alignment.BottomEnd).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}
