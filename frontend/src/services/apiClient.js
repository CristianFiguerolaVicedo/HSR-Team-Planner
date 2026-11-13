import axios from "axios";

// Same client as HSRWiki-frontend, with one difference: the API is called on the SAME origin as the site
// ("/api"). In development Vite forwards /api to the backend (vite.config.js); in production Vercel does
// (vercel.json rewrites). Same origin is what makes login work: the session cookie and the CSRF cookie
// are first-party, and axios copies the XSRF-TOKEN cookie into the X-XSRF-TOKEN header by itself.
// VITE_API_BASE_URL still exists to point at another API, but login won't work cross-origin.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

// Free hosting plans can put the API to sleep, and the first request then takes a while.
// So requests wait long enough, GETs are retried for up to 4 minutes while the server is
// starting, and slow requests are reported so the UI can say why.
const TIMEOUT_MS = 100000;
const RETRY_FOR_MS = 240000;
const RETRY_DELAY_MS = 3000;
const SLOW_AFTER_MS = 4000;

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: TIMEOUT_MS,
});

// --- "server is waking up" state, for the banner in Layout ---
let pending = 0;
let slowTimer = null;
let waking = false;
const listeners = new Set();
const setWaking = (value) => {
  if (waking === value) return;
  waking = value;
  listeners.forEach((l) => l());
};
export const isServerWaking = () => waking;
export const onServerWakingChange = (listener) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

const started = () => {
  pending += 1;
  if (!slowTimer) slowTimer = setTimeout(() => setWaking(true), SLOW_AFTER_MS);
};
const finished = () => {
  pending = Math.max(0, pending - 1);
  if (pending === 0) {
    clearTimeout(slowTimer);
    slowTimer = null;
    setWaking(false);
  }
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Network errors, timeouts and 502/503/504 are what a sleeping or starting server answers
const retryable = (err) => {
  const status = err.response?.status;
  return !err.response || status === 502 || status === 503 || status === 504;
};

export const fetchJson = async (url) => {
  started();
  try {
    const start = Date.now();
    for (;;) {
      try {
        const response = await apiClient.get(url);
        return response.data;
      } catch (err) {
        if (!retryable(err) || Date.now() - start > RETRY_FOR_MS) throw err;
        await sleep(RETRY_DELAY_MS);
      }
    }
  } finally {
    finished();
  }
};

/**
 * POSTs are not retried: creating a team twice would give two share links.
 * Errors keep axios' shape, so callers can read err.response.status and err.response.data.
 */
export const postJson = async (url, body) => {
  started();
  try {
    const response = await apiClient.post(url, body);
    return response.data;
  } finally {
    finished();
  }
};

/**
 * GET without retries, for endpoints whose 502/503 mean "the answer is no" rather than
 * "the server is waking up" (e.g. the UID import: game maintenance, Enka down).
 */
export const getOnce = async (url) => {
  started();
  try {
    const response = await apiClient.get(url);
    return response.data;
  } finally {
    finished();
  }
};

export default apiClient;
