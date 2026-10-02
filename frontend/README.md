# Sparkiv-subsumsjon frontend

Enkel React-app (Vite + TypeScript) for å søke opp og vise subsumsjon-meldinger for en gitt
`vedtaksperiodeId`. Meldingene grupperes på `vedtaksperiodeId` + `behandlingId`, og subsumsjonene
for hver gruppe listes under hverandre som ekspanderbare rader.

UI-komponenter er bygget med [Aksel](https://aksel.nav.no) (`@navikt/ds-react` / `@navikt/ds-css`),
NAVs designsystem.

Appen bygges inn i backend-jaren og serveres av backend-appen på samme origin (se
`processResources` i `build.gradle.kts` og `App.kt`), så det trengs ikke CORS eller noen egen
frontend-driftssetting.

## Kjøre lokalt

```bash
pnpm install
pnpm run dev
```

Åpne deretter `http://localhost:5173`. API-kall proxyes til `http://localhost:5173` → backend på
`http://localhost:8080` (se `vite.config.ts`), så backend-appen må kjøre samtidig (se `LocalApp.kt`
i hovedprosjektet for å starte en lokal backend med testcontainere og dummy-data).

## Bygge

```bash
pnpm run build
```

Bygger en statisk `dist/`-mappe som Gradle-bygget i hovedprosjektet kopierer inn som statiske
ressurser i backend-jaren. Gradle bygger ikke frontenden selv, så kjør `pnpm run build` før
`./gradlew build` hvis du vil ha med frontenden. Finnes ikke `dist/`, blir jaren bygget uten
frontend. På GitHub bygger workflowene frontenden med pnpm før Gradle kjører (se
`.github/actions/bygg-frontend`).
