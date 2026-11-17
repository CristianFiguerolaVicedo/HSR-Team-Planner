package com.crifigvic.hsrplanner.team;

/** Who may change a saved team. Pure Java so the rule is unit tested on its own. */
public final class TeamPermissions {

    /** Teams one account can keep; protects the database from a script creating teams in a loop. */
    public static final int MAX_TEAMS_PER_USER = 200;

    private TeamPermissions() {
    }

    /**
     * Only the owner, and only of a team that has one: teams saved without an account can't be
     * changed by anybody (otherwise the first person to log in could take over every old link).
     */
    public static boolean canModify(Long ownerId, Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }
}
