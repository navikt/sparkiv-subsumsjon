import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Backend-appen serverer frontendens dist/ på samme origin i prod/dev-gcp (se
    // build.gradle.kts), men under lokal utvikling kjører vite på en egen port. Proxy API-kallene
    // til den lokale backend-appen (se LocalApp.kt) slik at relative URL-er i api.ts fungerer.
    proxy: {
      '/vedtaksperiode': 'http://localhost:8080',
      '/fodselsnummer': 'http://localhost:8080',
    },
  },
})
