/** Same "lost signal" panel as HSRWiki's character list. */
const LoadError = ({ error, what = "the characters" }) => (
  <div className="glass-panel p-8 max-w-xl text-text-primary" role="alert">
    <p className="font-hand text-3xl text-accent-red mb-2">the Express lost signal</p>
    <p className="text-text-secondary mb-4">
      Couldn't load {what} from the server ({error?.message || "no response"}). If the API has just started,
      it may still be syncing with HSRWiki: try again in a moment.
    </p>
    <button type="button" onClick={() => window.location.reload()}
      className="px-4 py-2 rounded-lg font-semibold bg-text-primary text-bg-card hover:opacity-90">
      Try again
    </button>
  </div>
);

export default LoadError;
