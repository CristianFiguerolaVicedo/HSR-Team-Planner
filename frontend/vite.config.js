import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from "@tailwindcss/vite";

// The backend runs on :8080; the browser only ever talks to :5173.
// Forwarding these paths keeps cookies (session, CSRF) on one origin, and the OAuth redirect
// back from Discord/Google (http://localhost:5173/login/oauth2/code/...) reaches Spring.
// changeOrigin stays false so Spring sees Host: localhost:5173 and builds that redirect URI.
const backend = "http://localhost:8080";

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
