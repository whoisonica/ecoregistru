import assert from "node:assert/strict";
import { test } from "node:test";
import { countOf } from "@/lib/count";
import { strings } from "@/lib/strings";

test("numeralul românesc: 1, 2–19, apoi „de” de la 20, reluat la fiecare sută", () => {
  assert.equal(countOf(1, "linie", "linii"), "1 linie");
  assert.equal(countOf(2, "linie", "linii"), "2 linii");
  assert.equal(countOf(19, "linie", "linii"), "19 linii");
  assert.equal(countOf(20, "linie", "linii"), "20 de linii");
  assert.equal(countOf(100, "linie", "linii"), "100 de linii");
  assert.equal(countOf(101, "linie", "linii"), "101 linii");
  assert.equal(countOf(119, "linie", "linii"), "119 linii");
  assert.equal(countOf(120, "linie", "linii"), "120 de linii");
});

/** Decizia 66: adjectivul intră în pereche. 29.09.2026: dosarul scria „1 formular de transport emise”. */
test("Registrul Anexa 3 în dosar: „1 formular … emis”, „2 formulare … emise”, „20 de formulare … emise”", () => {
  const t = strings.auditFile;
  const line = (n: number) => t.anexa3RegisterYes.replace("{count}", countOf(n, t.formIssuedOne, t.formIssuedMany));
  assert.match(line(1), /^1 formular de transport emis în /);
  assert.match(line(2), /^2 formulare de transport emise în /);
  assert.match(line(20), /^20 de formulare de transport emise în /);
});
