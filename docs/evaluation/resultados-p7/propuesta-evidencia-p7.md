# Propuesta de actualización de «Evidencia P7»

Texto propuesto; no aplicado a `TRACEABILITY.md`, `informe.md` ni `PROGRESS.md` hasta tener la pasada final completa:

- **RF-03:** `VoterCsvReaderTest` y `CensusImportTest` (cabecera/separador/UTF-8/límite/duplicados/dryRun/descarte/lote); `ElectionPostgresIntegrationTest` verifica 401, scoping HTTP ADMIN y 403 para INTEGRATION. Pendientes los demás endpoints y roles.
- **RF-09/RNF-11/RNF-13:** `BallotBoxPostgresIntegrationTest` recorre las 14 tablas y permite marcas temporales documentadas; `ChainVerifierTest` comprueba papeleta modificada y desajuste de recibos; siguen pendientes flujo/logs reales y concurrencia.
- **RNF-1/H1a:** Playwright/axe en pantalla inicial de recibo, 1280×800 y 360×800, ES/EN; se detectan incumplimientos críticos/serios. No demuestra H1a y falta revisión NVDA/manual.
- **H1c:** `PENDIENTE`; k6 no instalado y no hay tres ejecuciones.

Actualizar las cifras globales solo con Surefire, Vitest y CSV JaCoCo de la ejecución final.
