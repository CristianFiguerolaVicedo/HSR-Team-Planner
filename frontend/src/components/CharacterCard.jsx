import { memo } from "react";
import { Star } from "lucide-react";
import { ELEMENT, PATH, roleLabel, tapeClass, tiltFor } from "../util/game";

/**
 * Instant photo with washi tape, like HSRWiki's CharInfoCard.
 * mode "open": the click opens the detail (catalog).
 * mode "toggle": the click adds or removes the character from "My characters" (roster page).
 */
const CharacterCard = ({ char, eidolon, onClick, mode = "open", dimWhenMissing = false }) => {
  const owned = eidolon !== undefined;
  const element = ELEMENT[char.element];
  const path = PATH[char.path];

  return (
    <button
      type="button"
      onClick={onClick}
      className={`photo-card ${tapeClass(char.element)} w-full text-left cursor-pointer`}
      style={{ "--tilt": `${tiltFor(char.id, 0.6)}deg`, "--tape-tilt": `${tiltFor(char.id + 3, 1.4)}deg` }}
      aria-pressed={mode === "toggle" ? owned : undefined}
      aria-haspopup={mode === "open" ? "dialog" : undefined}
      aria-label={mode === "toggle"
        ? `${char.name}: ${owned ? "in my characters, click to remove" : "click to add to my characters"}`
        : `View ${char.name} details`}
      data-dimmed={dimWhenMissing && !owned}
    >
      <span className="washi" aria-hidden="true" />
      {owned && (
        <span className="absolute top-4 left-4 z-10 px-2 py-0.5 rounded-md bg-text-primary text-bg-primary text-xs font-bold tabular-nums shadow-sm -rotate-3">
          E{eidolon}
        </span>
      )}
      {owned && mode === "toggle" && <span className="stamp">ON BOARD</span>}

      <div className="photo-card__picture h-44 flex items-end justify-center">
        {char.iconUrl
          ? <img src={char.iconUrl} alt="" loading="lazy" className="h-full w-full object-cover object-top" />
          : <span className="self-center font-hand text-4xl text-text-muted">?</span>}
      </div>

      <div className="pt-3 px-1">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold text-[1.05rem] leading-tight text-text-primary">{char.name}</h3>
          <div className="flex shrink-0 pt-0.5" aria-label={`${char.rarity} stars`}>
            {Array.from({ length: char.rarity }).map((_, index) => (
              <Star key={index} className={char.rarity === 5 ? "text-accent-gold fill-accent-gold" : "text-accent-purple fill-accent-purple"} size={11} />
            ))}
          </div>
        </div>

        <div className="mt-1.5 flex items-center gap-1.5 text-text-secondary min-w-0">
          {element && <img src={element.icon} alt="" className="w-5 h-5" title={element.label} />}
          {path && <img src={path.icon} alt="" className="w-5 h-5 invert opacity-70" title={path.label} />}
          <span className="font-hand text-xl leading-[0.95] ml-0.5 min-w-0 break-words">
            {roleLabel(char)}
          </span>
        </div>
      </div>
    </button>
  );
};

export default memo(CharacterCard);
