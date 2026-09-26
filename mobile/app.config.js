// `google-services.json` (Firebase, pentru push pe Android) e ignorat de git: repo-ul e public. Stă numai
// pe discul pe care a fost descărcat, iar la build-urile EAS vine ca fișier de mediu (todo-mobil §16).
// Fără el, `app.json` cerea copierea lui și prebuild-ul cădea — pe 26.09.2026, la prima probă după ce
// worktree-ul în care stătea a fost șters. Acum lipsa lui oprește doar push-ul pe Android, nu build-ul.
const fs = require("fs");
const path = require("path");

module.exports = ({ config }) => {
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
