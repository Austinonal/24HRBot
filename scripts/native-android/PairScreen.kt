package org.openmausbot.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import org.openmausbot.android.data.PairingInvite
import org.openmausbot.android.data.Repo
import org.openmausbot.android.data.currentBaseUrl

@Composable
fun PairScreen(invite: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pasted by remember { mutableStateOf("") }
    var manualOpen by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("https://") }
    var code by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<PairingInvite?>(null) }
    var saved by remember { mutableStateOf(Repo.computers) }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents == null) return@rememberLauncherForActivityResult
        val parsed = PairingInvite.parse(contents)
        if (parsed == null) error = "QR นี้ไม่ใช่คำเชิญเชื่อมต่อ OpenMausBot ที่ถูกต้อง"
        else pending = parsed
    }

    fun pair(inviteValue: PairingInvite) {
        busy = true
        error = null
        scope.launch {
            val outcome = Repo.pairWith(inviteValue)
            busy = false
            outcome.onSuccess {
                saved = Repo.computers
                pending = null
                onDismiss()
                Repo.connect(it)
            }.onFailure { error = it.message ?: "เชื่อมต่อไม่สำเร็จ" }
        }
    }

    LaunchedEffect(invite) {
        if (invite != null) {
            val parsed = PairingInvite.parse(invite)
            if (parsed == null) error = "ลิงก์นี้ไม่ใช่คำเชิญเชื่อมต่อที่ถูกต้อง"
            else pending = parsed
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "O",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "OPENMAUSBOT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "ANDROID APP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.4.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50.dp),
                ) {
                    Text(
                        "เชื่อมต่ออุปกรณ์",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(44.dp))
            Text(
                "ทีม AI ของคุณ\nอยู่ใกล้แค่ปลายนิ้ว",
                fontSize = 32.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "เชื่อมต่อกับ OpenMausBot Server บน Cloud หรือเซิร์ฟเวอร์ที่คุณดูแล เพื่อแชตกับ Bot ติดตามงาน และอนุมัติคำขอจากมือถือ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeaturePill("แชตกับ Bot")
                FeaturePill("ติดตามงาน")
                FeaturePill("แจ้งเตือน")
            }

            if (pending != null) {
                Spacer(Modifier.height(26.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "ตรวจสอบก่อนเชื่อมต่อ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            pending!!.serverName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            pending!!.baseUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "ตรวจสอบชื่อและที่อยู่ให้แน่ใจว่าเป็น OpenMausBot Server ที่คุณเป็นเจ้าของหรือไว้วางใจก่อนจับคู่",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (pending!!.token == null && pending!!.code == null) {
                            Text(
                                "คำเชิญนี้ไม่มีข้อมูลจับคู่ โปรดสร้าง QR Code หรือลิงก์คำเชิญใหม่จาก OpenMausBot Server",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { pending?.let(::pair) },
                                enabled = !busy && (pending!!.token != null || pending!!.code != null),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                if (busy) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("ยืนยันการเชื่อมต่อ")
                                }
                            }
                            OutlinedButton(
                                onClick = { pending = null; onDismiss() },
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("ยกเลิก")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scanner.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("สแกน QR Code จาก OpenMausBot Server")
                            .setBeepEnabled(false)
                            .setOrientationLocked(true)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("สแกน QR Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "เปิดหน้าจอจับคู่ของ OpenMausBot Server แล้วสแกน QR Code ที่แสดง",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            )

            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = pasted,
                onValueChange = { pasted = it },
                label = { Text("ลิงก์เชื่อมต่อ") },
                placeholder = { Text("วางลิงก์คำเชิญที่นี่") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedButton(
                onClick = {
                    val parsed = PairingInvite.parse(pasted)
                    if (parsed == null) error = "ตรวจสอบลิงก์ไม่สำเร็จ โปรดใช้ลิงก์คำเชิญจาก OpenMausBot Server"
                    else pending = parsed
                },
                enabled = pasted.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("ตรวจสอบลิงก์")
            }

            TextButton(
                onClick = { manualOpen = !manualOpen },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (manualOpen) "ซ่อนตัวเลือกการเชื่อมต่อ" else "เชื่อมต่อด้วยวิธีอื่น")
            }

            if (manualOpen) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("เชื่อมต่อด้วยที่อยู่และรหัส", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it.trim() },
                            label = { Text("ที่อยู่เซิร์ฟเวอร์") },
                            placeholder = { Text("https://maus.example.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            label = { Text("รหัสจับคู่ 6 หลัก") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Text(
                            "การเชื่อมต่อ HTTP ใช้ได้เฉพาะเครือข่ายภายในที่รองรับเท่านั้น หากเชื่อมต่อผ่านอินเทอร์เน็ต ให้ใช้ HTTPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Button(
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    val outcome = Repo.pairWithCode(host.trim(), code, null)
                                    busy = false
                                    outcome.onSuccess {
                                        saved = Repo.computers
                                        onDismiss()
                                        Repo.connect(it)
                                    }.onFailure { error = it.message ?: "เชื่อมต่อไม่สำเร็จ" }
                                }
                            },
                            enabled = !busy && code.length == 6 && host.length > 8,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("เชื่อมต่อด้วยรหัส")
                        }
                    }
                }
            }

            error?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                ),
            ) {
                Column(Modifier.padding(15.dp)) {
                    Text(
                        "ความปลอดภัย",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "จับคู่เฉพาะเซิร์ฟเวอร์ที่คุณควบคุมหรือเชื่อถือ ตรวจชื่อและที่อยู่ก่อนยืนยัน และใช้ HTTPS เมื่อเชื่อมต่อผ่านอินเทอร์เน็ต",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }

            if (saved.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                Text(
                    "เซิร์ฟเวอร์ที่เชื่อมต่อแล้ว",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                saved.forEach { computer ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    computer.serverName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(computer.serverName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    computer.currentBaseUrl(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = {
                                Repo.forget(computer.id)
                                saved = Repo.computers
                            }) { Text("ลบ") }
                            TextButton(onClick = {
                                Repo.select(computer.id)
                                onDismiss()
                            }) { Text("เปิด") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FeaturePill(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
