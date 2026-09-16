# Sparkiv-subsumsjon frontend

Enkel React-app (Vite + TypeScript) for å søke opp og vise subsumsjon-meldinger for en gitt
`vedtaksperiodeId`. Meldingene grupperes på `vedtaksperiodeId` + `behandlingId`, og subsumsjonene
for hver gruppe listes under hverandre som ekspanderbare rader.

UI-komponenter er bygget med [Aksel](https://aksel.nav.no) (`@navikt/ds-react` / `@navikt/ds-css`),
NAVs designsystem.

Appen kjører helt uavhengig av backend-appen og snakker med den over HTTP (CORS).

## Kjøre lokalt

```bash
cp .env.example .env   # juster VITE_API_URL om backend kjører på en annen adresse/port
npm install
npm run dev
```

Åpne deretter `http://localhost:5173`.

Backend-appen må kjøre samtidig (se `LocalApp.kt` i hovedprosjektet for å starte en lokal backend
med testcontainere og dummy-data), og må ha `http://localhost:5173` i sin `FRONTEND_ORIGINS`
(dette er default når `FRONTEND_ORIGINS` ikke er satt).

## Bygge

```bash
npm run build
```

Bygger en statisk `dist/`-mappe som kan hostes hvor som helst (f.eks. NAIS static hosting), så
lenge `VITE_API_URL` peker til riktig backend-URL på build-tidspunktet.
