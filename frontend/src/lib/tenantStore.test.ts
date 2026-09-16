import assert from "node:assert/strict";
import { test } from "node:test";
import { tabScopedStore } from "@/lib/tenantStore";

function memory() {
  const data = new Map<string, string>();
  return {
    getItem: (k: string) => data.get(k) ?? null,
    setItem: (k: string, v: string) => void data.set(k, v),
    removeItem: (k: string) => void data.delete(k),
  };
}

test("BUG-024: o firmă aleasă în alt tab nu mută cererile tabului deschis", () => {
  const shared = memory();
  const tabA = tabScopedStore("eco_tenant", memory(), shared);
  const tabB = tabScopedStore("eco_tenant", memory(), shared);

  tabA.set("onsia");
  assert.equal(tabB.get(), "onsia", "un tab nou pornește de la ultima alegere");

  tabB.set("ardeal");
  assert.equal(tabA.get(), "onsia", "tabul A rămâne pe firma pe care o arată");
  assert.equal(tabB.get(), "ardeal");
  assert.equal(tabScopedStore("eco_tenant", memory(), shared).get(), "ardeal", "al treilea tab ia ultima alegere");
});

test("BUG-024: un tab care n-a ales încă își fixează firma la prima citire", () => {
  const shared = memory();
  shared.setItem("eco_tenant", "onsia");
  const tabA = tabScopedStore("eco_tenant", memory(), shared);
  assert.equal(tabA.get(), "onsia");

  tabScopedStore("eco_tenant", memory(), shared).set("ardeal");
  assert.equal(tabA.get(), "onsia");
});

test("ieșirea șterge firma și din tab, și din browser", () => {
  const shared = memory();
  const tab = tabScopedStore("eco_tenant", memory(), shared);
  tab.set("onsia");
  tab.clear();
  assert.equal(tab.get(), null);
  assert.equal(shared.getItem("eco_tenant"), null);
});
