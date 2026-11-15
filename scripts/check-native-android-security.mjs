import { readFile, readdir } from "node:fs/promises";
import path from "node:path";

const root = path.resolve(process.argv[2] ?? "native-client-source/apps/android");
const read = (relative) => readFile(path.join(root, relative), "utf8");
const manifest = await read("app/src/main/AndroidManifest.xml");
const api = await read("app/src/main/java/org/openmausbot/android/data/ApiClient.kt");
const routing = await read("app/src/main/java/org/openmausbot/android/data/CompanionRouting.kt");
const store = await read("app/src/main/java/org/openmausbot/android/data/DeviceStore.kt");
const chat = await read("app/src/main/java/org/openmausbot/android/ui/ChatScreen.kt");

const failures = [];
const required = [
  [manifest.includes('android:allowBackup="false"'), "App backup must remain disabled for pairing tokens."],
  [manifest.includes('android:name="android.permission.CAMERA"'), "Camera permission must remain declared for QR pairing."],
  [!manifest.includes("READ_MEDIA_IMAGES"), "Broad image-library permission must not be declared."],
  [!manifest.includes("READ_EXTERNAL_STORAGE"), "Broad external-storage permission must not be declared."],
  [!manifest.includes("MANAGE_EXTERNAL_STORAGE"), "All-files access permission must not be declared."],
  [!manifest.includes("QUERY_ALL_PACKAGES"), "Installed-app inventory permission must not be declared."],
  [!manifest.includes("READ_SMS") && !manifest.includes("READ_CONTACTS") && !manifest.includes("READ_CALL_LOG"), "Sensitive personal-data permissions must not be declared."],
  [api.includes("enforceTransport") && api.includes("instanceFollowRedirects = false"), "API calls must restrict cleartext transport and avoid automatic redirects."],
  [routing.includes("isPrivateOrLocalHost") && routing.includes("EndpointKind.LAN"), "HTTP pairing routes must be restricted to private/local addresses."],
  [store.includes("AndroidKeyStore") && store.includes("AES/GCM/NoPadding"), "Device pairing data must use Android Keystore AES/GCM encryption."],
  [chat.includes("PickVisualMedia"), "Image attachments must use the system Photo Picker."],
];

for (const [ok, message] of required) {
  if (message === "placeholder") continue;
  if (!ok) failures.push(message);
}

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...await walk(full));
    else if (/\.(kt|java|xml|gradle)$/.test(entry.name)) files.push(full);
  }
  return files;
}

const sourceFiles = await walk(path.join(root, "app/src/main"));
const secretPatterns = [
  /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/,
  /\bsk-[A-Za-z0-9]{24,}\b/,
  /\bgh[pousr]_[A-Za-z0-9]{30,}\b/,
  /\bAIza[0-9A-Za-z_-]{30,}\b/,
];
for (const file of sourceFiles) {
  const content = await readFile(file, "utf8");
  if (secretPatterns.some((pattern) => pattern.test(content))) {
    failures.push("Possible embedded credential found in " + path.relative(root, file));
  }
}

if (failures.length) {
  console.error("Android security checks failed:");
  for (const failure of failures) console.error(" - " + failure);
  process.exit(1);
}

console.log("Android security checks passed.");
console.log(" - No broad storage or installed-app inventory permissions");
console.log(" - Pairing data encrypted with Android Keystore AES/GCM");
console.log(" - Public cleartext routes rejected; HTTPS required for remote hosts");
console.log(" - Photo Picker used for user-selected image attachments");
console.log(" - No embedded API keys or private-key blocks detected");
