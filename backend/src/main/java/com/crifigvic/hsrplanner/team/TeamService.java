package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.catalog.*;
import com.crifigvic.hsrplanner.catalog.Character;
import com.crifigvic.hsrplanner.catalog.dto.CharacterDto;
import com.crifigvic.hsrplanner.catalog.dto.LightConeDto;
import com.crifigvic.hsrplanner.common.NotFoundException;
import com.crifigvic.hsrplanner.spd.SpdCalculator;
import com.crifigvic.hsrplanner.team.dto.TeamRequest;
import com.crifigvic.hsrplanner.team.dto.TeamResponse;
import com.crifigvic.hsrplanner.team.dto.TeamSlotRequest;
import com.crifigvic.hsrplanner.team.dto.TeamSlotResponse;
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

    @Transactional
    public TeamResponse create(TeamRequest request) {
        Resolved resolved = resolve(request);
        TeamValidationResult validation =
                validator.validate(resolved.candidates(), synergyLookups.within(resolved.characters().keySet()));
        if (!validation.valid()) {
            throw new InvalidTeamException(validation);
        }

        Team team = Team.builder()
                .shareCode(uniqueShareCode())
                .name(request.name().strip())
                .gameMode(request.gameMode())
                .notes(request.notes())
                .createdAt(Instant.now(clock))
                .build();
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
        return toResponse(teams.save(team), validation);
    }

    @Transactional(readOnly = true)
    public TeamResponse get(String shareCode) {
        if (!ShareCodeGenerator.isWellFormed(shareCode)) {
            throw new NotFoundException("Team " + shareCode + " not found");
        }
        Team team = teams.findByShareCode(shareCode)
                .orElseThrow(() -> new NotFoundException("Team " + shareCode + " not found"));

        List<SlotCandidate> candidates = team.getSlots().stream()
                .map(s -> candidate(s.getPosition(), s.getCharacter(), s.getLightCone()))
                .toList();
        Set<Integer> ids = candidates.stream().map(SlotCandidate::characterId).collect(Collectors.toSet());
        // Recomputed on read: synergies and roles may have improved since the team was saved
        return toResponse(team, validator.validate(candidates, synergyLookups.within(ids)));
    }

    // --- helpers -------------------------------------------------------------------------------

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

    private TeamResponse toResponse(Team team, TeamValidationResult validation) {
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
        return new TeamResponse(team.getShareCode(), team.getName(), team.getGameMode(), team.getNotes(),
                team.getCreatedAt(), slots, validation);
    }
}
