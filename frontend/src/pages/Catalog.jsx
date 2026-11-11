import { useMemo, useState } from "react";
import { useCharacters } from "../hooks/useCharacters";
import { useRoster } from "../hooks/useRoster";
import CharacterCard from "../components/CharacterCard";
import CharacterDetail from "../components/CharacterDetail";
import CharacterFilters from "../components/CharacterFilters";
import EmptyResult from "../components/EmptyResult";
import LoadError from "../components/LoadError";
import SkeletonLoader from "../components/SkeletonLoader";
import { EMPTY_FILTERS, filterCharacters } from "../util/filters";

/** Every character: browse, filter and open one to see who they team up best with. */
const Catalog = () => {
  const { characters, loading, error } = useCharacters();
  const { owned } = useRoster();
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [selected, setSelected] = useState(null);

  const visible = useMemo(() => filterCharacters(characters, filters, owned), [characters, filters, owned]);
  const ownedCount = characters.filter((char) => String(char.id) in owned).length;

  if (loading) return <SkeletonLoader type="card" count={8} />;
  if (error) return <LoadError error={error} />;

  return (
    <div className="text-text-primary">
      <div className="mb-8">
        <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight text-text-primary">Characters</h1>
        <p className="font-hand text-2xl text-text-secondary mt-1">
          open anyone to see who they travel best with
        </p>
      </div>

      <CharacterFilters filters={filters} onChange={setFilters} ownedCount={ownedCount} total={characters.length} shown={visible.length} />

      {visible.length === 0 ? (
        <EmptyResult onClear={() => setFilters(EMPTY_FILTERS)} />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-x-5 gap-y-8 pt-3">
          {visible.map((char) => (
            <CharacterCard key={char.id} char={char} eidolon={owned[String(char.id)]} onClick={() => setSelected(char)} />
          ))}
        </div>
      )}

      {selected && <CharacterDetail char={selected} onClose={() => setSelected(null)} onSelect={setSelected} />}
    </div>
  );
};

export default Catalog;
