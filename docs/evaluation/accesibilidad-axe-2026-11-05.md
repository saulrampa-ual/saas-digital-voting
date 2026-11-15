# H1a · Barrido axe del flujo del votante (05/11/2026)

Parte automática de H1a (RNF-1, WCAG 2.2 AA). Tarea A4 de `docs/ai/PLAN.md`. La revisión manual con teclado y NVDA
(`accesibilidad-manual.md`, tarea B3) **no forma parte de esta medición** y no se realizó antes del cierre.

## Qué se midió

| Elemento | Valor |
| --- | --- |
| Fecha | 05/11/2026 (tres barridos: antes de corregir DEF-05, con DEF-05 corregido y con DEF-05 y DEF-06 corregidos) y 08/11/2026 (barrido 4, sobre el commit `06f39be`) |
| Versión evaluada | Barrido 1: `dev` en `4a5e593`. Barrido 2: `dev` en `4a5e593` con la corrección de DEF-05 en `frontend/src/app/features/voter/steps/ballot-step.component.ts` (sin commit). **Barrido 3:** `dev` en `4a5e593` con las correcciones de DEF-05 y DEF-06 sin commit (huellas SHA-256, 12 primeros caracteres: `ballot-step.component.ts` `353213fb93e6`, `receipt-check.page.ts` `402c9ef12240`, `voter-flow.accessibility.spec.ts` `747d65cbec86`, con saltos LF). **Barrido 4 (vigente):** `dev` en `06f39be` (merge de `fix/auditoria-visual`; las correcciones de DEF-05 y DEF-06 entraron en `9c948cf`), árbol limpio. Los ficheros del flujo del votante son idénticos a los del barrido 3 salvo por los saltos de línea; los cambios de `06f39be` afectan a la consola de gestión, la página 404, claves i18n nuevas e iconos |
| Prueba | `frontend/tests/e2e/voter-flow.accessibility.spec.ts` |
| Herramientas (iguales en los tres barridos) | Playwright 1.64.1 · Chromium (Chrome for Testing 153.0.8010.12) · axe-core 4.14.0 · `@axe-core/playwright` 4.14.0 |
| Aplicación | Angular 22.2.4 · Ionic 9.0.10 · servidor de desarrollo (`ng serve`) · Node.js 22.23.4 |
| Reglas | Etiquetas `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa` |
| Combinaciones | 22 pantallas o estados × 2 anchos (1280 y 360 px) × 2 idiomas (ES, EN) = **88 auditorías** por barrido |
| Datos | Respuestas de la API sintéticas, conformes a los contratos de voter-access y ballot-box; sin backend, sin datos reales |
| Entorno | Entorno de la IA (Linux); resultados JSON fuera del repositorio |

La prueba recorre cada estado como lo haría una persona (pulsar «Empezar», escribir el código, elegir una opción,
revisar, confirmar, comprobar el recibo) y ejecuta axe al llegar. También comprueba que no haya códigos, tokens ni
recibos en la URL ni en el almacenamiento del navegador.

Pantallas y estados: V1 bienvenida; B1 código enviado; V2 código incorrecto; B2 papeleta; B2 papeleta sin elegir
(error); B3 revisión; B4 recibo; V7 voto no registrado; V3 ya votó; V6 sesión caducada; C4 acceso bloqueado; V4 aún no
abierto; V5 cerrado; C3 enlace no válido; C3 enlace caducado; acceso con cuenta institucional (RF-05b); V8 resultado
disponible; V8 resultado; B5 comprobar recibo (inicial, formato erróneo, presente y no encontrado).

## Resultado

| Severidad (impacto axe) | Barrido 1 (`4a5e593`) | Barrido 2 (con DEF-05 corregido) | Barrido 3 (con DEF-05 y DEF-06 corregidos) |
| --- | ---: | ---: | ---: |
| Críticos | 8 | 0 | **0** |
| Serios | 0 | 0 | **0** |
| Moderados | 0 | 0 | 0 |
| Menores | 0 | 0 | 0 |

- **Barrido 1:** los 8 críticos eran la misma incidencia, **DEF-05**: regla `aria-required-children` sobre el `ion-list`
  que agrupaba las opciones dentro del `ion-radio-group` de la papeleta (B2, con o sin opción elegida, en los dos anchos
  y los dos idiomas). Las otras 20 pantallas o estados daban 0.
- **Corrección:** la papeleta usa un contenedor sin rol de lista (`div.options`) en lugar de `ion-list` y cada opción
  conserva su `ion-item` sin línea; el grupo de radio y sus opciones no cambian. Vitest 19 de 19 y compilación correctos.
- **Barrido 2:** 0 incumplimientos en las 88 auditorías. La prueba E2E anterior del recibo
  (`receipt-check.accessibility.spec.ts`) también pasa (4 de 4).
- **Corrección de DEF-06** (aprobada por el autor): ver más abajo.
- **Barrido 3 (05/11/2026, noche):** 0 incumplimientos en las 88 auditorías, con las mismas versiones de las
  herramientas. El estado «formato erróneo» del recibo ya no está marcado como fallo conocido y pasa en verde en los
  cuatro casos; `receipt-check.accessibility.spec.ts`, 4 de 4 (92 pruebas en total, 92 pasan). Vitest 19 de 19 y
  compilación correctos. En una primera ejecución completa falló una prueba ajena a DEF-06 (B4 recibo, 1280 px, ES: la
  escritura del código de acceso se quedó en la primera cifra al arrancar y no llegó a la papeleta, por tiempo de
  espera); por separado pasó, y el barrido completo se repitió desde cero con 92 de 92. Los resultados de este informe
  son los de esa repetición.
- **Barrido 4 (08/11/2026; vigente):** repetido sobre `06f39be` (copia exacta con `git archive`, en el entorno de la
  IA), con las mismas herramientas: 88 auditorías, 0 críticos, 0 serios, 0 moderados, 0 menores; 92 de 92 pruebas;
  Vitest 19 de 19 y compilación correctos. Los avisos sin decidir son los mismos que en el barrido 3. La revisión
  manual no se realizó (08/11/2026): parte manual no realizada; sin decisión.

Avisos que axe no puede decidir solo («incomplete»; requieren la revisión manual, no son incumplimientos):
`color-contrast` en B2, B4, B5, C4, V6 y V8 (texto sobre fondos que axe no puede calcular) y `aria-prohibited-attr`
en B4. En el barrido 3 salen los mismos y, además, `color-contrast` en B1 en una sola combinación (360 px, EN); el estado
«formato erróneo» del recibo, con el mensaje nuevo, no da ningún aviso.

**Incidencia que axe no detecta (DEF-06, corregida):** en «Comprobar mi recibo», con un código de formato erróneo,
Angular añadía la clase `ion-invalid` al campo pero Ionic la quitaba al volver a pintarlo, y el mensaje «El código debe
tener 64 caracteres…» no se llegaba a mostrar (WCAG 3.3.1, identificación de errores). Se vio al automatizar ese estado
y la prueba lo marcó como fallo conocido (`test.fail`). Corrección: `receipt-check.page.ts` muestra el mensaje en un
elemento propio dentro de una región `role="alert"` (se anuncia al aparecer) y ya no depende de `errorText` ni de las
clases de Ionic. Se quitó la marca de fallo conocido; la comprobación sigue y se añadió otra (el mensaje está en una
región `role="alert"`). Las dos pasan. Detalle en `resultados-p7/defectos.md`. Queda por comprobar con NVDA que el
anuncio se oye y que se entiende.

## Decisión sobre H1a con estos datos

Criterio (validado por el tutor): cero incumplimientos críticos ni serios en las pantallas evaluadas **y** revisión
manual con teclado y lector de pantalla sin bloqueos en acceso, voto y recibo.

- **Parte automática: favorable** en la versión con DEF-05 y DEF-06 corregidos (barrido 3: 0 críticos y 0 serios en 88
  auditorías).
- **Parte manual: no realizada** antes del cierre (08/11/2026); es una parte del protocolo sin ejecutar, no un
  incumplimiento.
- **Decisión: H1a, parte automática favorable; parte manual no realizada; sin decisión.** No se declara aceptada ni
  rechazada, igual que RNF-4.

## Qué no cubre

- Pantallas de la consola de gestión (login, inicio, censos, procesos, participación, resultados, auditoría,
  trazabilidad, privacidad, claves de API): fuera de esta medición del flujo del votante.
- Página pública de resultados (`/resultados/:publicId`).
- Comportamiento con el backend real (la API se sustituye por respuestas sintéticas).
- Lighthouse: no se ejecutó. Su auditoría de accesibilidad usa un subconjunto de las reglas de axe y no permite
  sustituir la API, así que no añade cobertura a este barrido.
- axe no certifica la conformidad con WCAG.

## Cómo repetirlo

```powershell
cd frontend
npm run test:e2e -- tests/e2e/voter-flow.accessibility.spec.ts
```

Los JSON por pantalla quedan en `%TEMP%\verivoto-p7-e2e\voter-flow-axe\`. La prueba falla si aparece algún
incumplimiento crítico o serio, pero sigue auditando todas las combinaciones.
