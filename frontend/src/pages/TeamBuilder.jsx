import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Eraser, Info } from "lucide-react";
import { useCharacters } from "../hooks/useCharacters";
import { useLightcones } from "../hooks/useLightcones";
import { useRoster } from "../hooks/useRoster";
import { useTeamDraft } from "../hooks/useTeamDraft";
import { useDebouncedQuery } from "../hooks/useDebouncedQuery";
import { useTeamSuggestions } from "../hooks/useTeamSuggestions";
import { getTeamSpd, validateTeam } from "../services/plannerService";
import CharacterPicker from "../components/team/CharacterPicker";
import LightconePicker from "../components/team/LightconePicker";
import RecommendationsPanel from "../components/team/RecommendationsPanel";
import SaveTeam from "../components/team/SaveTeam";
import TeamSlot from "../components/team/TeamSlot";
import ValidationPanel from "../components/team/ValidationPanel";
import LoadError from "../components/LoadError";
import SkeletonLoader from "../components/SkeletonLoader";
import { filledSlots, slotsFromRecommendation, toTeamRequest } from "../util/team";

// Module-level so useDebouncedQuery gets the same function every render
const fetchSpd = ({ speeds }) => getTeamSpd(speeds);

const byId = (list) => Object.fromEntries(list.map((item) => [item.id, item]));

/**
 * Build a team of 4: pick characters (yours first), light cones and SPD, with the backend checking
 * the rules and synergies as you go, then save it to get a share link.
 */
const TeamBuilder = () => {
  const { characters, loading, error } = useCharacters();
  const { lightcones } = useLightcones();
  const { owned, eidolon } = useRoster();
  const { draft, setField, updateSlot, placeCharacter, clearSlot, setSlots, reset } = useTeamDraft();
  const [picker, setPicker] = useState(null); // { type: "character" | "lightcone", position }

  const charactersById = useMemo(() => byId(characters), [characters]);
  const lightconesById = useMemo(() => byId(lightcones), [lightcones]);

  // Slots whose character no longer exists (old draft) are left out of everything below
  const memberIds = filledSlots(draft).map((s) => s.characterId).filter((id) => charactersById[id]);
  const knownDraft = { ...draft, slots: draft.slots.map((s) => (s && charactersById[s.characterId] ? s : null)) };
  const request = characters.length ? toTeamRequest(knownDraft) : null;
  const validation = useDebouncedQuery(validateTeam, request);

  const spdPayload = useMemo(() => {
    const entries = draft.slots
      .map((slot, position) => ({ position, spd: slot?.spd }))
      .filter(({ spd }) => Number.isFinite(spd) && spd >= 1 && spd <= 1000);
    return entries.length ? { speeds: entries.map((e) => e.spd), positions: entries.map((e) => e.position) } : null;
  }, [draft.slots]);
  const spd = useDebouncedQuery(fetchSpd, spdPayload);
  const spdByPosition = {};
  if (spd.data && spdPayload) {
    spdPayload.positions.forEach((position, i) => {
      // Only trust an answer that still matches what the slot holds
      if (spd.data[i]?.spd === draft.slots[position]?.spd) spdByPosition[position] = spd.data[i];
    });
  }

  const suggestions = useTeamSuggestions(memberIds);

  if (loading) return <SkeletonLoader type="card" count={4} />;
  if (error) return <LoadError error={error} />;

  const issuesFor = (position) => validation.data?.issues.filter((i) => i.position === position) ?? [];
  const hasDraft = draft.slots.some(Boolean) || draft.name || draft.notes;
  const pickerChar = picker?.type === "lightcone" ? charactersById[draft.slots[picker.position]?.characterId] : null;

  return (
    <div className="text-text-primary">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight">Team builder</h1>
          <p className="font-hand text-2xl text-text-secondary mt-1">four seats on the Express, choose wisely</p>
        </div>
        {hasDraft && (
          <button type="button" onClick={() => { if (window.confirm("Start a new team? The current one will be cleared.")) reset(); }}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm text-text-secondary hover:text-accent-red hover:bg-accent-red-muted">
            <Eraser size={16} /> New team
          </button>
        )}
      </div>

      {Object.keys(owned).length === 0 && (
        <p className="glass-panel p-3 mb-6 flex items-center gap-2 text-sm text-text-secondary">
          <Info size={16} className="text-accent-cyan shrink-0" />
          Tip: mark the characters you have in <Link to="/roster" className="text-accent-cyan underline underline-offset-2">My characters</Link> and the builder will suggest from them first.
        </p>
      )}

      <RecommendationsPanel
        owned={owned}
        gameMode={draft.gameMode}
        onGameModeChange={(mode) => setField("gameMode", mode)}
        memberIds={memberIds}
        onUse={(team) => setSlots(slotsFromRecommendation(team, draft))}
      />

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-x-5 gap-y-8 pt-3 mb-8">
        {draft.slots.map((slot, position) => (
          <TeamSlot
            key={position}
            position={position}
            slot={slot}
            char={slot && charactersById[slot.characterId]}
            lightcone={slot?.lightConeId ? lightconesById[slot.lightConeId] : null}
            spdAnalysis={spdByPosition[position]}
            issues={issuesFor(position)}
            onAdd={() => setPicker({ type: "character", position })}
            onChangeCharacter={() => setPicker({ type: "character", position })}
            onChooseLightcone={() => setPicker({ type: "lightcone", position })}
            onUpdate={(patch) => updateSlot(position, patch)}
            onClear={() => clearSlot(position)}
          />
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_380px] items-start">
        <ValidationPanel validation={validation.data} loading={validation.loading} error={validation.error} empty={!request} />
        <SaveTeam draft={draft} setField={setField} request={request}
          canSave={Boolean(request) && !validation.loading && validation.data?.valid === true} />
      </div>

      {picker?.type === "character" && (
        <CharacterPicker
          position={picker.position}
          draft={draft}
          characters={characters}
          charactersById={charactersById}
          owned={owned}
          suggestions={suggestions}
          onPick={(char) => { placeCharacter(picker.position, char.id, eidolon(char.id)); setPicker(null); }}
          onClose={() => setPicker(null)}
        />
      )}
      {picker?.type === "lightcone" && pickerChar && (
        <LightconePicker
          char={pickerChar}
          lightcones={lightcones}
          currentId={draft.slots[picker.position]?.lightConeId ?? null}
          onPick={(lc) => { updateSlot(picker.position, { lightConeId: lc?.id ?? null }); setPicker(null); }}
          onClose={() => setPicker(null)}
        />
      )}
    </div>
  );
};

export default TeamBuilder;
