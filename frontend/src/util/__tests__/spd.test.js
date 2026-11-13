import { test } from "node:test";
import assert from "node:assert/strict";
import { actionsLabel, formatBreakpoint, formatMissing } from "../spd.js";

test("breakpoints round up so the shown value really reaches them", () => {
  assert.equal(formatBreakpoint(133.33333333), "133.34");
  assert.equal(formatBreakpoint(160), "160.00");
  assert.equal(formatBreakpoint(142.857142), "142.86");
});

test("missing SPD has a plus sign", () => {
  assert.equal(formatMissing(13.3333), "+13.34");
});

test("actions are pluralised", () => {
  assert.equal(actionsLabel(1), "1 action");
  assert.equal(actionsLabel(2), "2 actions");
  assert.equal(actionsLabel(0), "0 actions");
});
