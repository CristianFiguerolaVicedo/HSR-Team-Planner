import { useEffect, useRef } from "react";
import { Check, Flame, Gauge, Plus, Route, Shield, Star, Trash2, X, Zap } from "lucide-react";
import { useRoster } from "../hooks/useRoster";
import { useSynergies } from "../hooks/useSynergies";
import { EIDOLON_RANGE } from "../util/roster";
import { ELEMENT, PATH, roleLabel, rolesOf, tagLabel, tapeClass } from "../util/game";

/**
 * Character detail on top of the catalog, built on the native <dialog>:
 * focus trap, Esc to close and the backdrop come from the browser.
 */
const CharacterDetail = ({ char, onClose, onSelect }) => {
  const dialogRef = useRef(null);
  const scrollRef = useRef(null);

  // No close() on cleanup: removing the <dialog> already closes it, and under StrictMode a
  // close() here would fire a late "close" event that shuts the panel that just opened.
  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();
  }, []);

  // Jumping to a partner starts the panel from the top again
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: 0 });
  }, [char.id]);

  // A click on the backdrop lands on the <dialog> itself, not on its content
  const onBackdropClick = (e) => {
    if (e.target === dialogRef.current) onClose();
  };

  const element = ELEMENT[char.element];
  const path = PATH[char.path];

  return (
    <dialog ref={dialogRef} className="detail-dialog" aria-labelledby="character-detail-title" onClose={onClose} onClick={onBackdropClick}>
      <div ref={scrollRef} className="max-h-[inherit] overflow-y-auto grid sm:grid-cols-[230px_1fr]">
        <div className={`${tapeClass(char.element)} photo-card__picture relative min-h-64`}>
          {char.iconUrl && <img src={char.iconUrl} alt="" className="absolute inset-0 h-full w-full object-cover object-top" />}
        </div>

        <div className="p-5 space-y-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="flex" aria-label={`${char.rarity} stars`}>
                {Array.from({ length: char.rarity }).map((_, i) => (
                  <Star key={i} size={14} className={char.rarity === 5 ? "text-accent-gold fill-accent-gold" : "text-accent-purple fill-accent-purple"} />
                ))}
              </div>
              <h2 id="character-detail-title" className="text-3xl font-extrabold tracking-tight text-text-primary">{char.name}</h2>
            </div>
            <button type="button" onClick={onClose} aria-label="Close" className="p-1.5 rounded-lg text-text-muted hover:text-text-primary hover:bg-bg-elevated">
              <X size={20} />
            </button>
          </div>

          <dl className="grid grid-cols-2 gap-2 text-sm">
            <Fact icon={Zap} label="Element">
              {element && <img src={element.icon} alt="" className="w-5 h-5" />} {element?.label ?? char.element}
            </Fact>
            <Fact icon={Route} label="Path">
              {path && <img src={path.icon} alt="" className="w-5 h-5 invert opacity-70" />} {path?.label ?? char.path}
            </Fact>
            <Fact icon={Shield} label={rolesOf(char).length > 1 ? "Roles" : "Role"}>{roleLabel(char)}</Fact>
            {tagLabel(char) && <Fact icon={Flame} label="Damage">{tagLabel(char)}</Fact>}
            <Fact icon={Gauge} label="Base SPD">
              <span title="At level 80, before light cone, relics and traces">{char.baseSpd}</span>
            </Fact>
          </dl>

          <RosterControl id={char.id} />
          <Synergies char={char} onSelect={onSelect} />
        </div>
      </div>
    </dialog>
  );
};

const Fact = ({ icon, label, children }) => {
  const Icon = icon;
  return (
  <div className="rounded-lg bg-bg-secondary px-3 py-2">
    <dt className="flex items-center gap-1 text-xs text-text-muted"><Icon size={12} /> {label}</dt>
    <dd className="mt-0.5 flex items-center gap-1.5 font-semibold text-text-primary">{children}</dd>
  </div>
  );
};

/** "I have it" plus the Eidolon picker, like HSRWiki's CollectionControl. */
const RosterControl = ({ id }) => {
  const { has, eidolon, set, remove } = useRoster();
  const [min, max] = EIDOLON_RANGE;

  if (!has(id)) {
    return (
      <button type="button" onClick={() => set(id, 0)}
        className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90">
        <Plus size={18} /> I have this character
      </button>
    );
  }

  return (
    <div className="glass-panel p-3">
      <div className="flex items-center justify-between gap-2 mb-2">
        <span className="flex items-center gap-1.5 text-sm font-semibold text-text-primary">
          <Check size={16} className="text-accent-cyan" /> In my characters
        </span>
        <button type="button" onClick={() => remove(id)}
          className="flex items-center gap-1 px-2 py-1 rounded-lg text-xs text-text-secondary hover:text-accent-red hover:bg-accent-red-muted">
          <Trash2 size={14} /> Remove
        </button>
      </div>
      <div className="grid grid-cols-7 gap-1" role="group" aria-label="Eidolon">
        {Array.from({ length: max - min + 1 }, (_, i) => min + i).map((level) => (
          <button key={level} type="button" onClick={() => set(id, level)} aria-pressed={eidolon(id) === level}
            className={`py-1.5 rounded-md text-sm tabular-nums transition-colors ${eidolon(id) === level
              ? "bg-text-primary text-bg-primary font-bold"
              : "bg-bg-secondary text-text-secondary hover:text-text-primary"}`}>
            E{level}
          </button>
        ))}
      </div>
    </div>
  );
};

const Synergies = ({ char, onSelect }) => {
  const { synergies, loading, error } = useSynergies(char.id);
  const { has } = useRoster();
  const best = Math.max(1, ...synergies.map((s) => s.score));
  const ownedCount = synergies.filter((s) => has(s.partner.id)).length;

  return (
    <section aria-labelledby="synergies-title">
      <h3 id="synergies-title" className="font-hand text-3xl text-accent-cyan leading-none">travels well with</h3>

      {loading && (
        <div className="mt-3 space-y-2 animate-pulse" aria-busy="true">
          {[0, 1, 2].map((i) => <div key={i} className="h-11 bg-bg-elevated rounded-lg" />)}
        </div>
      )}
      {error && <p className="mt-2 text-sm text-text-muted">Couldn't load the synergies.</p>}
      {!loading && !error && synergies.length === 0 && (
        <p className="mt-2 text-sm text-text-muted">No recommended partners yet.</p>
      )}

      {synergies.length > 0 && (
        <>
          <p className="mt-1 text-xs text-text-muted">
            From HSRWiki's recommended teams · you have {ownedCount} of these {synergies.length}
          </p>
          <ul className="mt-2 space-y-1">
            {synergies.map(({ partner, score, reason }) => (
              <li key={partner.id}>
                <button type="button" onClick={() => onSelect(partner)} title={reason ?? undefined}
                  className="w-full flex items-center gap-3 px-2 py-1.5 rounded-lg text-left hover:bg-bg-secondary">
                  {partner.miniIconUrl
                    ? <img src={partner.miniIconUrl} alt="" loading="lazy" className="w-9 h-9 rounded-full bg-bg-elevated object-cover" />
                    : <span className="w-9 h-9 rounded-full bg-bg-elevated" />}
                  <span className="flex-1 min-w-0">
                    <span className="flex items-center gap-2 text-sm font-semibold text-text-primary">
                      <span className="truncate">{partner.name}</span>
                      {has(partner.id) && <Check size={14} className="shrink-0 text-accent-cyan" aria-label="you have this character" />}
                    </span>
                    <span className="progress-bar mt-1 block">
                      <span className="progress-bar-fill cyan block" style={{ width: `${(score / best) * 100}%` }} />
                    </span>
                  </span>
                  <span className="w-6 text-right text-xs tabular-nums text-text-muted" aria-label={`synergy score ${score}`}>{score}</span>
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
};

export default CharacterDetail;
