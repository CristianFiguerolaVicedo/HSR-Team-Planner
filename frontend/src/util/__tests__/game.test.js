import { test } from "node:test";
import assert from "node:assert/strict";
import { roleLabel, rolesOf, tagLabel } from "../game.js";

test("roles come from the list, main role first", () => {
  assert.deepEqual(rolesOf({ role: "SUB_DPS", roles: ["SUB_DPS", "DEBUFFER"] }), ["SUB_DPS", "DEBUFFER"]);
  assert.equal(roleLabel({ role: "SUB_DPS", roles: ["SUB_DPS", "DEBUFFER"] }), "Sub-DPS / Debuffer");
});

test("older answers with a single role still work", () => {
  assert.deepEqual(rolesOf({ role: "SUSTAIN" }), ["SUSTAIN"]);
  assert.equal(roleLabel({ role: "SUSTAIN" }), "Sustain");
  assert.deepEqual(rolesOf({}), []);
});

test("tags read as labels, and characters without tags give an empty label", () => {
  assert.equal(tagLabel({ tags: ["AOE", "BREAK"] }), "AoE / Break");
  assert.equal(tagLabel({ tags: [] }), "");
  assert.equal(tagLabel({}), "");
});
