import { copyFile, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const repoRoot = process.cwd();
const appRoot = path.resolve(process.argv[2] ?? "native-client-source/apps/android");

async function copyTemplate(templatePath, destinationPath) {
  await copyFile(path.join(repoRoot, templatePath), path.join(appRoot, destinationPath));
  console.log("installed " + destinationPath);
}

async function replacePairs(relativePath, pairs) {
  const fullPath = path.join(appRoot, relativePath);
  let source = await readFile(fullPath, "utf8");
  for (const [from, to] of pairs) {
    if (!source.includes(from)) {
      throw new Error("Expected localization source was not found in " + relativePath + ": " + from);
    }
    source = source.split(from).join(to);
  }
  await writeFile(fullPath, source, "utf8");
  console.log("localized " + relativePath);
}

await copyTemplate(
  "scripts/native-android/PairScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/PairScreen.kt",
);
await copyTemplate(
  "scripts/native-android/Theme.kt",
  "app/src/main/java/org/openmausbot/android/ui/Theme.kt",
);
await copyTemplate(
  "scripts/native-android/ChatScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/ChatScreen.kt",
);
await copyTemplate(
  "scripts/native-android/AccountsScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/AccountsScreen.kt",
);
await copyTemplate(
  "scripts/native-android/BotSheet.kt",
  "app/src/main/java/org/openmausbot/android/ui/BotSheet.kt",
);
await copyTemplate(
  "scripts/native-android/DeviceStore.kt",
  "app/src/main/java/org/openmausbot/android/data/DeviceStore.kt",
);
await copyTemplate(
  "scripts/native-android/HomeScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/HomeScreen.kt",
);
await copyTemplate(
  "scripts/native-android/RoutinesScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/RoutinesScreen.kt",
);
await copyTemplate(
  "scripts/native-android/InboxScreen.kt",
  "app/src/main/java/org/openmausbot/android/ui/InboxScreen.kt",
);

const localization = {
  "app/src/main/java/org/openmausbot/android/ui/HomeScreen.kt": [
    ['"忽略"', '"ปิด"'],
    ['"刷新"', '"รีเฟรช"'],
    ['"更多"', '"เมนูเพิ่มเติม"'],
    ['"设置"', '"ตั้งค่า"'],
    ['"重新连接"', '"เชื่อมต่ออีกครั้ง"'],
    ['"新建 Bot"', '"สร้าง Bot ใหม่"'],
    ['"聊天"', '"แชต"'],
    ['"房间"', '"ห้อง"'],
    ['"正在连接…"', '"กำลังเชื่อมต่อ…"'],
    ['"已连接"', '"เชื่อมต่อแล้ว"'],
    ['"未连接"', '"ยังไม่เชื่อมต่อ"'],
    ['"离线"', '"ออฟไลน์"'],
    ['"未配对"', '"ยังไม่ได้จับคู่"'],
    ['"还没有 Bot"', '"ยังไม่มี Bot"'],
    ['"点右下角的 + 新建一个。"', '"แตะปุ่ม + เพื่อสร้าง Bot ใหม่"'],
    ['"  · 置顶"', '"  · ปักหมุด"'],
    ['"还没有房间"', '"ยังไม่มีห้องแชต"'],
    ['"房间是多个 Bot 的群聊，请在电脑上创建。"', '"ห้องคือพื้นที่สนทนาร่วมกันของหลาย Bot สร้างได้จากคอมพิวเตอร์"'],
    ['"连不上你的电脑"', '"เชื่อมต่อคอมพิวเตอร์ไม่ได้"'],
    ['"那台电脑上的 OpenMausBot 必须正在运行，且手机要和它处于同一网络，或已配对可远程访问的线路。"', '"ตรวจสอบว่า OpenMausBot กำลังทำงาน และโทรศัพท์อยู่ในเครือข่ายเดียวกัน หรือใช้ช่องทางระยะไกลที่ตั้งค่าไว้"'],
    ['"重试"', '"ลองอีกครั้ง"'],
    ['"每个 Bot 都有独立的对话、记忆和模型。"', '"แต่ละ Bot มีบทสนทนา หน่วยความจำ และโมเดลแยกจากกัน"'],
    ['"名称"', '"ชื่อ"'],
    ['"创建"', '"สร้าง"'],
    ['"取消"', '"ยกเลิก"'],
  ],
  "app/src/main/java/org/openmausbot/android/ui/ChatScreen.kt": [
    ['"聊天"', '"แชต"'],
    ['"返回"', '"กลับ"'],
    ['"菜单"', '"เมนู"'],
    ['"Bot 设置"', '"ตั้งค่า Bot"'],
    ['"停止本轮"', '"หยุดการทำงาน"'],
    ['"设置"', '"ตั้งค่า"'],
    ['"加载更早"', '"โหลดข้อความก่อนหน้า"'],
    ['"已附加 1 张图片"', '"แนบรูปภาพแล้ว 1 ภาพ"'],
    ['"移除"', '"นำออก"'],
    ['"失败：$it"', '"ไม่สำเร็จ: $it"'],
    ['"未发送"', '"ยังไม่ได้ส่ง"'],
    ['"发送中…"', '"กำลังส่ง…"'],
    ['"附件"', '"ไฟล์แนบ"'],
    ['"图片"', '"รูปภาพ"'],
    ['"Bot 正在工作…"', '"Bot กำลังทำงาน…"'],
    ['"输入消息"', '"พิมพ์ข้อความ"'],
    ['"停止"', '"หยุด"'],
    ['"发送"', '"ส่ง"'],
  ],
  "app/src/main/java/org/openmausbot/android/ui/SettingsScreen.kt": [
    ['"返回"', '"กลับ"'],
    ['"设置"', '"ตั้งค่า"'],
    ['"外观"', '"รูปลักษณ์"'],
    ['"主题"', '"ธีม"'],
    ['"深色"', '"มืด"'],
    ['"浅色"', '"สว่าง"'],
    ['"跟随系统"', '"ตามระบบ"'],
    ['"系统"', '"ระบบ"'],
    ['"提醒"', '"การแจ้งเตือน"'],
    ['"后台通知"', '"การแจ้งเตือนเบื้องหลัง"'],
    ['"在后台保持连接，Bot 回复时发送通知。"', '"รับการแจ้งเตือนเมื่อ Bot ตอบกลับ แม้ไม่ได้เปิดแอปอยู่"'],
    ['"朗读回复"', '"อ่านข้อความตอบกลับ"'],
    ['"使用手机自带的语音引擎朗读新的 Bot 消息。"', '"ใช้ระบบเสียงของโทรศัพท์อ่านข้อความใหม่จาก Bot"'],
    ['"连接"', '"การเชื่อมต่อ"'],
    ['"未连接"', '"ยังไม่เชื่อมต่อ"'],
    ['"已连接"', '"เชื่อมต่อแล้ว"'],
    ['"正在连接"', '"กำลังเชื่อมต่อ"'],
    ['"出错"', '"เกิดข้อผิดพลาด"'],
    ['"离线"', '"ออฟไลน์"'],
    ['"空闲"', '"ว่าง"'],
    ['"重新连接"', '"เชื่อมต่ออีกครั้ง"'],
    ['"忘记这台电脑"', '"ลบการเชื่อมต่อคอมพิวเตอร์นี้"'],
    ['"已配对的电脑"', '"คอมพิวเตอร์ที่เชื่อมต่อแล้ว"'],
    ['"当前"', '"กำลังใช้งาน"'],
    ['"配对另一台电脑"', '"เชื่อมต่อคอมพิวเตอร์เครื่องอื่น"'],
    ['"OpenMausBot Android 版', '"OpenMausBot สำหรับ Android'],
    ['"你的电脑负责运行 Bot。此应用只保存一个可撤销的设备令牌，不会在电脑之外保存 Bot 凭据或聊天记录。"', '"Bot ทำงานบนคอมพิวเตอร์ แอปนี้เก็บเฉพาะโทเคนของอุปกรณ์ที่เพิกถอนได้ และไม่เก็บข้อมูลเข้าสู่ระบบหรือประวัติแชตไว้นอกคอมพิวเตอร์"'],
  ],
  "app/src/main/java/org/openmausbot/android/ui/BotSheet.kt": [
    ['"未选择模型"', '"ยังไม่ได้เลือกโมเดล"'],
    ['"名称"', '"ชื่อ"'],
    ['"保存"', '"บันทึก"'],
    ['"模型"', '"โมเดล"'],
    ['"正在读取这台电脑的模型…"', '"กำลังอ่านรายชื่อโมเดลจากคอมพิวเตอร์…"'],
    ['"当前连接无法获取模型列表，请在电脑上切换模型。"', '"การเชื่อมต่อนี้อ่านรายชื่อโมเดลไม่ได้ โปรดเลือกโมเดลจากคอมพิวเตอร์"'],
    ['"接收此 Bot 的通知"', '"รับการแจ้งเตือนจาก Bot นี้"'],
    ['"回复和审批请求会推送到手机"', '"ข้อความตอบกลับและคำขออนุมัติจะส่งมาที่โทรศัพท์"'],
    ['"置顶"', '"ปักหมุด"'],
    ['"让这个 Bot 始终排在列表顶部"', '"ให้ Bot นี้อยู่ด้านบนของรายการเสมอ"'],
    ['"新对话"', '"เริ่มแชตใหม่"'],
    ['"隐藏 Bot"', '"ซ่อน Bot"'],
  ],
  "app/src/main/java/org/openmausbot/android/ui/Cards.kt": [
    ['"问题"', '"คำถาม"'],
    ['"审批"', '"คำขออนุมัติ"'],
    ['"请求"', '"คำขอ"'],
    ['"已回答：', '"ตอบแล้ว: '],
    ['"已允许"', '"อนุญาตแล้ว"'],
    ['"已拒绝"', '"ปฏิเสธแล้ว"'],
    ['"这个请求已过期，请让 Bot 重新发起。"', '"คำขอนี้หมดอายุแล้ว โปรดให้ Bot ส่งคำขอใหม่"'],
    ['"当前回合已停止，无法再回答这个请求。"', '"การทำงานรอบนี้หยุดแล้ว จึงตอบคำขอนี้ไม่ได้"'],
    ['"已忽略"', '"ละเว้นแล้ว"'],
    ['"正在等待电脑响应"', '"กำลังรอการตอบกลับจากคอมพิวเตอร์"'],
    ['"始终允许此类操作"', '"อนุญาตการกระทำประเภทนี้เสมอ"'],
    ['审批选项由电脑', 'ตัวเลือกการอนุมัติมาจากคอมพิวเตอร์'],
    ['"允许一次"', '"อนุญาตครั้งนี้"'],
    ['"始终允许"', '"อนุญาตเสมอ"'],
    ['"拒绝"', '"ปฏิเสธ"'],
  ],
  "app/src/main/java/org/openmausbot/android/data/PairingClient.kt": [
    ['"这个邀请没有配对码"', '"คำเชิญนี้ไม่มีรหัสจับคู่"'],
  ],
  "app/src/main/java/org/openmausbot/android/data/ApiClient.kt": [
    ['"上传失败"', '"อัปโหลดไม่สำเร็จ"'],
    ['"电脑没有返回设备令牌"', '"คอมพิวเตอร์ไม่ได้ส่งโทเคนอุปกรณ์กลับมา"'],
    ['"请求失败"', '"คำขอล้มเหลว"'],
    ['"事件流被拒绝"', '"เซิร์ฟเวอร์ปฏิเสธการเชื่อมต่อสตรีม"'],
  ],
  "app/src/main/java/org/openmausbot/android/data/CompanionRouting.kt": [
    ['"这台电脑"', '"คอมพิวเตอร์เครื่องนี้"'],
    ['"无法连接到 $computer。请确认电脑已唤醒并打开 OpenMausBot，然后重试。"', '"เชื่อมต่อ $computer ไม่ได้ โปรดเปิดคอมพิวเตอร์และ OpenMausBot แล้วลองอีกครั้ง"'],
    ['"手机无法通过 Tailscale 连接到 $computer。请确认手机已开启 Tailscale 并登录同一 tailnet，然后重试。"', '"โทรศัพท์เชื่อมต่อ $computer ผ่าน Tailscale ไม่ได้ โปรดเปิด Tailscale และเข้าสู่ระบบ tailnet เดียวกับคอมพิวเตอร์ แล้วลองอีกครั้ง"'],
    ['"手机无法在当前网络连接到 $computer。请把手机连接到与电脑相同的 Wi-Fi；"', '"โทรศัพท์เชื่อมต่อ $computer ผ่านเครือข่ายปัจจุบันไม่ได้ โปรดเชื่อมต่อ Wi-Fi เดียวกับคอมพิวเตอร์;"'],
    ['"如果已经连接，电脑防火墙可能拦截了 OpenMausBot；"', '"หากเชื่อมต่อเครือข่ายเดียวกันแล้ว ไฟร์วอลล์ของคอมพิวเตอร์อาจบล็อก OpenMausBot;"'],
    ['"或者在电脑的“设置 → 远程访问”中登录，让手机从任何网络连接。"', '"หรือเปิดการตั้งค่า → การเข้าถึงระยะไกลบนคอมพิวเตอร์ เพื่อให้โทรศัพท์เชื่อมต่อจากเครือข่ายอื่นได้"'],
    ['"\\n尝试过："', '"\\nที่อยู่ที่ลองแล้ว: "'],
  ],
};

for (const [relativePath, pairs] of Object.entries(localization)) {
  // ChatScreen.kt is supplied as a Thai-first redesigned template.
  if (relativePath.endsWith("/ChatScreen.kt") || relativePath.endsWith("/HomeScreen.kt")) continue;
  await replacePairs(relativePath, pairs);
}

const apiPath = path.join(appRoot, "app/src/main/java/org/openmausbot/android/data/ApiClient.kt");
let api = await readFile(apiPath, "utf8");
const oldOpen = [
  "    private fun open(method: String, path: String): HttpURLConnection {",
  "        val url = URL(baseUrl + path)",
  "        val connection = url.openConnection() as HttpURLConnection",
  "        connection.requestMethod = method",
].join("\n");
const newOpen = [
  "    private fun open(method: String, path: String): HttpURLConnection {",
  "        val url = URL(baseUrl + path)",
  "        enforceTransport(url)",
  "        val connection = url.openConnection() as HttpURLConnection",
  "        connection.instanceFollowRedirects = false",
  "        connection.requestMethod = method",
].join("\n");
if (!api.includes(oldOpen)) throw new Error("ApiClient.open block not found");
api = api.replace(oldOpen, newOpen);
const oldStream = [
  '            val conn = URL(client.baseUrl + "/api/events").openConnection() as HttpURLConnection',
  "            connection = conn",
  "            try {",
  '                conn.requestMethod = "GET"',
].join("\n");
const newStream = [
  '            val streamUrl = URL(client.baseUrl + "/api/events")',
  "            client.enforceTransport(streamUrl)",
  "            val conn = streamUrl.openConnection() as HttpURLConnection",
  "            connection = conn",
  "            try {",
  "                conn.instanceFollowRedirects = false",
  '                conn.requestMethod = "GET"',
].join("\n");
if (!api.includes(oldStream)) throw new Error("ApiClient Stream URL block not found");
api = api.replace(oldStream, newStream);
const apiMarker = "    private fun open(method: String, path: String): HttpURLConnection {";
const apiHelper = [
  "    private fun enforceTransport(url: URL) {",
  '        if (url.protocol.equals("https", ignoreCase = true)) return',
  '        if (!url.protocol.equals("http", ignoreCase = true) || !isPrivateOrLocalHost(url.host)) {',
  '            throw IOException("เพื่อความปลอดภัย การเชื่อมต่อ HTTP อนุญาตเฉพาะเครือข่ายภายใน ใช้ HTTPS สำหรับที่อยู่ภายนอก")',
  "        }",
  "    }",
  "",
  "    private fun isPrivateOrLocalHost(rawHost: String): Boolean {",
  '        val host = rawHost.trim().removePrefix("[").removeSuffix("]").lowercase()',
  '        if (host == "localhost" || host == "::1" || host.endsWith(".local") || host.endsWith(".ts.net")) return true',
  '        val octets = host.split(".").mapNotNull { it.toIntOrNull() }',
  "        if (octets.size == 4 && octets.all { it in 0..255 }) {",
  "            val first = octets[0]",
  "            val second = octets[1]",
  "            return first == 10 || first == 127 ||",
  "                (first == 172 && second in 16..31) ||",
  "                (first == 192 && second == 168) ||",
  "                (first == 169 && second == 254) ||",
  "                (first == 100 && second in 64..127)",
  "        }",
  '        return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")',
  "    }",
  "",
].join("\n");
if (!api.includes(apiMarker)) throw new Error("ApiClient insertion marker not found");
api = api.replace(apiMarker, apiHelper + apiMarker);
await writeFile(apiPath, api, "utf8");
console.log("hardened ApiClient transport");

const routingPath = path.join(appRoot, "app/src/main/java/org/openmausbot/android/data/CompanionRouting.kt");
let routing = await readFile(routingPath, "utf8");
const oldPolicy = [
  "            when (kind) {",
  '                EndpointKind.HOSTED -> if (scheme != "https") return null',
  '                EndpointKind.TAILNET -> if (scheme != "http" || !isTailnetHost(host)) return null',
  '                EndpointKind.LAN, EndpointKind.BONJOUR -> if (scheme != "http") return null',
  "            }",
].join("\n");
const newPolicy = [
  "            when (kind) {",
  '                EndpointKind.HOSTED -> if (scheme != "https") return null',
  '                EndpointKind.TAILNET -> if (scheme != "http" || !isTailnetHost(host)) return null',
  '                EndpointKind.LAN -> if (scheme != "http" || !isPrivateOrLocalHost(host)) return null',
  "                EndpointKind.BONJOUR -> if (scheme != \"http\" || !(host == \"localhost\" || host.endsWith(\".local\"))) return null",
  "            }",
].join("\n");
if (!routing.includes(oldPolicy)) throw new Error("CompanionRouting endpoint policy not found");
routing = routing.replace(oldPolicy, newPolicy);
const routingMarker = "        fun fromJson(json: JSONObject): CompanionEndpoint? {";
const routingHelper = [
  "        private fun isPrivateOrLocalHost(host: String): Boolean {",
  '            if (host == "localhost" || host == "::1" || host.endsWith(".local") || isTailnetHost(host)) return true',
  '            val octets = host.split(".").mapNotNull { it.toIntOrNull() }',
  "            if (octets.size == 4 && octets.all { it in 0..255 }) {",
  "                val first = octets[0]",
  "                val second = octets[1]",
  "                return first == 10 || first == 127 ||",
  "                    (first == 172 && second in 16..31) ||",
  "                    (first == 192 && second == 168) ||",
  "                    (first == 169 && second == 254) ||",
  "                    (first == 100 && second in 64..127)",
  "            }",
  '            return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")',
  "        }",
  "",
].join("\n");
if (!routing.includes(routingMarker)) throw new Error("CompanionRouting insertion marker not found");
routing = routing.replace(routingMarker, routingHelper + routingMarker);
await writeFile(routingPath, routing, "utf8");
console.log("restricted non-HTTPS endpoints to private/local routes");

const manifestPath = path.join(appRoot, "app/src/main/AndroidManifest.xml");
let manifest = await readFile(manifestPath, "utf8");
manifest = manifest.replace('    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />\n', "");
manifest = manifest.replace('android:icon="@drawable/ic_launcher"', 'android:icon="@drawable/mausbot_logo"');
manifest = manifest.replace('    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"\n        android:maxSdkVersion="32" />\n', "");
manifest = manifest.replace(
  "    <application\n",
  "    <!-- HTTP is retained only for explicitly selected private LAN/Tailscale routes.\n         CompanionEndpoint and ApiClient reject public cleartext endpoints. Remote hosts require HTTPS. -->\n    <application\n",
);
await writeFile(manifestPath, manifest, "utf8");
console.log("removed broad media/storage permissions");

console.log("Native Android Thai UI and security patches applied.");
