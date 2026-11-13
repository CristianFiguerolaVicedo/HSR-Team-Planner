import { test } from "node:test";
import assert from "node:assert/strict";
import { compareImport, isValidUid } from "../importer.js";

test("UIDs have 9 or 10 digits and don't start with 0", () => {
  assert.equal(isValidUid("800333171"), true);
  assert.equal(isValidUid(" 1800333171 "), true);
  assert.equal(isValidUid("080033317"), false);
  assert.equal(isValidUid("12345"), false);
  assert.equal(isValidUid("80033317a"), false);
});

test("compareImport marks new, changed and unchanged characters", () => {
  const imported = [
    { character: { id: 1102 }, eidolon: 2, level: 80 },
    { character: { id: 1306 }, eidolon: 0, level: 80 },
    { character: { id: 1208 }, eidolon: 1, level: 80 },
  ];
  const owned = { 1306: 1, 1208: 1, 1006: 0 };

  const { rows, changes, newCount, changedCount } = compareImport(imported, owned);

  assert.deepEqual(rows.map((r) => r.status), ["new", "changed", "same"]);
  assert.equal(rows[1].current, 1);
  assert.deepEqual(changes, { 1102: 2, 1306: 0 });
  assert.equal(newCount, 1);
  assert.equal(changedCount, 1);
});

test("an empty showcase changes nothing", () => {
  assert.deepEqual(compareImport([], { 1102: 0 }).changes, {});
});
