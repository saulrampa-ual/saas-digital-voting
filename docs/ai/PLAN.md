# PLAN · Lo que falta hasta la entrega

Creado: 03/11/2026 · Entrega de la memoria: 27/01/2027 · Base: `PROGRESS.md`, `TRACEABILITY.md`, `IMPROVEMENTS.md`
y las marcas `[PENDIENTE]` de la memoria.

**Cómo usarlo.** Marca `[x]` al terminar una tarea y anota la fecha o el commit. Para retomar, pide a la IA
«revisa `docs/ai/PLAN.md` y dime qué falta»: debe contrastar cada tarea con el repositorio y actualizar este fichero.
Cuando una pregunta de la sección 1 tenga respuesta, se escribe la respuesta y, si cierra una decisión, se pasa a
`DECISIONS.md`.

**Quién:** `IA` la puede hacer sola · `Autor` solo la puedes hacer tú · `Ambos` la IA prepara y tú ejecutas o validas.

## Estado general

| Bloque | Estado |
| --- | --- |
| Desarrollo del MVP (P0–P6, RF-01 a RF-21) | Hecho. Única excepción: RF-05a parcial (falta un IdP externo de prueba) |
| Pruebas y medición (P7) | En curso: 60 pruebas backend, 19 frontend, cobertura 7–14 %; H1a, H1b y H1c sin medir |
| Memoria (P8) | En curso: borrador de 82 páginas; tabla de hipótesis en «PENDIENTE» |
| Publicación | `dev` va por delante de `origin/dev`: falta `git push origin dev` (lo hace el autor) |

---

## 1. Preguntas para el autor (responder primero: desbloquean tareas)

| ID | Pregunta | Desbloquea | Respuesta |
| --- | --- | --- | --- |
| Q-01 | ¿RF-11 (revoto) queda como «Could» o pasa a «Won't» en la tabla 4.3? | C3 | ¿? |
| Q-02 | ¿«Sin ayuda» en RNF-4 excluye también consultar la documentación? (marcador `[PENDIENTE DEL AUTOR]` en `especificacion_detallada.tex`) | C3, B4 | ¿? |
| Q-03 | ¿Se anotan RF-22 y RF-23 como fuera del MVP en la tabla 1.1? | C3 | ¿? |
| Q-04 | ¿En P9 se depura el modelo de datos de sustitución del voto (`votes.status` `SUBSTITUTED`, RI-6/RI-8/RI-9) o se deja descrito? | C3 | ¿? |
| Q-05 | ¿Se hace la sesión RNF-4 con cinco administradores o se declara pendiente en la memoria? | B4 | ¿? |
| Q-06 | ¿Fecha para el ensayo y para las cinco sesiones con votantes (H1b)? ¿Quiénes participan (sin datos reales)? | B2 | ¿? |
| Q-07 | ¿Se prueba RF-05a con un IdP externo (p. ej. un segundo realm de Keycloak como «universidad») o se deja como limitación? | A9 | ¿? |
| Q-08 | ¿Qué mejoras de `IMPROVEMENTS.md` se aprueban antes de la entrega? Propuesta en la sección 4 | D1–D5 | ¿? |
| Q-09 | ¿Cuándo se fusiona `dev` en `main` (al cerrar P7, o antes para tener una versión estable)? | E2 | ¿? |
| Q-10 | ¿Qué fecha tiene la próxima revisión con el tutor y qué quiere ver? (marca el orden de C1–C4) | C | ¿? |

---

## 2. Se puede empezar ya (sin esperar respuestas)

Orden recomendado: de arriba abajo. Semana del 03/11 al 16/11/2026.

- [ ] **A1 · Publicar lo que hay** (`Autor`): `git push origin dev`. Opcional: borrar ramas locales ya fusionadas
  (`feat/login-and-home-polish`, `feat/login-language-flags`, `feat/login-responsive-redesign`,
  `fix/login-mobile-spacing`, `feat/console-rail-navigation`) y la remota `origin/feat/role-home-dashboards`.
- [ ] **A2 · Poner al día `PROGRESS.md`** (`IA`): entrada de la consola del 03/11 (barra de iconos, logo, banderas,
  gráfica por periodo) y quitar la marca suelta `PENDIENTE_PANEL`.
- [ ] **A3 · Demo UAL y E2E** (`Ambos`): arrancar el stack, `node scripts/demo/seed-ual.mjs` (corregido, sin repetir)
  y `npm run test:e2e` contra el frontend de la rama actual (no otro servidor en el puerto 4200, ver DEF-03).
- [ ] **A4 · axe en todas las pantallas** (`Ambos`, parte automática de H1a): login, inicio de cada rol, censos,
  procesos (A3/A4), participación, resultados, auditoría, trazabilidad, privacidad, claves de API, flujo del votante
  (V1–V8, B1–B5) en 1280 y 360 px, ES y EN. Criterio: cero incumplimientos críticos ni serios. El selector de idioma
  con banderas es nuevo (sustituye al que dio DEF-01): repetir también `/recibo/:id`.
- [ ] **A5 · Pruebas unitarias P7.1** (`IA`): censo/CSV, claims institucionales (RF-05b), privacidad (RF-18),
  recordatorios (RF-17), errores y reglas de dominio sin prueba; frontend: gráfica por día (`app-day-chart`),
  agregados del inicio y flujo visual del votante.
- [ ] **A6 · Integración P7.2/P7.3** (`IA`): matriz completa de autorización HTTP entre organizaciones y roles,
  concurrencia del token de voto (un solo voto con N envíos), reintentos e idempotencia del *outbox*, inspección del
  esquema y de los logs para el secreto del voto (RNF-11), todas las manipulaciones de la cadena.
- [ ] **A7 · Carga H1c** (`Ambos`): instalar k6, completar escenarios de acceso y escrutinio, credenciales sintéticas
  fuera del repo, tres ejecuciones de 500 VU con p95, errores y recursos; comprobar la cadena y la participación después.
- [ ] **A8 · Gantt de la memoria** (`IA`, IMP-02): la iteración 3 aún dice «re-voto».
- [ ] **A9 · RF-05a** (`IA`, según Q-07): IdP externo de prueba y prueba de inicio de sesión de gestión federado.
- [ ] **A10 · Job E2E en CI** (`IA`): Playwright + axe en GitHub Actions (al menos con servicios simulados).

## 3. Necesitan al autor

- [ ] **B1 · Responder las preguntas Q-01 a Q-10** (`Autor`).
- [ ] **B2 · Sesión H1b con cinco votantes** (`Autor`, prioritaria): montar el escenario `USAB`
  (`docs/evaluation/usabilidad/preparacion-sesion.md`, `censo-sesion.csv`), hacer un ensayo y las cinco sesiones;
  anotar T0/T1/T2 y resultados como cantidades (x de 5) en la hoja de registro. Criterio: 5 de 5 en ≤ 3 min y al
  menos 4 de 5 interpretan la verificación.
- [ ] **B3 · Revisión manual de accesibilidad** (`Autor`, parte manual de H1a): teclado y lector de pantalla
  (NVDA o Narrador) en acceso, voto y recibo siguiendo `docs/evaluation/accesibilidad-manual.md`.
- [ ] **B4 · RNF-4 con administradores** (`Ambos`, según Q-05): la IA prepara un protocolo propio (tareas, tiempos,
  definición de «sin ayuda» de Q-02); el autor hace las cinco sesiones.

## 4. Mejoras opcionales (solo si se aprueban en Q-08)

Las que más aportan antes de entregar, de más a menos:

- [ ] **D1 · IMP-15**: aviso 2 minutos antes de que caduque la sesión de gestión (WCAG 2.2.1).
- [ ] **D2 · IMP-21**: límite de peticiones por IP en el *gateway* para `/api/v1/voter-access/**` (RNF-14).
- [ ] **D3 · IMP-32**: cabeceras CSP, HSTS y `Referrer-Policy` en el despliegue del frontal.
- [ ] **D4 · IMP-11 / IMP-40**: al suspender una organización, deshabilitar sus usuarios y claves de API en Keycloak.
- [ ] **D5 · IMP-27**: comprobar la audiencia del JWT en cada servicio y quitar `verivoto-dev-swagger` en producción.

El resto de `IMPROVEMENTS.md` queda para P9 salvo que se decida otra cosa.

## 5. Memoria (P8)

- [ ] **C1 · Resultados de la evaluación** (`IA`, tras A4, A7, B2, B3): completar `docs/evaluation/informe.md`, la tabla
  13.1 (`evaluacion_resultados.tex`), la lista `[PENDIENTE]` de `contenido1.tex` y las conclusiones. Lo que no se
  mida se deja como PENDIENTE y limitación, sin inventar resultados.
- [ ] **C2 · Alinear con lo implementado** (`IA`): figuras de arquitectura (objetivo frente a implementado),
  diagramas conceptuales y texto heredado; capturas seguras de las pantallas (datos sintéticos de la demo UAL).
- [ ] **C3 · Aplicar las decisiones Q-01 a Q-04** (`IA`) en la especificación y las tablas 1.1 y 4.3.
- [ ] **C4 · Revisión editorial y visual final** (`Ambos`, P8.4): compilar el PDF, revisar página a página,
  avisos de maquetación de portada y contraportada; el autor da el visto bueno.

## 6. Cierre

- [ ] **E1 · Cerrar P7** (`IA`): cuando A3–A7 y B2–B4 estén hechos o declarados pendientes, actualizar `PROGRESS.md`,
  `TRACEABILITY.md` (columna de evidencia) y `docs/evaluation/resultados-p7/`.
- [ ] **E2 · `dev` → `main`** (`Autor`, según Q-09) y etiqueta de la versión entregada.
- [ ] **E3 · Entrega** (`Autor`): PDF final y anexos.

## Calendario orientativo

| Semanas | Foco |
| --- | --- |
| 03/11 – 16/11/2026 | A1–A6, B1 (preguntas) |
| 17/11 – 30/11/2026 | A7, A8, A10, B3, ensayo de B2; D1–D5 si se aprueban |
| 01/12 – 21/12/2026 | B2 (sesiones con votantes), B4 si se hace, A9 |
| 22/12/2026 – 10/01/2027 | C1, C2, C3, E1 |
| 11/01 – 27/01/2027 | C4, E2, E3 |

## Registro de avance

| Fecha | Tarea | Nota / commit |
| --- | --- | --- |
| 03/11/2026 | — | Plan creado |
