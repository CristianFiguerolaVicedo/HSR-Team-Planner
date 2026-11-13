import { useState } from "react";
import { Ban, Search, Star } from "lucide-react";
import Modal from "../Modal";
import { PATH } from "../../util/game";
import { sortLightconesFor } from "../../util/team";

/** Chooses a light cone for one character. Same-Path cones first: the passive only works on that Path. */
const LightconePicker = ({ char, lightcones, currentId, onPick, onClose }) => {
  const [search, setSearch] = useState("");
  const [onlyPath, setOnlyPath] = useState(true);
  const query = search.trim().toLowerCase();
  const path = PATH[char.path];

  const visible = sortLightconesFor(lightcones, char.path)
    .filter((lc) => !onlyPath || lc.path === char.path)
    .filter((lc) => !query || lc.name.toLowerCase().includes(query));

  return (
    <Modal title={`Light cone for ${char.name}`} onClose={onClose}>
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <label className="relative flex-1 min-w-40">
          <span className="sr-only">Search light cones</span>
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" aria-hidden="true" />
          <input type="search" autoFocus value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search light cones..."
            className="w-full pl-9 pr-3 py-2 rounded-lg border border-border bg-bg-primary text-sm focus:outline-none focus:border-accent-cyan" />
        </label>
        <label className="flex items-center gap-2 text-sm text-text-secondary cursor-pointer">
          <input type="checkbox" checked={onlyPath} onChange={(e) => setOnlyPath(e.target.checked)} className="accent-[#1F2A44]" />
          Only {path?.label ?? char.path}
        </label>
      </div>

      <ul className="space-y-1">
        <li>
          <button type="button" onClick={() => onPick(null)} aria-pressed={currentId == null}
            className="w-full flex items-center gap-3 px-2 py-2 rounded-lg text-left text-sm text-text-secondary hover:bg-bg-secondary aria-pressed:bg-bg-secondary">
            <Ban size={20} className="text-text-muted" /> No light cone
          </button>
        </li>
        {visible.map((lc) => (
          <li key={lc.id}>
            <button type="button" onClick={() => onPick(lc)} aria-pressed={lc.id === currentId}
              className="w-full flex items-center gap-3 px-2 py-1.5 rounded-lg text-left hover:bg-bg-secondary aria-pressed:bg-[#FFE68F]/60">
              {lc.iconUrl
                ? <img src={lc.iconUrl} alt="" loading="lazy" className="w-10 h-10 object-contain shrink-0" />
                : <span className="w-10 h-10 shrink-0 rounded bg-bg-elevated" />}
              <span className="flex-1 min-w-0">
                <span className="block text-sm font-semibold text-text-primary truncate">{lc.name}</span>
                <span className="flex items-center gap-1.5 text-xs text-text-muted">
                  <span className="flex">
                    {Array.from({ length: lc.rarity }).map((_, i) => (
                      <Star key={i} size={10} className={lc.rarity === 5 ? "text-accent-gold fill-accent-gold" : lc.rarity === 4 ? "text-accent-purple fill-accent-purple" : "text-accent-cyan fill-accent-cyan"} />
                    ))}
                  </span>
                  {PATH[lc.path] && <img src={PATH[lc.path].icon} alt="" className="w-3.5 h-3.5 invert opacity-70" />}
                  {PATH[lc.path]?.label ?? lc.path}
                  {lc.path !== char.path && <span className="text-accent-red">· passive won't work</span>}
                </span>
              </span>
            </button>
          </li>
        ))}
      </ul>
      {visible.length === 0 && <p className="py-6 text-center font-hand text-2xl text-text-secondary">no light cones match</p>}
    </Modal>
  );
};

export default LightconePicker;
