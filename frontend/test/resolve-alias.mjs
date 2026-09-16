import fs from "node:fs";
import { fileURLToPath, pathToFileURL } from "node:url";

const SRC = new URL("../src/", import.meta.url);

export async function resolve(specifier, context, next) {
  let target = null;
  if (specifier.startsWith("@/")) target = new URL(specifier.slice(2), SRC);
  else if (specifier.startsWith(".") && context.parentURL?.startsWith("file:")) {
    target = new URL(specifier, context.parentURL);
  }
  if (target && !/\.[cm]?[jt]sx?$/.test(target.pathname)) {
    const withTs = fileURLToPath(target) + ".ts";
    if (fs.existsSync(withTs)) return next(pathToFileURL(withTs).href, context);
  }
  return next(target ? target.href : specifier, context);
}
