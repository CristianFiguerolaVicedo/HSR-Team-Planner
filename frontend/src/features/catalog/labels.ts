import type { Path, Role } from "../../api/types";

export const roleLabels: Record<Role, string> = {
  MAIN_DPS: "Main DPS",
  SUB_DPS: "Sub-DPS",
  AMPLIFIER: "Amplifier",
  SUSTAIN: "Sustain",
};

export const pathLabels: Record<Path, string> = {
  DESTRUCTION: "Destruction",
  HUNT: "The Hunt",
  ERUDITION: "Erudition",
  HARMONY: "Harmony",
  NIHILITY: "Nihility",
  PRESERVATION: "Preservation",
  ABUNDANCE: "Abundance",
  REMEMBRANCE: "Remembrance",
  ELATION: "Elation",
};
