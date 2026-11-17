package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.account.AppUser;
import com.crifigvic.hsrplanner.catalog.*;
import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.catalog.dto.LightConeDto;
import com.crifigvic.hsrplanner.common.ForbiddenException;
import com.crifigvic.hsrplanner.common.NotFoundException;
import com.crifigvic.hsrplanner.spd.SpdCalculator;
import com.crifigvic.hsrplanner.team.dto.TeamRequest;
import com.crifigvic.hsrplanner.team.dto.TeamResponse;
import com.crifigvic.hsrplanner.team.dto.TeamSlotRequest;
import com.crifigvic.hsrplanner.team.dto.TeamSlotResponse;
import com.crifigvic.hsrplanner.team.dto.TeamSummary;
import com.crifigvic.hsrplanner.team.validation.SlotCandidate;
import com.crifigvic.hsrplanner.team.validation.SynergyLookup;
import com.crifigvic.hsrplanner.team.validation.TeamValidationResult;
import com.crifigvic.hsrplanner.team.validation.TeamValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    /** Cycles shown in each slot's SPD analysis. */
    static final int SPD_CYCLES = 3;
    private static final int MAX_SHARE_CODE_ATTEMPTS = 5;

    private final TeamRepository teams;
    private final CharacterRepository characters;
    private final LightConeRepository lightCones;
    private final SynergyLookups synergyLookups;
    private final ShareCodeGenerator shareCodes;
    private final Clock clock;

    private final TeamValidator validator = new TeamValidator();
    private final SpdCalculator spdCalculator = new SpdCalculator();

    /** Checks a team without saving it, for live feedback while the user builds it. */
    @Transactional(readOnly = true)
    public TeamValidationResult validate(TeamRequest request) {
        Resolved resolved = resolve(request);
        return validator.validate(resolved.candidates(), synergyLookups.within(resolved.characters().keySet()));
    }

    /** @param owner the logged-in user, or null: then the team is an immutable snapshot */
    @Transactional
    public TeamResponse create(TeamRequest request, AppUser owner) {
        if (owner != null && teams.countByOwnerId(owner.getId()) >= TeamPermissions.MAX_TEAMS_PER_USER) {
            throw new IllegalArgumentException("You can keep up to " + TeamPermissions.MAX_TEAMS_PER_USER
                    + " teams: delete some in My teams first");
        }
        Resolved resolved = resolve(request);
        TeamValidationResult validation = checkValid(resolved);

        Team team = Team.builder()
                .shareCode(uniqueShareCode())
                .createdAt(Instant.now(clock))
                .owner(owner)
                .build();
        fill(team, request, resolved);
        return toResponse(teams.save(team), validation, owner);
    }

    /** Replaces name, mode, notes and slots of an owned team. The share code (the link) stays. */
    @Transactional
    public TeamResponse update(String shareCode, TeamRequest request, AppUser user) {
        Team team = findForChange(shareCode, user);
        Resolved resolved = resolve(request);
        TeamValidationResult validation = checkValid(resolved);

        team.getSlots().clear();
        // Hibernate inserts before it deletes; without this flush the new slot 0 would clash with the
        // old slot 0 on the (team_id, position) unique constraint.
        teams.flush();
        fill(team, request, resolved);
        team.setUpdatedAt(Instant.now(clock));
        return toResponse(team, validation, user);
    }

    @Transactional
    public void delete(String shareCode, AppUser user) {
        teams.delete(findForChange(shareCode, user));
    }

    @Transactional(readOnly = true)
    public List<TeamSummary> listMine(AppUser user) {
        return teams.findAllByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(t -> new TeamSummary(t.getShareCode(), t.getName(), t.getGameMode(), t.getCreatedAt(), t.getUpdatedAt(),
                        t.getSlots().stream()
                                .sorted(Comparator.comparingInt(TeamSlot::getPosition))
                                .map(s -> CharacterDto.from(s.getCharacter()))
                                .toList()))
                .toList();
    }

    /** @param viewer the logged-in user or null; only decides whether the answer says "editable" */
    @Transactional(readOnly = true)
    public TeamResponse get(String shareCode, AppUser viewer) {
        Team team = find(shareCode);

        List<SlotCandidate> candidates = team.getSlots().stream()
                .map(s -> candidate(s.getPosition(), s.getCharacter(), s.getLightCone()))
                .toList();
        Set<Integer> ids = candidates.stream().map(SlotCandidate::characterId).collect(Collectors.toSet());
        // Recomputed on read: synergies and roles may have improved since the team was saved
        return toResponse(team, validator.validate(candidates, synergyLookups.within(ids)), viewer);
    }

    // --- helpers -------------------------------------------------------------------------------

    private Team find(String shareCode) {
        if (!ShareCodeGenerator.isWellFormed(shareCode)) {
            throw new NotFoundException("Team " + shareCode + " not found");
        }
        return teams.findByShareCode(shareCode)
                .orElseThrow(() -> new NotFoundException("Team " + shareCode + " not found"));
    }

    /** The team, if this user may change it; 403 otherwise. */
    private Team findForChange(String shareCode, AppUser user) {
        Team team = find(shareCode);
        if (!TeamPermissions.canModify(team.ownerId(), user.getId())) {
            throw new ForbiddenException("Only the person who saved this team while logged in can change it");
        }
        return team;
    }

    private TeamValidationResult checkValid(Resolved resolved) {
        TeamValidationResult validation =
                validator.validate(resolved.candidates(), synergyLookups.within(resolved.characters().keySet()));
        if (!validation.valid()) {
            throw new InvalidTeamException(validation);
        }
        return validation;
    }

    private static void fill(Team team, TeamRequest request, Resolved resolved) {
        team.setName(request.name().strip());
        team.setGameMode(request.gameMode());
        team.setNotes(request.notes());
        for (TeamSlotRequest slot : request.slots()) {
            team.addSlot(TeamSlot.builder()
                    .position(slot.position())
                    .character(resolved.characters().get(slot.characterId()))
                    .lightCone(slot.lightConeId() == null ? null : resolved.lightCones().get(slot.lightConeId()))
                    .eidolon(slot.eidolon())
                    .superimposition(slot.lightConeId() == null ? null : Objects.requireNonNullElse(slot.superimposition(), 1))
                    .spd(slot.spd())
                    .build());
        }
    }

    private record Resolved(Map<Integer, Character> characters, Map<Long, LightCone> lightCones,
                            List<SlotCandidate> candidates) {
    }

    private Resolved resolve(TeamRequest request) {
        Set<Integer> characterIds = request.slots().stream().map(TeamSlotRequest::characterId).collect(Collectors.toSet());
        Set<Long> lightConeIds = request.slots().stream().map(TeamSlotRequest::lightConeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, Character> foundCharacters = characters.findAllById(characterIds).stream()
                .collect(Collectors.toMap(Character::getId, Function.identity()));
        Map<Long, LightCone> foundLightCones = lightCones.findAllById(lightConeIds).stream()
                .collect(Collectors.toMap(LightCone::getId, Function.identity()));

        characterIds.stream().filter(id -> !foundCharacters.containsKey(id)).findFirst().ifPresent(id -> {
            throw new NotFoundException("Character " + id + " not found");
        });
        lightConeIds.stream().filter(id -> !foundLightCones.containsKey(id)).findFirst().ifPresent(id -> {
            throw new NotFoundException("Light cone " + id + " not found");
        });

        List<SlotCandidate> candidates = request.slots().stream()
                .map(s -> candidate(s.position(), foundCharacters.get(s.characterId()),
                        s.lightConeId() == null ? null : foundLightCones.get(s.lightConeId())))
                .toList();
        return new Resolved(foundCharacters, foundLightCones, candidates);
    }

    private static SlotCandidate candidate(int position, Character c, LightCone lc) {
        return new SlotCandidate(position, c.getId(), c.getName(), c.getPath(), List.copyOf(c.getRoles()), c.getVariantGroup(),
                lc == null ? null : lc.getName(), lc == null ? null : lc.getPath());
    }

    private String uniqueShareCode() {
        for (int attempt = 0; attempt < MAX_SHARE_CODE_ATTEMPTS; attempt++) {
            String code = shareCodes.next();
            if (!teams.existsByShareCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a free share code");
    }

    private TeamResponse toResponse(Team team, TeamValidationResult validation, AppUser viewer) {
        List<TeamSlotResponse> slots = team.getSlots().stream()
                .sorted(Comparator.comparingInt(TeamSlot::getPosition))
                .map(s -> new TeamSlotResponse(
                        s.getPosition(),
                        CharacterDto.from(s.getCharacter()),
                        s.getLightCone() == null ? null : LightConeDto.from(s.getLightCone()),
                        s.getEidolon(),
                        s.getSuperimposition(),
                        s.getSpd(),
                        s.getSpd() == null ? null : spdCalculator.analyze(s.getSpd(), SPD_CYCLES)))
                .toList();
        boolean editable = viewer != null && TeamPermissions.canModify(team.ownerId(), viewer.getId());
        return new TeamResponse(team.getShareCode(), team.getName(), team.getGameMode(), team.getNotes(),
                team.getCreatedAt(), team.getUpdatedAt(), editable, slots, validation);
    }
}
