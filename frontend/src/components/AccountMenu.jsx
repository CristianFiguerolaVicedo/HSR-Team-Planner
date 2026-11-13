import { useEffect, useRef, useState } from "react";
import { LogIn, LogOut, UserRound } from "lucide-react";
import { useSession } from "../hooks/useSession";

/**
 * Header corner: "Sign in" with the configured providers, or the user's avatar with "Sign out".
 * The provider buttons are plain links on purpose: OAuth needs a full page navigation
 * (the browser goes to Discord/Google and comes back), not a fetch.
 */
const AccountMenu = () => {
  const { status, user, providers, logout } = useSession();
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  // Close when clicking outside or pressing Esc
  useEffect(() => {
    if (!open) return undefined;
    const onClick = (e) => { if (!ref.current?.contains(e.target)) setOpen(false); };
    const onKey = (e) => { if (e.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  if (status === "loading") return <span className="w-8 h-8 rounded-full bg-bg-elevated animate-pulse" aria-hidden="true" />;
  // No provider configured on the server: no login at all, the app works as before
  if (status === "anonymous" && providers.length === 0) return null;

  return (
    <div ref={ref} className="relative">
      <button type="button" onClick={() => setOpen((v) => !v)} aria-expanded={open} aria-haspopup="menu"
        className="flex items-center gap-2 px-2 py-1.5 rounded-lg text-sm text-text-secondary hover:text-text-primary hover:bg-bg-elevated">
        {status === "authenticated" ? (
          <>
            {user.avatarUrl
              ? <img src={user.avatarUrl} alt="" className="w-7 h-7 rounded-full object-cover" referrerPolicy="no-referrer" />
              : <UserRound size={20} />}
            <span className="hidden sm:inline font-semibold text-text-primary max-w-32 truncate">{user.name}</span>
          </>
        ) : (
          <><LogIn size={16} /> Sign in</>
        )}
      </button>

      {open && (
        <div role="menu" className="absolute right-0 mt-2 w-60 glass-panel p-2 shadow-lg z-50">
          {status === "authenticated" ? (
            <>
              <p className="px-2 py-1.5 text-xs text-text-muted">
                Signed in with {user.provider === "discord" ? "Discord" : "Google"} as{" "}
                <span className="font-semibold text-text-primary">{user.name}</span>
              </p>
              <button type="button" role="menuitem" onClick={() => { setOpen(false); logout(); }}
                className="w-full flex items-center gap-2 px-2 py-2 rounded-md text-sm text-text-secondary hover:text-accent-red hover:bg-accent-red-muted">
                <LogOut size={16} /> Sign out
              </button>
            </>
          ) : (
            <>
              <p className="px-2 py-1.5 text-xs text-text-muted">Keep your characters and teams on every device.</p>
              {providers.map((provider) => (
                <a key={provider.id} href={provider.loginUrl} role="menuitem"
                  className="flex items-center gap-2 px-2 py-2 rounded-md text-sm font-semibold text-text-primary hover:bg-bg-elevated">
                  <LogIn size={16} /> Continue with {provider.name}
                </a>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default AccountMenu;
