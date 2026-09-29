import assert from "node:assert/strict";
import { test } from "node:test";
import { clampPage } from "@/lib/pagination";

/**
 * 29.09.2026: unsprezece mișcări, pagina a doua, se șterge singurul ei rând — serverul spune o singură
 * pagină, iar tabelul rămânea pe a doua: ecran gol, „Nicio mișcare în <lună>”, paginarea dispărută.
 */
test("după ștergerea singurului rând de pe ultima pagină, tabelul se întoarce pe pagina de dinainte", () => {
  assert.equal(clampPage(1, 1), 0);
  assert.equal(clampPage(4, 2), 1);
});

test("pagina care încă există rămâne; un tabel golit cade pe prima", () => {
  assert.equal(clampPage(1, 3), 1);
  assert.equal(clampPage(0, 1), 0);
  assert.equal(clampPage(2, 0), 0);
});
