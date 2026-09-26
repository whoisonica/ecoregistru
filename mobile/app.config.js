// `google-services.json` (Firebase, pentru push pe Android) e ignorat de git: repo-ul e public. Stă numai
// pe discul pe care a fost descărcat, iar la build-urile EAS vine ca fișier de mediu (todo-mobil §16).
// Fără el, `app.json` cerea copierea lui și prebuild-ul cădea — pe 26.09.2026, la prima probă după ce
// worktree-ul în care stătea a fost șters. Acum lipsa lui oprește doar push-ul pe Android, nu build-ul.
const fs = require("fs");
const path = require("path");

// `WH_FARA_PUSH=1`: build semnat cu o echipă Apple personală (gratuită), care nu are voie la Push Notifications —
// fără dreptul `aps-environment` semnarea trece. Înregistrarea tokenului
// cade atunci prinsă (`src/push.ts`), restul aplicației merge. Pe 26.09.2026, pentru iPhone-ul proprietarului.
// Pluginul se aplică automat pentru pachetul instalat, deci nu ajunge scos din listă: dreptul se șterge după el.
function withoutPush(config) {
  if (process.env.WH_FARA_PUSH !== "1") return config;
  const { withEntitlementsPlist } = require("expo/config-plugins");
  return withEntitlementsPlist(config, (c) => {
    delete c.modResults["aps-environment"];
    return c;
  });
}

module.exports = ({ config: base }) => {
  const config = withoutPush(base);
  const file = config.android?.googleServicesFile;
  if (file && !fs.existsSync(path.resolve(__dirname, file))) {
    console.warn(
      `⚠️  ${file} lipsește: build fără Firebase, deci fără push pe Android. ` +
        "Se descarcă din consola Firebase, proiectul wastehouse-22763, aplicația ro.wastehouse.app.",
    );
    const { googleServicesFile: _, ...android } = config.android;
    return { ...config, android };
  }
  return config;
};
