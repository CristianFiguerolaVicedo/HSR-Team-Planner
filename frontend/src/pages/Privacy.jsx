import { useState } from "react";
import { Link } from "react-router-dom";
import { useSession } from "../hooks/useSession";
import DeleteAccountDialog from "../components/DeleteAccountDialog";

// Everything this site keeps in the browser starts with this prefix
const BROWSER_PREFIX = "hsr-planner-";

const Section = ({ title, children }) => (
  <section className="space-y-2">
    <h2 className="text-xl font-bold text-text-primary">{title}</h2>
    <div className="space-y-2 text-text-secondary">{children}</div>
  </section>
);

/** Plain-language privacy page: what is stored, where, and how to delete it. */
const Privacy = () => {
  const { status } = useSession();
  const [deleting, setDeleting] = useState(false);

  const clearBrowser = () => {
    if (!window.confirm("Clear the characters, draft and UID this site keeps in this browser?")) return;
    try {
      Object.keys(localStorage).filter((k) => k.startsWith(BROWSER_PREFIX)).forEach((k) => localStorage.removeItem(k));
    } catch { /* storage blocked: nothing stored either */ }
    // The stores keep a copy in memory: reload so every page starts empty
    window.location.reload();
  };

  return (
    <div className="max-w-3xl text-text-primary space-y-8">
      <div>
        <h1 className="text-5xl font-extrabold tracking-tight">Privacy</h1>
        <p className="font-hand text-2xl text-text-secondary mt-1">what this site keeps, and how to get rid of it</p>
      </div>

      <Section title="Without an account">
        <p>
          Nothing about you is sent to the server. "My characters", the team you're building and your UID are kept
          only in this browser (local storage). Teams you save get a public link; anyone with the link can see them.
        </p>
      </Section>

      <Section title="With an account (Discord or Google)">
        <p>When you sign in, the server stores:</p>
        <ul className="list-disc pl-6 space-y-1">
          <li>which service you used and your id there, your display name and your avatar picture address;</li>
          <li>your saved characters and their Eidolons;</li>
          <li>the teams you save (they still have a public link).</li>
        </ul>
        <p>
          It does <span className="font-semibold text-text-primary">not</span> ask for or store your email, and it
          doesn't save the Discord or Google access tokens. You stay signed in through a session cookie; the session
          itself is kept in the database (so a server restart doesn't sign you out) and is deleted when you sign out,
          delete your account, or after 30 days without visits.
        </p>
      </Section>

      <Section title="Import by UID">
        <p>
          Your UID is sent through this site's server to Enka.Network (or Mihomo) to read your public in-game profile.
          The answer is kept in memory for a few minutes and then forgotten; it isn't saved.
        </p>
      </Section>

      <Section title="Deleting your data">
        <div className="flex flex-wrap gap-3 pt-1">
          {status === "authenticated" ? (
            <button type="button" onClick={() => setDeleting(true)}
              className="px-4 py-2 rounded-lg text-sm font-semibold bg-accent-red text-white hover:opacity-90">
              Delete my account and my data
            </button>
          ) : (
            <p className="text-sm">Sign in to delete an account and everything saved in it.</p>
          )}
          <button type="button" onClick={clearBrowser}
            className="px-4 py-2 rounded-lg text-sm font-semibold border border-border bg-bg-card text-text-primary hover:bg-bg-elevated">
            Clear this browser's data
          </button>
        </div>
        <p className="text-sm">
          Deleting the account removes your saved characters and all your teams; their links stop working.
          Teams saved without an account have no owner, so they can't be traced back to you.
        </p>
      </Section>

      <p className="text-sm text-text-muted">
        Questions? Open an issue on the <a className="underline" href="https://github.com/CristianFiguerolaVicedo/HSR-Team-Planner" target="_blank" rel="noopener noreferrer">GitHub repository</a>.
        {" "}<Link to="/" className="underline">Back to the planner</Link>
      </p>

      {deleting && <DeleteAccountDialog onClose={() => setDeleting(false)} />}
    </div>
  );
};

export default Privacy;
