import { test } from "node:test";
import assert from "node:assert/strict";
import { formatDate } from "./utils";

test("o zi calendaristică se scrie ca atare", () => {
  assert.equal(formatDate("2026-03-15"), "15.03.2026");
});

// BUG-062: o bifă la 00:30, ora României, e 22:30 UTC în ziua dinainte.
test("un moment se scrie în ziua locală, nu în cea din UTC", () => {
  const before = process.env.TZ;
  process.env.TZ = "Europe/Bucharest";
  try {
    assert.equal(formatDate("2026-03-12T22:30:00Z"), "13.03.2026");
  } finally {
    if (before === undefined) delete process.env.TZ;
    else process.env.TZ = before;
  }
});
