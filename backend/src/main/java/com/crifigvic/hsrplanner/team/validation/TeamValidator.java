package com.crifigvic.hsrplanner.team.validation;

import com.crifigvic.hsrplanner.catalog.Role;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Team rules. Errors are things the game itself forbids; warnings and info are advice.
 * Pure Java on purpose: {@code TeamService} turns entities into {@link SlotCandidate}s first.
 */
public class TeamValidator {

    public static final int TEAM_SIZE = 4;

    public TeamValidationResult validate(List<SlotCandidate> slots, SynergyLookup synergies) {
        List<ValidationIssue> issues = new ArrayList<>();

        checkSize(slots, issues);
        checkPositions(slots, issues);
        checkDuplicates(slots, issues);
        checkLightCones(slots, issues);
        Map<Role, Long> roles = countRoles(slots);
        checkRoles(slots, roles, issues);

        List<TeamValidationResult.SynergyPair> pairs = synergyPairs(slots, synergies);
        int synergyScore = pairs.stream().mapToInt(TeamValidationResult.SynergyPair::score).sum();

        boolean valid = issues.stream().noneMatch(i -> i.severity() == IssueCode.Severity.ERROR);
        return new TeamValidationResult(valid, List.copyOf(issues), roles, pairs, synergyScore);
    }

    private void checkSize(List<SlotCandidate> slots, List<ValidationIssue> issues) {
        if (slots.isEmpty()) {
            issues.add(ValidationIssue.of(IssueCode.EMPTY_TEAM, null, "A team needs at least one character"));
        } else if (slots.size() > TEAM_SIZE) {
            issues.add(ValidationIssue.of(IssueCode.TOO_MANY_SLOTS, null,
                    "A team has at most " + TEAM_SIZE + " characters, got " + slots.size()));
        }
    }

    private void checkPositions(List<SlotCandidate> slots, List<ValidationIssue> issues) {
        Set<Integer> seen = new HashSet<>();
        for (SlotCandidate slot : slots) {
            if (slot.position() < 0 || slot.position() >= TEAM_SIZE) {
                issues.add(ValidationIssue.of(IssueCode.INVALID_POSITION, slot.position(),
                        "Position must be between 0 and " + (TEAM_SIZE - 1)));
            } else if (!seen.add(slot.position())) {
                issues.add(ValidationIssue.of(IssueCode.DUPLICATE_POSITION, slot.position(),
                        "Position " + slot.position() + " is used twice"));
            }
        }
    }

    private void checkDuplicates(List<SlotCandidate> slots, List<ValidationIssue> issues) {
        Set<Integer> characters = new HashSet<>();
        Map<String, String> variantOwners = new HashMap<>();
        for (SlotCandidate slot : slots) {
            if (!characters.add(slot.characterId())) {
                issues.add(ValidationIssue.of(IssueCode.DUPLICATE_CHARACTER, slot.position(),
                        slot.characterName() + " is already in the team"));
                continue;
            }
            if (slot.variantGroup() != null) {
                String owner = variantOwners.putIfAbsent(slot.variantGroup(), slot.characterName());
                if (owner != null) {
                    issues.add(ValidationIssue.of(IssueCode.DUPLICATE_VARIANT, slot.position(),
                            slot.characterName() + " and " + owner + " are the same character"));
                }
            }
        }
    }

    private void checkLightCones(List<SlotCandidate> slots, List<ValidationIssue> issues) {
        for (SlotCandidate slot : slots) {
            if (slot.lightConePath() != null && slot.lightConePath() != slot.path()) {
                issues.add(ValidationIssue.of(IssueCode.LIGHT_CONE_PATH_MISMATCH, slot.position(),
                        slot.lightConeName() + " is a " + slot.lightConePath() + " light cone: its passive won't work on "
                                + slot.characterName()));
            }
        }
    }

    private Map<Role, Long> countRoles(List<SlotCandidate> slots) {
        Map<Role, Long> roles = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            roles.put(role, 0L);
        }
        for (SlotCandidate slot : slots) {
            slot.roles().stream().distinct().forEach(role -> roles.merge(role, 1L, Long::sum));
        }
        return roles;
    }

    private void checkRoles(List<SlotCandidate> slots, Map<Role, Long> roles, List<ValidationIssue> issues) {
        // Role advice only makes sense once the team is full
        if (slots.size() != TEAM_SIZE) {
            return;
        }
        if (roles.get(Role.SUSTAIN) == 0) {
            issues.add(ValidationIssue.of(IssueCode.NO_SUSTAIN, null, "No sustain: the team has no healer or shielder"));
        } else if (roles.get(Role.SUSTAIN) >= 2) {
            issues.add(ValidationIssue.of(IssueCode.SEVERAL_SUSTAINS, null,
                    roles.get(Role.SUSTAIN) + " sustains: safe, but fewer slots for damage"));
        }
        if (roles.get(Role.MAIN_DPS) == 0 && roles.get(Role.SUB_DPS) == 0) {
            issues.add(ValidationIssue.of(IssueCode.NO_DPS, null, "Nobody in the team is a damage dealer"));
        }
    }

    private List<TeamValidationResult.SynergyPair> synergyPairs(List<SlotCandidate> slots, SynergyLookup synergies) {
        List<SlotCandidate> distinct = slots.stream()
                .collect(Collectors.toMap(SlotCandidate::characterId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        List<TeamValidationResult.SynergyPair> pairs = new ArrayList<>();
        for (int i = 0; i < distinct.size(); i++) {
            for (int j = i + 1; j < distinct.size(); j++) {
                SlotCandidate a = distinct.get(i);
                SlotCandidate b = distinct.get(j);
                int score = synergies.score(a.characterId(), b.characterId());
                if (score > 0) {
                    pairs.add(new TeamValidationResult.SynergyPair(
                            a.characterId(), a.characterName(), b.characterId(), b.characterName(), score));
                }
            }
        }
        pairs.sort(Comparator.comparingInt(TeamValidationResult.SynergyPair::score).reversed());
        return pairs;
    }
}
