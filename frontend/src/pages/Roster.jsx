import { useMemo, useState } from "react";
import { MinusCircle, PlusCircle, Trash2 } from "lucide-react";
import { useCharacters } from "../hooks/useCharacters";
import { useRoster } from "../hooks/useRoster";
import CharacterCard from "../components/CharacterCard";
import CharacterFilters from "../components/CharacterFilters";
import EmptyResult from "../components/EmptyResult";
import ImportFromUid from "../components/ImportFromUid";
import LoadError from "../components/LoadError";
import SkeletonLoader from "../components/SkeletonLoader";
import { EMPTY_FILTERS, activeFilterCount, filterCharacters } from "../util/filters";

/**
 * "My characters": one click per character to mark it as owned. The bulk buttons act on what the
 * filters show, so "every 4★" is two clicks. Eidolons are set from the character detail.
 */
const Roster = () => {
  const { characters, loading, error } = useCharacters();
  const { owned, toggle, setMany, clear } = useRoster();
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  const visible = useMemo(() => filterCharacters(characters, filters, owned), [characters, filters, owned]);
  // Only count ids that still exist in the catalog
  const ownedCount = characters.filter((char) => String(char.id) in owned).length;
  const visibleIds = visible.map((char) => char.id);
  const visibleMissing = visible.filter((char) => !(String(char.id) in owned)).length;
  const visibleOwned = visible.length - visibleMissing;
  const scope = activeFilterCount(filters) > 0 ? "shown" : "";

  const clearAll = () => {
    if (window.confirm(`Remove all ${ownedCount} characters from your list?`)) clear();
  };

  if (loading) return <SkeletonLoader type="card" count={8} />;
  if (error) return <LoadError error={error} />;

  return (
    <div className="text-text-primary">
      <div className="mb-6">
        <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight text-text-primary">My characters</h1>
        <p className="font-hand text-2xl text-text-secondary mt-1">
          tap everyone you have; the team builder will only use them
        </p>
      </div>

      <ImportFromUid />

      <div className="glass-panel p-4 mb-4 flex flex-wrap items-center gap-4">
        <div className="flex-1 min-w-56">
          <p className="text-sm text-text-secondary">
            <span className="text-2xl font-extrabold text-text-primary tabular-nums">{ownedCount}</span> of {characters.length} on board
          </p>
          <div className="progress-bar mt-2">
            <div className="progress-bar-fill cyan" style={{ width: `${characters.length ? (ownedCount / characters.length) * 100 : 0}%` }} />
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => setMany(visibleIds, true)} disabled={visibleMissing === 0}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm font-semibold bg-text-primary text-bg-card hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed">
            <PlusCircle size={16} /> Add all {scope} ({visibleMissing})
          </button>
          <button type="button" onClick={() => setMany(visibleIds, false)} disabled={visibleOwned === 0}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm border border-border bg-bg-card text-text-secondary hover:text-text-primary hover:bg-bg-elevated disabled:opacity-40 disabled:cursor-not-allowed">
            <MinusCircle size={16} /> Remove all {scope} ({visibleOwned})
          </button>
          <button type="button" onClick={clearAll} disabled={ownedCount === 0}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-sm text-text-secondary hover:text-accent-red hover:bg-accent-red-muted disabled:opacity-40 disabled:cursor-not-allowed">
            <Trash2 size={16} /> Clear list
          </button>
        </div>
      </div>

      <CharacterFilters filters={filters} onChange={setFilters} ownedCount={ownedCount} total={characters.length} shown={visible.length} />

      {visible.length === 0 ? (
        <EmptyResult onClear={() => setFilters(EMPTY_FILTERS)} />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-x-5 gap-y-8 pt-3">
          {visible.map((char) => (
            <CharacterCard key={char.id} char={char} eidolon={owned[String(char.id)]} mode="toggle" dimWhenMissing
              onClick={() => toggle(char.id)} />
          ))}
        </div>
      )}
    </div>
  );
};

export default Roster;
