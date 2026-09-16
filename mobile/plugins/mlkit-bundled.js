// Recunoașterea de text pe Android cu modelul LEGAT în aplicație, nu cel descărcat de Play Services.
//
// `expo-text-extractor` cere `play-services-mlkit-text-recognition`, al cărui model vine abia la prima
// folosire, prin Play Services și cu internet. Pe emulator prima citire a căzut cu „Waiting for the text
// optional module to be downloaded” (16.09.2026) — la rampă, fără semnal, prima poză a unui magazioner
// n-ar fi fost citită deloc. `com.google.mlkit:text-recognition` adaugă modelul în aplicație (~11 MB pe
// arm64) și descriptorul lui local, pe care ML Kit îl preferă. **Nu se exclude** artefactul Play Services:
// cel legat depinde de el pentru clasele API (`TextRecognizerOptions`) — o excludere dă NoClassDefFoundError.
const { withAppBuildGradle } = require("expo/config-plugins");

const MARK = "// mlkit-bundled";

module.exports = function withMlkitBundled(config) {
  return withAppBuildGradle(config, (cfg) => {
    if (cfg.modResults.contents.includes(MARK)) return cfg;
    cfg.modResults.contents += `
${MARK}
dependencies {
    implementation "com.google.mlkit:text-recognition:16.0.1"
}
`;
    return cfg;
  });
};
