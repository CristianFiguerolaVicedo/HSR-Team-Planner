import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from "@tailwindcss/vite";

// The backend runs on :8080; the browser only ever talks to :5173.
// Forwarding these paths keeps cookies (session, CSRF) on one origin, and the OAuth redirect
// back from Discord/Google (http://localhost:5173/login/oauth2/code/...) reaches Spring.
//
// changeOrigin MUST be false: Spring builds the OAuth redirect_uri from the Host header, so it has
// to keep seeing "localhost:5173". The short form ("/api": backend) turns changeOrigin ON, which
// makes Spring send http://localhost:8080/login/oauth2/code/... and the provider answers
// redirect_uri_mismatch. That's why every entry uses the long form.
const backend = { target: "http://localhost:8080", changeOrigin: false };

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/api": backend,
      "/oauth2": backend,
      "/login/oauth2": backend,
    },
  },
})
