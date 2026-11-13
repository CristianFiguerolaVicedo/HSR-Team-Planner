import { test } from "node:test";
import assert from "node:assert/strict";
import { actionTimes, actionsLabel, breakpointTable, cycleEndAv, formatBreakpoint, formatMissing } from "../spd.js";

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

test("cycles last 150 AV and then 100 more each", () => {
  assert.equal(cycleEndAv(0), 150);
  assert.equal(cycleEndAv(2), 350);
});

test("breakpoint table has the usual community values", () => {
  const rows = breakpointTable(3);
  const find = (cycle, actions) => rows.find((r) => r.cycle === cycle && r.actions === actions)?.spd;
  assert.equal(formatBreakpoint(find(0, 2)), "133.34");
  assert.equal(find(1, 3), 120);
  assert.equal(find(1, 4), 160);
  assert.equal(formatBreakpoint(find(2, 5)), "142.86");
  assert.equal(formatBreakpoint(find(2, 6)), "171.43");
  assert.ok(rows.every((r) => r.spd >= 100 && r.spd <= 200 + 1e-9));
});

test("action times step by the action value and include the cycle edge", () => {
  assert.deepEqual(actionTimes(75, 150), [75, 150]);
  assert.deepEqual(actionTimes(100, 250), [100, 200]);
});
