import assert from "node:assert/strict";
import { mock, test } from "node:test";
import { todayIso } from "@/components/movements/movementRules";

// QA de lansare, generator — G16. `todayIso()` dă data implicită a unei mișcări noi
// (MovementFormDialog.tsx:196). E luată din `toISOString()`, adică din UTC: între 00:00 și 03:00,
// ora României, formularul propune ziua de ieri. Oracolul: ziua din calendarul României.

process.env.TZ = "Europe/Bucharest";

function at(instant: string, body: () => void) {
  mock.timers.enable({ apis: ["Date"], now: new Date(instant) });
  try {
    body();
  } finally {
    mock.timers.reset();
  }
}

test("BUG-037: la 00:30 ora României, data implicită e ziua de azi, nu cea de ieri", () => {
  at("2026-09-19T21:30:00Z", () => assert.equal(todayIso(), "2026-09-20"));
});

test("controlul: la prânz, UTC și România sunt în aceeași zi", () => {
  at("2026-09-19T09:00:00Z", () => assert.equal(todayIso(), "2026-09-19"));
});
