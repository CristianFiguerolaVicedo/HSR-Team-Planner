// Formatting for the SPD analysis the backend returns (GET /spd, POST /spd/team, GET /teams/{code}).

/** Breakpoints are rarely round (133.333…): round UP so the value shown really reaches it. */
export const formatBreakpoint = (spd) => (Math.ceil(spd * 100) / 100).toFixed(2);

/** "+13.34" for the SPD still missing. */
export const formatMissing = (spd) => `+${formatBreakpoint(spd)}`;

/** "2 actions" / "1 action" */
export const actionsLabel = (n) => `${n} action${n === 1 ? "" : "s"}`;

/** Total AV elapsed when cycle `cycle` ends: 150, 250, 350… (same constants as the backend's SpdCalculator). */
export const cycleEndAv = (cycle) => 150 + 100 * cycle;

/**
 * Reference rows for the breakpoint table: SPD needed for N actions by the end of each cycle,
 * keeping only the usual range (100–200 SPD). The exact analysis of a unit still comes from the API.
 */
export const breakpointTable = (cycles, minSpd = 100, maxSpd = 200) => {
  const rows = [];
  for (let cycle = 0; cycle < cycles; cycle++) {
    const av = cycleEndAv(cycle);
    for (let actions = 1; actions * 10000 / av <= maxSpd + 1e-9; actions++) {
      const spd = actions * 10000 / av;
      if (spd >= minSpd - 1e-9) rows.push({ cycle, actions, spd });
    }
  }
  return rows;
};

/** AV at which each action of a unit happens, up to `untilAv`. Uses the actionValue the API returned. */
export const actionTimes = (actionValue, untilAv) => {
  const times = [];
  for (let t = actionValue; t <= untilAv + 1e-9; t += actionValue) times.push(t);
  return times;
};
