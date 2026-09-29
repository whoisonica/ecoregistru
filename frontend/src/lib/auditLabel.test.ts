import assert from "node:assert/strict";
import { test } from "node:test";
import { auditRowLabel } from "./auditLabel";
import { strings } from "./strings";

test("Istoric: termenul și cifrele de ambalaje se citesc pe românește", () => {
  assert.equal(
    auditRowLabel("ReportingDeadline", "SIM_ANNUAL · 2027-03-15"),
    `${strings.enums.reportType.SIM_ANNUAL} · 15.03.2027`
  );
  assert.equal(
    auditRowLabel("PackagingMarketEntry", "2026 · HARTIE_CARTON"),
    `2026 · ${strings.enums.packagingMaterial.HARTIE_CARTON}`
  );
  // Restul etichetelor trec neatinse.
  assert.equal(auditRowLabel("Partner", "Reciclare SRL · RO123"), "Reciclare SRL · RO123");
  assert.equal(auditRowLabel("ReportingDeadline", null), "");
});
