import assert from "node:assert/strict";
import { test } from "node:test";
import {
  asksMarketRoles,
  asksTransport,
  mayLackEnvAuth,
  skipsEnvAuth,
  stepOf,
  validate,
  type AccountRequestValues,
} from "@/components/account-request/accountRequestRules";

/** O cerere completă și validă; fiecare probă strică exact o rubrică. */
function complete(): AccountRequestValues {
  return {
    companyName: "Proba SRL",
    // CUI real ca formă (cifra de control), altfel pasul 1 cade din alt motiv decât cel probat.
    cui: "RO14399840",
    companyType: "GENERATOR",
    companyAddress: "Str. Probei 1, Cluj-Napoca",
    caenCode: "1812",
    workPointName: "Sediu",
    workPointAddress: "Str. Probei 1",
    authNumber: "123",
    authExpiry: "2027-01-01",
    noEnvAuth: false,
    transportMeans: "",
    transportLicenseNumber: "",
    transportLicenseExpiry: "",
    contactName: "Ion Popescu",
    contactEmail: "ion@proba.ro",
    contactPhone: "0740000000",
    contactRole: "Administrator",
    marketRoles: ["PRODUCER"],
  };
}

test("o cerere completă nu are nicio greșeală", () => {
  assert.deepEqual(validate(complete()), {});
});

test("tipul firmei decide ce se cere", () => {
  assert.equal(asksTransport("GENERATOR"), false);
  assert.equal(asksTransport("COLLECTOR"), true);
  assert.equal(asksMarketRoles("COLLECTOR"), false);
  assert.equal(asksMarketRoles("GENERATOR"), true);
  assert.equal(mayLackEnvAuth("GENERATOR"), true);
  assert.equal(mayLackEnvAuth("COLLECTOR"), false);
});

test("colectorul trebuie să declare transportul, generatorul nu", () => {
  const collector = { ...complete(), companyType: "COLLECTOR" as const, marketRoles: [] };
  const errs = validate(collector);
  assert.ok(errs.transportMeans, "colectorului i se cere mijlocul de transport");
  assert.ok(errs.transportLicenseNumber);
  // …iar tipul de generator NU i se cere, deși lista lui e goală.
  assert.equal(errs.marketRoles, undefined);
  assert.deepEqual(validate(complete()).transportMeans, undefined);
});

test("bifa „n-avem autorizație” scutește autorizația — și numai la generator", () => {
  const fara = { ...complete(), authNumber: "", authExpiry: "", noEnvAuth: true };
  assert.deepEqual(validate(fara), {});
  assert.equal(skipsEnvAuth(fara), true);
  // Aceeași bifă la un colector nu-l scutește: el are întotdeauna autorizație.
  const colector = { ...fara, companyType: "COLLECTOR" as const };
  assert.equal(skipsEnvAuth(colector), false);
  assert.ok(validate(colector).authNumber);
});

test("CUI-ul se verifică pe cifra de control, nu doar pe gol", () => {
  assert.equal(validate({ ...complete(), cui: "" }).cui !== undefined, true);
  // O cifră schimbată în CUI-ul valabil de mai sus: forma e bună, cifra de control nu.
  assert.ok(validate({ ...complete(), cui: "RO14399841" }).cui);
});

test("emailul cere o formă, nu doar conținut", () => {
  assert.ok(validate({ ...complete(), contactEmail: "" }).contactEmail);
  assert.ok(validate({ ...complete(), contactEmail: "ion.proba.ro" }).contactEmail);
});

test("`only` verifică un singur pas", () => {
  const gol = { ...complete(), companyName: "", contactName: "" };
  assert.ok(validate(gol, 1).companyName);
  assert.equal(validate(gol, 1).contactName, undefined, "pasul 1 nu se plânge de pasul 3");
  assert.ok(validate(gol, 3).contactName);
});

test("greșeala trimite omul înapoi la pasul ei", () => {
  assert.equal(stepOf({ companyName: "x" }), 1);
  assert.equal(stepOf({ workPointName: "x" }), 2);
  assert.equal(stepOf({ transportLicenseExpiry: "x" }), 2);
  assert.equal(stepOf({ contactEmail: "x" }), 3);
  assert.equal(stepOf({ marketRoles: "x" }), 4);
  // Cea mai din față are prioritate: omul e dus la prima rubrică greșită, nu la ultima.
  assert.equal(stepOf({ companyName: "x", contactEmail: "y" }), 1);
});
