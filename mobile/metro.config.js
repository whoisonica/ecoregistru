// Textele și tipurile vin direct din `frontend/src/lib` (todo-mobil §5): două fișiere fără niciun `import`,
// deci Metro trebuie doar să vadă folderul. Aliasul `@web/*` e în tsconfig.json.
const path = require("path");
const { getDefaultConfig } = require("expo/metro-config");

const config = getDefaultConfig(__dirname);
config.watchFolders = [path.resolve(__dirname, "../frontend/src/lib")];

module.exports = config;
