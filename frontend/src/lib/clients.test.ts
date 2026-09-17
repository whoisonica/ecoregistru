import assert from "node:assert/strict";
import { test } from "node:test";
import { byAttention, clientRow, MATCHES } from "@/lib/clients";
import type { ClientOverview, Company } from "@/lib/types";

const TODAY = "2026-09-17";
// 18547290 are cifra de control bună (cheia 753217532).
const company: Company = {
  id: "c1", name: "Brutăria SRL", cui: "RO18547290", type: "GENERATOR", active: true,
  address: "Cluj", caenCode: "1071", wasteManagerName: "Popescu",
};
const overview: ClientOverview = {
  companyId: "c1", subscriptionStatus: "ACTIVE", plan: "GENERATOR", monthlyPrice: 99, userCount: 2,
  lastInvoice: { number: "WH 12", total: 99, status: "PAID", dueDate: "2026-09-10", lastError: null },
};
const invoice = (patch: Partial<NonNullable<ClientOverview["lastInvoice"]>>) => ({
  ...overview, lastInvoice: { ...overview.lastInvoice!, ...patch },
});

test("un client plătit, cu oameni și fișa completă nu cere nimic", () => {
  const r = clientRow(company, overview, TODAY);
  assert.deepEqual(r.reasons, []);
  assert.deepEqual(r.gaps, []);
  assert.equal(r.cuiInvalid, false);
});

test("emiterea căzută, restanța și lipsa oamenilor cer atenție", () => {
  assert.deepEqual(clientRow(company, invoice({ status: "DRAFT", number: null, lastError: "CUI" }), TODAY).reasons, ["FAILED"]);
  const late = clientRow(company, invoice({ status: "ISSUED", dueDate: "2026-09-11" }), TODAY);
  assert.deepEqual(late.reasons, ["OVERDUE"]);
  assert.equal(late.overdueDays, 6);
  assert.deepEqual(clientRow(company, { ...overview, userCount: 0 }, TODAY).reasons, ["NO_USERS"]);
});

test("scadența de azi nu e încă restanță; o ciornă fără eroare nu e căzută", () => {
  assert.deepEqual(clientRow(company, invoice({ status: "ISSUED", dueDate: TODAY }), TODAY).reasons, []);
  assert.deepEqual(clientRow(company, invoice({ status: "DRAFT", number: null }), TODAY).reasons, []);
});

test("fără rândul de abonamente nu se deduce nimic: nici „fără abonament”, nici „fără utilizatori”", () => {
  const r = clientRow(company, undefined, TODAY);
  assert.equal(r.noSubscription, false);
  assert.equal(r.noUsers, false);
  assert.deepEqual(r.reasons, []);
});

test("firma de cabinet nu e „fără abonament”; firma inactivă nu cere atenție", () => {
  const none = { ...overview, subscriptionStatus: null, plan: null, monthlyPrice: null, lastInvoice: null };
  assert.equal(clientRow(company, none, TODAY).noSubscription, true);
  assert.equal(clientRow({ ...company, consultancyId: "k" }, none, TODAY).noSubscription, false);
  assert.deepEqual(clientRow({ ...company, active: false }, { ...none, userCount: 0 }, TODAY).reasons, []);
});

test("fișa numără rubricile goale, iar CUI-ul greșit se vede separat", () => {
  const r = clientRow({ ...company, cui: "12345678", caenCode: " ", wasteManagerName: null }, overview, TODAY);
  assert.deepEqual(r.gaps, ["caenCode", "wasteManagerName"]);
  assert.equal(r.cuiInvalid, true);
});

test("filtrele și ordinea: cele cu probleme primele, apoi după nume", () => {
  const ok = clientRow({ ...company, id: "a", name: "Alfa" }, overview, TODAY);
  const bad = clientRow({ ...company, id: "z", name: "Zeta" }, { ...overview, userCount: 0 }, TODAY);
  assert.deepEqual([ok, bad].sort(byAttention).map((r) => r.company.name), ["Zeta", "Alfa"]);
  assert.equal(MATCHES.ATTENTION(bad), true);
  assert.equal(MATCHES.NO_USERS(ok), false);
  assert.equal(MATCHES.CABINETS(clientRow({ ...company, consultancyId: "k" }, overview, TODAY)), true);
});
