import { test } from "node:test";
import assert from "node:assert/strict";
import { applyRosterChanges, clampEidolon, localExtras, parseRoster, sameRoster } from "../roster.js";

test("parseRoster reads a saved roster", () => {
  assert.deepEqual(parseRoster('{"1102": 2, "1306": 0}'), { 1102: 2, 1306: 0 });
});

test("parseRoster turns missing or broken data into an empty roster", () => {
  assert.deepEqual(parseRoster(null), {});
  assert.deepEqual(parseRoster("not json"), {});
  assert.deepEqual(parseRoster("[1102, 1306]"), {});
  assert.deepEqual(parseRoster('"text"'), {});
});

test("parseRoster drops ids that aren't numbers and clamps eidolons", () => {
  assert.deepEqual(parseRoster('{"1102": 9, "abc": 1, "1306": -2}'), { 1102: 6, 1306: 0 });
});

test("clampEidolon keeps values between E0 and E6", () => {
  assert.equal(clampEidolon(3), 3);
  assert.equal(clampEidolon(2.6), 3);
  assert.equal(clampEidolon(10), 6);
  assert.equal(clampEidolon(-1), 0);
  assert.equal(clampEidolon("nope"), 0);
});

test("applyRosterChanges adds, updates and removes without touching the original", () => {
  const before = { 1102: 0, 1306: 1 };
  const after = applyRosterChanges(before, { 1102: 2, 1306: null, 1006: 0 });

  assert.deepEqual(after, { 1102: 2, 1006: 0 });
  assert.deepEqual(before, { 1102: 0, 1306: 1 });
});

test("localExtras finds what the browser has and the account doesn't", () => {
  const local = { 1102: 2, 1306: 0, 1208: 1 };
  const account = { 1102: 3, 1306: 0, 1006: 0 };
  // 1102 is higher in the account (kept), 1306 is equal, 1208 is missing
  assert.deepEqual(localExtras(local, account), { 1208: 1 });
  assert.deepEqual(localExtras({ 1102: 4 }, { 1102: 1 }), { 1102: 4 });
  assert.deepEqual(localExtras({}, account), {});
});

test("sameRoster compares characters and Eidolons", () => {
  assert.equal(sameRoster({ 1: 0, 2: 1 }, { 2: 1, 1: 0 }), true);
  assert.equal(sameRoster({ 1: 0 }, { 1: 1 }), false);
  assert.equal(sameRoster({ 1: 0 }, { 1: 0, 2: 0 }), false);
});
