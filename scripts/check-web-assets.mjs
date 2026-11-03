import fs from "node:fs";
import vm from "node:vm";

const htmlPath = "app/src/main/assets/index.html";
const html = fs.readFileSync(htmlPath, "utf8");
const scripts = [...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/gi)];

if (scripts.length === 0) {
  throw new Error(`No inline scripts found in ${htmlPath}`);
}

scripts.forEach((match, index) => {
  new vm.Script(match[1], { filename: `${htmlPath}#script-${index + 1}` });
});

const required = [
  ["account-scoped session storage", "scopedSessionsKey(storageOwner)"],
  ["account switch on authentication", "switchStorageOwner(signedIn?user.uid:'guest')"],
  ["explicit cloud create flag", "bridge.connect(state.date,cloudPayload(),Boolean(createIfMissing))"],
  ["receipt storage budget", "pendingReceipt.length>480000"]
];

for (const [label, source] of required) {
  if (!html.includes(source)) {
    throw new Error(`Missing safeguard: ${label}`);
  }
}

if (/localStorage\.setItem\((?:KEY|SESSIONS_KEY)/.test(html)) {
  throw new Error("Unscoped localStorage write detected");
}

console.log(`Checked ${scripts.length} script block(s) and storage safeguards.`);
