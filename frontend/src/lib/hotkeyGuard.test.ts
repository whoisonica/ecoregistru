import assert from "node:assert/strict";
import { test } from "node:test";
import { hotkeyAllowed, isTypingTarget, type KeyTarget } from "@/lib/hotkeyGuard";

const el = (tagName: string, attrs: Record<string, string> = {}, isContentEditable = false): KeyTarget => ({
  tagName,
  isContentEditable,
  getAttribute: (name) => attrs[name] ?? null,
});

/**
 * 29.09.2026: cu formularul de mișcare deschis și focusul pe un buton, o cifră schimba ecranul, iar N crea
 * o dublură a mișcării editate. Tastele paginii tac cât e deschis un dialog.
 */
test("cu un dialog deschis, tastele paginii tac", () => {
  assert.equal(hotkeyAllowed({ typing: false, modalOpen: true, whileTyping: false, inDialog: false }), false);
  // Și cele care lucrează din câmpuri de text: paleta se deschide cu Ctrl+K, dar nu peste un formular.
  assert.equal(hotkeyAllowed({ typing: false, modalOpen: true, whileTyping: true, inDialog: false }), false);
});

test("ce e legat pentru dialog lucrează și cu el deschis", () => {
  assert.equal(hotkeyAllowed({ typing: true, modalOpen: true, whileTyping: true, inDialog: true }), true);
  assert.equal(hotkeyAllowed({ typing: false, modalOpen: true, whileTyping: false, inDialog: true }), true);
});

test("fără dialog, regula de dinainte: tasta tace numai în câmpurile de text", () => {
  assert.equal(hotkeyAllowed({ typing: false, modalOpen: false, whileTyping: false, inDialog: false }), true);
  assert.equal(hotkeyAllowed({ typing: true, modalOpen: false, whileTyping: false, inDialog: false }), false);
  assert.equal(hotkeyAllowed({ typing: true, modalOpen: false, whileTyping: true, inDialog: false }), true);
});

test("câmpurile de text: input, textarea, select, contenteditable și comboboxul-buton", () => {
  assert.equal(isTypingTarget(el("INPUT")), true);
  assert.equal(isTypingTarget(el("TEXTAREA")), true);
  assert.equal(isTypingTarget(el("SELECT")), true);
  assert.equal(isTypingTarget(el("DIV", {}, true)), true);
  assert.equal(isTypingTarget(el("BUTTON", { role: "combobox" })), true);
  assert.equal(isTypingTarget(el("BUTTON")), false);
  assert.equal(isTypingTarget(null), false);
});
