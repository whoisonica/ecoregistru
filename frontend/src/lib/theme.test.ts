import assert from "node:assert/strict";
import { test } from "node:test";
import { applyTheme, parseTheme, readTheme, THEME_STORAGE_KEY } from "@/lib/theme";

const memory = (initial: Record<string, string> = {}) => {
  const data = { ...initial };
  return { data, getItem: (k: string) => data[k] ?? null, setItem: (k: string, v: string) => void (data[k] = v) };
};
const html = () => {
  const attrs: Record<string, string> = {};
  return {
    attrs,
    setAttribute: (n: string, v: string) => void (attrs[n] = v),
    removeAttribute: (n: string) => void delete attrs[n],
  };
};

test("o valoare necunoscută sau lipsă e Luminos", () => {
  assert.equal(parseTheme(null), "luminos");
  assert.equal(parseTheme(""), "luminos");
  assert.equal(parseTheme("dark"), "luminos");
  assert.equal(parseTheme("padure"), "padure");
  assert.equal(parseTheme("intunecat"), "intunecat");
});

test("se citește din browser; dacă citirea aruncă, e Luminos", () => {
  assert.equal(readTheme(memory({ [THEME_STORAGE_KEY]: "intunecat" })), "intunecat");
  assert.equal(readTheme(memory()), "luminos");
  const blocked = { getItem: () => { throw new Error("SecurityError"); }, setItem: () => {} };
  assert.equal(readTheme(blocked), "luminos");
});

test("aplicarea pune atributul și ține minte; Luminos scoate atributul", () => {
  const root = html();
  const storage = memory();
  applyTheme("padure", root, storage);
  assert.equal(root.attrs["data-theme"], "padure");
  assert.equal(storage.data[THEME_STORAGE_KEY], "padure");
  applyTheme("luminos", root, storage);
  assert.equal(root.attrs["data-theme"], undefined);
  assert.equal(storage.data[THEME_STORAGE_KEY], "luminos");
});

test("dacă scrierea aruncă, tema se aplică totuși pe pagină", () => {
  const root = html();
  const blocked = { getItem: () => null, setItem: () => { throw new Error("QuotaExceededError"); } };
  applyTheme("intunecat", root, blocked);
  assert.equal(root.attrs["data-theme"], "intunecat");
});
