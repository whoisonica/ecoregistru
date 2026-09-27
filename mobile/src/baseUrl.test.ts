// Serverul la care vorbește aplicația: `npm test`.
import assert from "node:assert/strict";
import { test } from "node:test";

import { apiBaseUrl } from "./baseUrl.ts";

const PROD = "https://ecoregistru-api.example";

test("în lucru se folosește serverul de pe Mac din .env.local", () => {
  assert.equal(apiBaseUrl("http://localhost:8096", true, PROD), "http://localhost:8096");
});

test("un build Release nu vorbește niciodată cu un server fără https (.env.local uitat la build)", () => {
  assert.equal(apiBaseUrl("http://localhost:8096", false, PROD), PROD);
  assert.equal(apiBaseUrl("http://192.168.1.20:8096", false, PROD), PROD);
});

test("fără nimic setat, producția", () => {
  assert.equal(apiBaseUrl(undefined, false, PROD), PROD);
  assert.equal(apiBaseUrl(undefined, true, PROD), PROD);
  assert.equal(apiBaseUrl("https://alt-server.example", false, PROD), "https://alt-server.example");
});
