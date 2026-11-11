/**
 * Breakpoints are rarely round (2 actions in cycle 0 = 133.333... SPD).
 * Rounding up to 2 decimals gives a value that really reaches the breakpoint.
 */
export function formatBreakpoint(spd: number): string {
  return (Math.ceil(spd * 100) / 100).toFixed(2);
}
