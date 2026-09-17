import assert from "node:assert/strict";
import { test } from "node:test";
import { amountDue, amountForBank, formatIban, isOverdue, looksLikeEmail, transferReference } from "@/lib/billing";
import type { BillingAccount } from "@/lib/types";

type Invoice = BillingAccount["invoices"][number];

const TODAY = "2026-09-17";
const invoice = (patch: Partial<Invoice>): Invoice => ({
  id: "i", periodStart: "2026-09-01", periodEnd: "2026-09-30", total: 149, status: "ISSUED",
  dueDate: "2026-09-27", fgoSerie: "WH", fgoNumar: "14", fgoLink: null, fgoLinkPlata: null,
  paidAt: null, paidBy: null, lastCardError: null, paymentCheckedAt: null, ...patch,
});

test("totul plătit: nimic de plată, nicio scadență", () => {
  const due = amountDue([invoice({ status: "PAID" })], TODAY);
  assert.equal(due.state, "PAID_UP");
  assert.equal(due.total, 0);
  assert.equal(due.dueDate, null);
});

test("două facturi neplătite se adună; scadența e cea mai veche, iar restanța se numără de la ea", () => {
  const due = amountDue(
    [invoice({ id: "b", fgoNumar: "15", total: 149, dueDate: "2026-10-27" }), invoice({ id: "a", total: 389.1, dueDate: "2026-09-12" })],
    TODAY,
  );
  assert.equal(due.state, "OVERDUE");
  assert.equal(due.total, 538.1);
  assert.equal(due.dueDate, "2026-09-12");
  assert.equal(due.overdueDays, 5);
  assert.equal(transferReference(due.unpaid), "WH 14, WH 15");
});

test("scadența de azi nu e încă restanță; o ciornă nu e de plată", () => {
  assert.equal(amountDue([invoice({ dueDate: TODAY })], TODAY).state, "DUE");
  assert.equal(isOverdue(invoice({ dueDate: TODAY }), TODAY), false);
  assert.equal(isOverdue(invoice({ dueDate: "2026-09-16" }), TODAY), true);
  assert.equal(amountDue([invoice({ status: "DRAFT" })], TODAY).state, "PAID_UP");
});

test("banii se adună în bani, nu cu erori de virgulă mobilă", () => {
  assert.equal(amountDue([invoice({ total: 0.1 }), invoice({ total: 0.2 })], TODAY).total, 0.3);
  assert.equal(amountForBank(538.1), "538,10");
});

test("IBAN-ul în grupuri de patru, oricum ar fi scris", () => {
  assert.equal(formatIban("RO32RZBR0000060027995375"), "RO32 RZBR 0000 0600 2799 5375");
  assert.equal(formatIban("RO32 RZBR 0000 0600 2799 5375"), "RO32 RZBR 0000 0600 2799 5375");
});

test("emailul", () => {
  assert.equal(looksLikeEmail(" contabil@firma.ro "), true);
  assert.equal(looksLikeEmail("contabil@firma"), false);
  assert.equal(looksLikeEmail("nu e mail"), false);
});
