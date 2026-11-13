import { useState } from "react";
import { Link } from "react-router-dom";
import { Search, Sparkles } from "lucide-react";
import Modal from "../Modal";
import { ELEMENT, PATH, ROLES } from "../../util/game";
import { filterCharacters, EMPTY_FILTERS, toggleIn } from "../../util/filters";
import { pickBlocker } from "../../util/team";

const SUGGESTION_LIMIT = 6;

/** Chooses who sits in one slot. Shows your characters by default and the best partners first. */
const CharacterPicker = ({ position, draft, characters, charactersById, owned, suggestions, onPick, onClose }) => {
  const ownedCount = Object.keys(owned).length;
  const [onlyOwned, setOnlyOwned] = useState(ownedCount > 0);
  const [search, setSearch] = useState("");
  const [roles, setRoles] = useState([]);

  const filters = { ...EMPTY_FILTERS, search, roles, ownership: onlyOwned ? "owned" : "all" };
  const visible = filterCharacters(characters, filters, owned);
  const visibleIds = new Set(visible.map((c) => c.id));

  const recommended = suggestions
    .filter((s) => visibleIds.has(s.id) && !pickBlocker(charactersById[s.id], draft, position, charactersById))
    .slice(0, SUGGESTION_LIMIT)
    .map((s) => ({ char: charactersById[s.id], score: s.score }));

  return (
    <Modal title={`Slot ${position + 1}: choose a character`} onClose={onClose}>
      <div className="flex flex-wrap items-center gap-2 mb-3">
        <label className="relative flex-1 min-w-40">
          <span className="sr-only">Search by name</span>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" aria-hidden="true" />
          <input type="search" autoFocus value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search by name..."
            className="w-full pl-9 pr-3 py-2 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan" />
        </label>
        <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
          <input type="checkbox" checked={onlyOwned} onChange={(e) => setOnlyOwned(e.target.checked)} className="accent-[#1F2A44]" />
          Only my characters ({ownedCount})
        </label>
      </div>

      <div className="flex flex-wrap gap-1.5 mb-4" role="group" aria-label="Role">
        {ROLES.map((role) => {
          const active = roles.includes(role.name);
          return (
            <button key={role.name} type="button" aria-pressed={active} onClick={() => setRoles(toggleIn(roles, role.name))}
              className={`px-2.5 py-1 rounded-lg text-xs border transition-colors ${active
                ? "bg-[#FFE68F] border-[#E3C552] text-text-primary font-semibold"
                : "bg-bg-card border-border text-text-secondary hover:text-text-primary"}`}>
              {role.label}
            </button>
          );
        })}
      </div>

      {onlyOwned && ownedCount === 0 && (
        <p className="mb-4 text-sm text-text-secondary">
          You haven't marked any characters yet. <Link to="/roster" className="text-accent-cyan underline underline-offset-2">Go to My characters</Link> or untick the box.
        </p>
      )}

      {recommended.length > 0 && (
        <section className="mb-5" aria-labelledby="picker-recommended">
          <h3 id="picker-recommended" className="flex items-center gap-1.5 text-sm font-semibold text-text-primary mb-2">
            <Sparkles size={16} className="text-accent-gold" /> Best with this team
          </h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {recommended.map(({ char, score }) => (
              <PickerTile key={char.id} char={char} eidolon={owned[String(char.id)]} score={score} onClick={() => onPick(char)} />
            ))}
          </div>
        </section>
      )}

      <h3 className="text-sm font-semibold text-text-primary mb-2">{onlyOwned ? "My characters" : "All characters"} ({visible.length})</h3>
      {visible.length === 0 ? (
        <p className="py-6 text-center font-hand text-2xl text-text-secondary">nobody matches</p>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
          {visible.map((char) => (
            <PickerTile key={char.id} char={char} eidolon={owned[String(char.id)]}
              blocker={pickBlocker(char, draft, position, charactersById)} onClick={() => onPick(char)} />
          ))}
        </div>
      )}
    </Modal>
  );
};

const PickerTile = ({ char, eidolon, score, blocker, onClick }) => (
  <button type="button" onClick={onClick} disabled={Boolean(blocker)} title={blocker ?? undefined}
    className="flex items-center gap-2 p-2 rounded-lg border border-border bg-bg-card text-left hover:border-accent-cyan/50 hover:bg-bg-secondary disabled:opacity-40 disabled:cursor-not-allowed">
    {char.miniIconUrl
      ? <img src={char.miniIconUrl} alt="" loading="lazy" className="w-10 h-10 rounded-full bg-bg-elevated object-cover shrink-0" />
      : <span className="w-10 h-10 rounded-full bg-bg-elevated shrink-0" />}
    <span className="min-w-0">
      <span className="block text-sm font-semibold text-text-primary leading-tight truncate">{char.name}</span>
      <span className="flex items-center gap-1 mt-0.5 text-xs text-text-muted">
        {ELEMENT[char.element] && <img src={ELEMENT[char.element].icon} alt="" className="w-3.5 h-3.5" />}
        {PATH[char.path] && <img src={PATH[char.path].icon} alt="" className="w-3.5 h-3.5 invert opacity-70" />}
        {eidolon !== undefined && <span className="font-semibold text-text-secondary">E{eidolon}</span>}
        {score !== undefined && <span className="text-accent-gold font-semibold">+{score}</span>}
        {blocker && <span className="truncate">{blocker}</span>}
      </span>
    </span>
  </button>
);

export default CharacterPicker;
