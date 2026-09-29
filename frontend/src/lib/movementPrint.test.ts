import assert from "node:assert/strict";
import { test } from "node:test";
import { anexa3NumberLabel, canPrintAnexa3, canPrintAviz } from "@/lib/movementPrint";
import type { WasteMovement } from "@/lib/types";

const handover = (extra: Partial<WasteMovement> = {}) =>
  ({
    hazardous: false,
    partnerId: "p1",
    operation: "RECOVERED",
    anexa3Number: null,
    ...extra,
  }) as WasteMovement;

/**
 * A4 (todo-reparatii-2809). În doar-citire, `SubscriptionAccessFilter` lasă toate GET-urile — „Poți vedea și
 * descărca tot” — dar ecranul trecea `useCanWrite()`, care include doar-citirea, și butoanele dispăreau.
 * Avizul tipărește ce e deja salvat: depinde numai de rol.
 */
test("avizul se tipărește și în doar-citire: îl oprește numai rolul", () => {
  assert.equal(canPrintAviz(handover(), true), true);
  assert.equal(canPrintAviz(handover(), false), false);
});

/**
 * Anexa 3 alocă numărul la prima tipărire, deci o primă tipărire rămâne o scriere. Retipărirea păstrează
 * seria și numărul deja alocate: nu scrie nimic nou și rămâne la îndemână în doar-citire.
 */
test("în doar-citire Anexa 3 se retipărește cu numărul ei, dar nu primește număr nou", () => {
  assert.equal(canPrintAnexa3(handover({ anexa3Number: 7 }), true, true), true);
  assert.equal(canPrintAnexa3(handover(), true, true), false);
  assert.equal(canPrintAnexa3(handover(), true, false), true);
  assert.equal(canPrintAnexa3(handover({ anexa3Number: 7 }), false, false), false);
});

test("numărul Anexei 3 emise, cu seria când există", () => {
  assert.equal(anexa3NumberLabel({ anexa3Series: "HMB", anexa3Number: 17 }), "HMB 17");
  assert.equal(anexa3NumberLabel({ anexa3Series: " ", anexa3Number: 17 }), "17");
  assert.equal(anexa3NumberLabel({ anexa3Series: null, anexa3Number: 3 }), "3");
  assert.equal(anexa3NumberLabel({ anexa3Series: "HMB", anexa3Number: null }), null);
});
