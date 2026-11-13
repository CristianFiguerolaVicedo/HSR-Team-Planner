// Formatting for the SPD analysis the backend returns (GET /spd, POST /spd/team, GET /teams/{code}).

/** Breakpoints are rarely round (133.333…): round UP so the value shown really reaches it. */
export const formatBreakpoint = (spd) => (Math.ceil(spd * 100) / 100).toFixed(2);

/** "+13.34" for the SPD still missing. */
export const formatMissing = (spd) => `+${formatBreakpoint(spd)}`;

/** "2 actions" / "1 action" */
export const actionsLabel = (n) => `${n} action${n === 1 ? "" : "s"}`;
