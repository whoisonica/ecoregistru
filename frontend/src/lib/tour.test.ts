import assert from "node:assert/strict";
import { test } from "node:test";
import type { Role } from "@/auth/AuthContext";
import { hasTour, nextVisibleIndex, shouldAutoStart, tourSteps } from "@/lib/tour";
import { strings } from "@/lib/strings";

const t = strings.tour;
const ALL_ROLES: Role[] = ["PLATFORM_ADMIN", "CONSULTANT", "ADMIN", "OPERATOR", "SCALE_OPERATOR", "CLIENT_VIEWER"];

test("adminul are tot turul, de la bun venit la sfârșit, cu echipa la Setări", () => {
  const steps = tourSteps({ role: "ADMIN", companyType: "GENERATOR" });
  assert.equal(steps[0].id, "welcome");
  assert.equal(steps[steps.length - 1].id, "end");
  assert.ok(steps.every((s) => s.title && s.body));
  const movements = steps.find((s) => s.id === "movements")!;
  assert.equal(movements.target, "add-waste");
  // Fără buton (abonament în doar-citire) pasul rămâne, cu caseta în mijloc.
  assert.equal(movements.requiresTarget, false);
  assert.equal(movements.body, t.movementsBody);
  assert.equal(steps.find((s) => s.id === "settings")!.body, t.settingsBody);
});

test("operatorul scrie predări, dar la Setări nu are echipa", () => {
  const steps = tourSteps({ role: "OPERATOR", companyType: "GENERATOR" });
  assert.equal(steps.find((s) => s.id === "movements")!.target, "add-waste");
  assert.equal(steps.find((s) => s.id === "settings")!.body, t.settingsBodyNoTeam);
});

test("cine doar vede are lista predărilor, nu butonul de adăugat, și textul lui", () => {
  const steps = tourSteps({ role: "CLIENT_VIEWER", companyType: "GENERATOR" });
  const movements = steps.find((s) => s.id === "movements")!;
  assert.equal(movements.target, "movements-list");
  assert.equal(movements.requiresTarget, false);
  assert.equal(movements.body, t.movementsBodyViewer);
  assert.equal(steps.find((s) => s.id === "settings")!.body, t.settingsBodyNoTeam);
  assert.equal(steps.some((s) => s.target === "add-waste"), false);
});

test("aceiași pași, în aceeași ordine, pentru fiecare rol", () => {
  const ids = tourSteps({ role: "ADMIN", companyType: "GENERATOR" }).map((s) => s.id);
  for (const role of ALL_ROLES) {
    assert.deepEqual(tourSteps({ role, companyType: "BOTH" }).map((s) => s.id), ids, role);
  }
});

test("turul e al firmei cu „Generare”: generator și generator-colector, nu colectorul pur", () => {
  assert.equal(hasTour("GENERATOR"), true);
  assert.equal(hasTour("BOTH"), true);
  assert.equal(hasTour("COLLECTOR"), false);
  assert.equal(hasTour(undefined), false);
  assert.deepEqual(tourSteps({ role: "ADMIN", companyType: "COLLECTOR" }), []);
  assert.deepEqual(tourSteps({ role: "ADMIN", companyType: undefined }), []);
  assert.ok(tourSteps({ role: "ADMIN", companyType: "BOTH" }).length > 0);
});

test("pornește singur doar la oamenii firmei care n-au văzut turul", () => {
  for (const role of ["ADMIN", "OPERATOR", "CLIENT_VIEWER"] as Role[]) {
    assert.equal(shouldAutoStart({ role, tourSeen: false }, "GENERATOR"), true, role);
    assert.equal(shouldAutoStart({ role, tourSeen: false }, "BOTH"), true, role);
    assert.equal(shouldAutoStart({ role, tourSeen: true }, "GENERATOR"), false, role);
    // O sesiune salvată înainte de tur nu are câmpul: înseamnă „văzut”.
    assert.equal(shouldAutoStart({ role }, "GENERATOR"), false, role);
    assert.equal(shouldAutoStart({ role, tourSeen: false }, "COLLECTOR"), false, role);
    assert.equal(shouldAutoStart({ role, tourSeen: false }, undefined), false, role);
  }
  for (const role of ["CONSULTANT", "PLATFORM_ADMIN", "SCALE_OPERATOR"] as Role[]) {
    assert.equal(shouldAutoStart({ role, tourSeen: false }, "GENERATOR"), false, role);
  }
  assert.equal(shouldAutoStart(null, "GENERATOR"), false);
  assert.equal(shouldAutoStart(undefined, "GENERATOR"), false);
});

test("pe telefon pașii „numai pe ecran lat” se sar, în ambele direcții", () => {
  const steps = tourSteps({ role: "ADMIN", companyType: "GENERATOR" });
  const at = (id: string) => steps.findIndex((s) => s.id === id);
  const navIndex = at("nav");
  const searchIndex = at("search");
  assert.ok(steps[navIndex].desktopOnly && steps[searchIndex].desktopOnly);

  // Înainte, din „Bun venit”: lat → meniul; îngust → primii pași.
  assert.equal(nextVisibleIndex(steps, at("welcome"), 1, true), navIndex);
  assert.equal(nextVisibleIndex(steps, at("welcome"), 1, false), at("first-steps"));
  // Înapoi, din primii pași: îngust → „Bun venit”.
  assert.equal(nextVisibleIndex(steps, at("first-steps"), -1, false), at("welcome"));
  // Căutarea, penultima: pe telefon din Setări se ajunge direct la sfârșit.
  assert.equal(nextVisibleIndex(steps, at("settings"), 1, false), at("end"));
  assert.equal(nextVisibleIndex(steps, at("settings"), 1, true), searchIndex);
  // Margini: nimic înainte de primul pas, nimic după ultimul.
  assert.equal(nextVisibleIndex(steps, 0, -1, true), null);
  assert.equal(nextVisibleIndex(steps, steps.length - 1, 1, true), null);
  // De la -1 înainte = primul pas care se arată.
  assert.equal(nextVisibleIndex(steps, -1, 1, false), 0);
});

test("pe telefon numărătoarea are doi pași mai puțin", () => {
  const steps = tourSteps({ role: "ADMIN", companyType: "GENERATOR" });
  const narrow = steps.filter((s) => !s.desktopOnly);
  assert.equal(narrow.length, steps.length - 2);
});
