package org.openmausbot.android.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.openmausbot.android.data.Attachment
import org.openmausbot.android.data.Message
import org.openmausbot.android.data.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    selection: Selection,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val bot = (selection as? Selection.BotTarget)?.bot
    val group = (selection as? Selection.GroupTarget)?.group
    val requestedThreadId = when (selection) {
        is Selection.BotTarget -> selection.threadId
        is Selection.GroupTarget -> selection.threadId
    }
    val threadId = requestedThreadId?.takeIf { it.isNotBlank() }
        ?: bot?.activeTask?.threadId ?: bot?.threadId ?: group?.threadId ?: ""
    val title = bot?.name ?: group?.name ?: "แชต"
    val subtitle = bot?.subtitle ?: group?.subtitle ?: ""
    val liveBot = bot?.let { Repo.bots.firstOrNull { b -> b.id == it.id } } ?: bot
    val messages = Repo.messages(threadId)
    val visibleMessages = messages.filterNot { it.isChatNoise() }
    val hasMore = Repo.hasMore(threadId)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var didInitialScroll by remember(threadId) { mutableStateOf(false) }
    var draft by rememberSaveable(threadId) { mutableStateOf("") }
    var attachment by remember { mutableStateOf<Attachment?>(null) }
    var uploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val busy = liveBot?.busy == true

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (bytes != null) {
                Repo.uploadImage(bytes, mime, "photo.jpg").onSuccess { attachment = it }
            }
            uploading = false
        }
    }

    LaunchedEffect(threadId) {
        Repo.openThread(threadId)
    }
    LaunchedEffect(threadId, Repo.openThreadId) {
        Repo.openThreadId = threadId
    }
    LaunchedEffect(threadId, visibleMessages.size) {
        if (visibleMessages.isEmpty()) return@LaunchedEffect
        val lastItem = visibleMessages.lastIndex + if (hasMore) 1 else 0
        if (!didInitialScroll) {
            runCatching { listState.scrollToItem(lastItem) }
            didInitialScroll = true
        } else {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= lastItem - 2) {
                runCatching { listState.animateScrollToItem(lastItem) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "กลับ")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (liveBot != null) {
                            BotAvatar(liveBot.name, liveBot.color, working = liveBot.busy)
                            Spacer(Modifier.size(10.dp))
                        } else {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.size(10.dp))
                        }
                        Column {
                            Text(
                                title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                when {
                                    busy -> "กำลังทำงาน…"
                                    subtitle.isNotBlank() -> subtitle
                                    Repo.connection.phase == org.openmausbot.android.data.ConnectionState.Phase.ONLINE -> "พร้อมสนทนา"
                                    else -> "กำลังเชื่อมต่อ"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (busy) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "เมนูเพิ่มเติม")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (bot != null) {
                            DropdownMenuItem(
                                text = { Text("ตั้งค่า Bot") },
                                onClick = { menu = false; sheet = true },
                            )
                            DropdownMenuItem(
                                text = { Text("หยุดการทำงาน") },
                                enabled = busy,
                                onClick = { menu = false; Repo.interrupt(bot.id, threadId) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("ตั้งค่าแอป") },
                            onClick = { menu = false; onOpenSettings() },
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                if (visibleMessages.isEmpty()) {
                    item {
                        ChatWelcome(
                            title = title,
                            onPrompt = { draft = it },
                        )
                    }
                }
                if (hasMore && visibleMessages.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (Repo.isLoading(threadId)) {
                                CircularProgressIndicator(Modifier.padding(10.dp).size(18.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(
                                    onClick = { scope.launch { Repo.loadOlder(threadId) } },
                                ) {
                                    Text("โหลดข้อความก่อนหน้า")
                                }
                            }
                        }
                    }
                }
                items(visibleMessages, key = { it.id }) { message ->
                    MessageRow(
                        message = message,
                        onAnswer = { answer ->
                            bot?.let { Repo.respondCard(it.id, threadId, message, answer) }
                        },
                        onAlwaysAllow = {
                            message.card?.let { card ->
                                bot?.let { Repo.alwaysAllow(it.id, threadId, card) }
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            attachment?.let {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "แนบรูปภาพแล้ว 1 ภาพ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = { attachment = null }) { Text("นำออก") }
                }
            }

            Composer(
                draft = draft,
                onDraft = { draft = it },
                busy = busy,
                uploading = uploading,
                canAttach = bot != null,
                onAttach = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onSend = {
                    val text = draft
                    val files = listOfNotNull(attachment)
                    if (bot != null) {
                        Repo.send(bot.id, threadId, text, files)
                    } else if (group != null) {
                        Repo.sendToGroup(group.id, threadId, text)
                    }
                    draft = ""
                    attachment = null
                },
                onStop = { bot?.let { Repo.interrupt(it.id, threadId) } },
            )
        }
    }

    if (sheet && bot != null) {
        BotSheet(bot = liveBot ?: bot, onDismiss = { sheet = false })
    }
}

@Composable
private fun ChatWelcome(title: String, onPrompt: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 54.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("AI", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "วันนี้อยากให้ $title ช่วยอะไร?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "พิมพ์คำถามหรือเลือกหัวข้อเพื่อเริ่มบทสนทนา",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )
        listOf(
            "ช่วยอธิบายเรื่องยากให้เข้าใจง่าย",
            "ช่วยสรุปเนื้อหาที่ฉันส่งให้",
            "ช่วยวางแผนงานเป็นขั้นตอน",
        ).forEach { prompt ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onPrompt(prompt) },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text(
                    prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                )
            }
        }
    }
}

private fun Message.isChatNoise(): Boolean =
    kind == "digest" || kind == "activity" || kind == "notice" ||
        tool != null || (isBot && (text.startsWith("[digest]") || text.startsWith("notice:")))

@Composable
private fun MessageRow(
    message: Message,
    onAnswer: (String) -> Unit,
    onAlwaysAllow: () -> Unit,
) {
    when {
        message.kind == "activity" || message.tool != null -> {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(
                        message.tool?.let { tool ->
                            (tool.label ?: tool.name).let { if (tool.ok) it else "ไม่สำเร็จ: $it" }
                        } ?: message.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.tool?.ok == false) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
        message.isUser -> Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(22.dp, 22.dp, 5.dp, 22.dp),
                modifier = Modifier.widthIn(max = 320.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    if (message.text.isNotBlank()) {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    message.attachments.forEach { AttachmentView(it) }
                    if (message.pending) {
                        Text(
                            if (message.failed) "ยังไม่ได้ส่ง" else "กำลังส่ง…",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (message.failed) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }
        else -> Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            message.fromName?.let { name ->
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall,
                    color = message.fromColor?.let { botColor(it, isDark()) }
                        ?: MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
                )
            }
            if (message.card != null) {
                DecisionCard(message, onAnswer = onAnswer, onAlwaysAllow = onAlwaysAllow)
            } else {
                Column(Modifier.fillMaxWidth().padding(end = 4.dp)) {
                    if (message.text.isNotBlank()) Markdown(message.text)
                    message.attachments.forEach { AttachmentView(it) }
                }
            }
        }
    }
}

@Composable
private fun AttachmentView(attachment: Attachment) {
    if (!attachment.isImage) {
        Text(
            attachment.name ?: "ไฟล์แนบ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        return
    }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, attachment.path) {
        val bytes = Repo.imageBytes(attachment.path)
        value = bytes?.let { raw ->
            runCatching { BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap() }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = attachment.name ?: "รูปภาพ",
            modifier = Modifier
                .padding(top = 7.dp)
                .clip(RoundedCornerShape(14.dp))
                .widthIn(max = 300.dp),
        )
    } else {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(width = 180.dp, height = 90.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraft: (String) -> Unit,
    busy: Boolean,
    uploading: Boolean,
    canAttach: Boolean,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                if (canAttach) {
                    IconButton(
                        onClick = onAttach,
                        enabled = !uploading,
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    ) {
                        if (uploading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                "+",
                                fontSize = 25.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraft,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(if (busy) "Bot กำลังคิด…" else "ถามอะไรก็ได้")
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                Spacer(Modifier.size(7.dp))
                if (busy) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "หยุด",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSend,
                        enabled = draft.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (draft.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "ส่ง",
                            tint = if (draft.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                "OpenMausBot อาจตอบผิดพลาดได้ โปรดตรวจสอบข้อมูลสำคัญ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
