# E2E y axe

Playwright 1.64.1 + Chromium 156.0.8078.4 ejecutó cuatro casos sobre la pantalla inicial «Comprobar mi recibo», en 1280×800 y 360×800, español e inglés. La ruta `GET /recibo/7` no contiene credencial; el estado inicial no llama a la API.

Resultados por ejecución: escritorio/español: 1 crítico y 1 serio (`aria-required-children`, `label-content-name-mismatch`); escritorio/inglés: 1 serio (`label-content-name-mismatch`); móvil/español: 1 crítico y 1 serio (mismas dos reglas); móvil/inglés: 1 serio (`label-content-name-mismatch`). Se registra como incidencia DEF-01 (criterio de D-49: se documenta y se decide si se corrige antes del cierre). Mientras siga abierta, H1a no puede aceptarse; la decisión final queda PENDIENTE hasta auditar todas las pantallas y completar la revisión manual. Origen probable: el selector de idioma compartido (`shared/language-switcher`), presente en todas las pantallas. No se modificó producción; se registra en `defectos.md`.

JSON resumidos: `%TEMP%\verivoto-p7-e2e\desktop-es-axe.json`, `desktop-en-axe.json`, `mobile-es-axe.json`, `mobile-en-axe.json`. Informe HTML Playwright: `%TEMP%\verivoto-p7-e2e\playwright-report\index.html` (fuera del repositorio).

La prueba también verifica teclado, presencia de foco, URL sin query/hash, claves de almacenamiento sin nombres sensibles y errores de consola. Aclaración (31/10/2026): el enlace personal entra por `/votar/:linkToken` por diseño (es el enlace de la invitación, D-33); lo que nunca debe ir en la URL ni en el almacenamiento es el código, el token de voto o de resultados y el recibo. El criterio «sin enlace en la URL» del encargo era erróneo. Véase DEF-02 (reclasificado).

Pendiente: flujo votante completo, gestión, estados de error, backend real, Mailpit, auditoría axe de cada pantalla/estado, comprobación de foco visual real con NVDA y job de CI. Propuesta de CI (sin activar): añadir un job `e2e` que levante los servicios de prueba, prepare un censo sintético, ejecute Chromium/Playwright y publique el HTML como artefacto sin almacenar tokens ni OTP.

## Repetición tras corregir DEF-01 (31/10/2026)

Con el cambio de `fix/a11y-language-switcher`, axe (`wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa`) da 0 incumplimientos en `/recibo/7` y en `/votar/…` (estado de error, sin backend), en 1280×800 y 360×800, ES y EN. Ejecución en el entorno de la IA con Chromium de Playwright 1.63 y `@axe-core/playwright` 4.14.0; falta repetir `npm run test:e2e` en el equipo del autor. Esto no cambia H1a: siguen pendientes el resto de pantallas y estados y la revisión manual.

## Fase 2 · Confirmación Windows (31/10/2026)

Comando: `npm run test:e2e` (Playwright 1.64.1, Chromium del entorno local), desde `frontend/` en Windows 11. La primera invocación no pudo iniciar porque faltaban los paquetes locales; después de `npm ci` (lockfile sin cambios) se ejecutaron los cuatro casos existentes, y los cuatro fallaron en el criterio axe. Los cuatro conservaron foco visible, almacenamiento/URL sin datos sensibles y consola sin errores.

| pantalla | estado | ancho | idioma | críticos | serios | moderados | menores | reglas |
| --- | --- | ---: | --- | ---: | ---: | ---: | ---: | --- |
| Comprobar recibo | inicial | 1280 | ES | 1 | 2 | 0 | 0 | `aria-required-children`, `label-content-name-mismatch` |
| Comprobar recibo | inicial | 1280 | EN | 0 | 2 | 0 | 0 | `label-content-name-mismatch` |
| Comprobar recibo | inicial | 360 | ES | 1 | 2 | 0 | 0 | `aria-required-children`, `label-content-name-mismatch` |
| Comprobar recibo | inicial | 360 | EN | 0 | 2 | 0 | 0 | `label-content-name-mismatch` |

Selectores devueltos por axe: `ion-segment` (`aria-required-children`; detectado en ES; informa hijos `ion-segment-button[aria-label]`) y los botones internos `button[aria-label="Spanish"]` / `button[aria-label="English"]` (`label-content-name-mismatch`; el texto visible ES/EN no forma parte del nombre accesible). La causa está en el selector compartido; no se modificó código de producción. Véase DEF-03.

JSON completo, con regla, impacto, selector, HTML y resumen de fallo: `%TEMP%\\verivoto-p7-e2e\\desktop-es-axe.json`, `desktop-en-axe.json`, `mobile-es-axe.json`, `mobile-en-axe.json`. Informe Playwright: `%TEMP%\\verivoto-p7-e2e\\playwright-report\\index.html` (fuera del repositorio). La repetición contradice la verificación previa hecha en otro entorno; el resultado Windows de esta fase se informa tal cual. H1a automática: no cumple en estas pantallas; evaluación total PENDIENTE.

**Revisión del autor (01/11/2026):** esta repetición no es válida. Se ejecutó contra el frontend antiguo que seguía escuchando en el puerto 4200 (véase DEF-03 descartado en `defectos.md`). Hay que repetirla con `E2E_BASE_URL` apuntando al frontend de la rama o con ese puerto libre.
