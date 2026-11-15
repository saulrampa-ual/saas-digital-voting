# A4 · Barrido axe de la consola de gestión y de los resultados públicos (08/11/2026)

Segundo bloque de la parte automática de accesibilidad (tarea A4 de `docs/ai/PLAN.md`). El primero, el flujo del
votante, está en `accesibilidad-axe-2026-11-05.md`. Este barrido **no cambia la decisión de H1a**: el criterio de H1a
se refiere a las pantallas del flujo de voto; la consola se mide como evidencia de RNF-1 en el resto de la aplicación.
La única pantalla de este bloque que ve un votante (V8, resultados públicos) da 0 incumplimientos.

## Qué se midió

| Elemento | Valor |
| --- | --- |
| Fecha | 08/11/2026 (barrido 1 y barrido 2, tras corregir DEF-07) |
| Versión evaluada | `dev` en `06f39be` (copia exacta con `git archive`) más la prueba nueva; barrido 2 con la corrección de DEF-07, sin commit |
| Prueba | `frontend/tests/e2e/admin-console.accessibility.spec.ts` |
| Herramientas | Playwright 1.64.1 · Chrome for Testing 153.0.8010.12 · axe-core 4.14.0 · `@axe-core/playwright` 4.14.0 (las mismas que el barrido del votante) |
| Aplicación | Angular 22.2.4 · Ionic 9.0.10 · servidor de desarrollo (`ng serve`) · Node.js 22.23.4 |
| Reglas | Etiquetas `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa`, `wcag22aa` |
| Combinaciones | 27 pantallas o estados × 2 anchos (1280 y 360 px) × 2 idiomas (ES, EN) = **108 auditorías** |
| Datos | Keycloak y API simulados con respuestas sintéticas conformes a los contratos (identificador de organización con el formato de identity-service; huellas del resultado coherentes, la verificación en el navegador sale «verificado»). Sin backend, sin datos reales |
| Entorno | Entorno de la IA (Linux); resultados JSON y capturas fuera del repositorio |

El inicio de sesión se simula interceptando el *Authorization Code* de Keycloak (pantalla de login y token sintético
con el rol y la organización); **las pantallas de login de Keycloak no se miden aquí** (son plantillas de Keycloak; se
midieron con axe el 03/11/2026 sobre el Keycloak real, ver `PROGRESS.md`).

**Pantallas y estados (27):** inicio de cada rol (superadministrador, administrador, gestor de censo, auditor) y el
inicio con el menú lateral desplegado; organizaciones; censos, detalle de un censo con personas y de un censo vacío;
procesos; nuevo proceso (paso 1, y paso 1 con errores de validación); editar proceso (paso 1 y paso 4, papeleta);
detalle de un proceso abierto y de uno escrutado; participación con el panel de correos; resultados de un proceso
escrutado y de uno sin escrutar; auditoría de la urna; trazabilidad; claves de API; privacidad (inicial y con una
persona encontrada); «sin permiso»; resultados públicos (V8); página 404.

## Resultado

| Severidad (impacto axe) | Barrido 1 (`06f39be`) | Barrido 2 (con DEF-07 corregido) |
| --- | ---: | ---: |
| Críticos | 0 | **0** |
| Serios | 3 | **0** |
| Moderados | 0 | 0 |
| Menores | 0 | 0 |

- **Barrido 1:** las 3 incidencias serias eran la misma, **DEF-07** (`scrollable-region-focusable`, WCAG 2.1.1), solo
  a 360 px: detalle de un censo con personas en ES (en EN la tabla cabe y pasa) y tabla de correos no entregados de
  Participación en ES y EN. El contenedor `.table-wrap` (`overflow-x: auto`) se desplaza en horizontal y no recibía el
  foco, así que con teclado no se podía ver la parte cortada. Las otras 24 pantallas o estados daban 0 en las 4
  combinaciones, incluidos los resultados públicos (V8) y la 404.
- **Corrección de DEF-07** (opción A, aprobada por el autor el 08/11/2026): cada `.table-wrap` de la consola es una
  región que recibe el foco con nombre (`tabindex="0"`, `role="region"` y `aria-labelledby` al título de su tabla o
  `aria-label` con el mismo texto). No cambia el aspecto: el contorno de foco es el global de `styles.scss`. Afecta al
  detalle del censo, al panel de correos de Participación (3 tablas), claves de API, privacidad, auditoría y
  trazabilidad (ver `resultados-p7/defectos.md`).
- **Barrido 2 (08/11/2026, vigente):** mismas herramientas y datos: 108 auditorías, 0 críticos, 0 serios, 0 moderados,
  0 menores. Se quitó el `test.fail` de las 3 combinaciones de DEF-07: **108 de 108 pruebas en verde sin marcas**.
  Vitest 19 de 19 y `npm run build` correctos. No aparece ningún otro incumplimiento.

La primera ejecución completa del barrido 1 usó un identificador de organización de 36 caracteres, que no cumple el
formato real (máximo 20 letras o cifras): se corrigió el dato y se repitió; los incumplimientos no cambiaron.

## Avisos pendientes de decisión

axe no puede decidir solo estos casos («incomplete»); no son incumplimientos y **quedan pendientes de decisión** del
autor (revisión manual o aceptar como limitación). Son los mismos en los dos barridos:

| Regla | Dónde | Motivo |
| --- | --- | --- |
| `color-contrast` | 8 pantallas: procesos, nuevo proceso con errores, editar proceso (papeleta), participación, censos, detalle de censo, inicio con el menú desplegado, privacidad con una persona encontrada | Texto sobre fondos que axe no puede calcular (degradados, superposiciones). El conjunto exacto varía entre ejecuciones |
| `aria-valid-attr-value` | Editar proceso, paso 4 (papeleta) | Un atributo ARIA cuyo valor axe no puede comprobar (referencia a un elemento que puede no existir en ese momento) |

## Qué no cubre

- Las pantallas de login, contraseña y código de Keycloak (medidas aparte el 03/11/2026).
- El backend real: las respuestas son sintéticas.
- Diálogos y acciones que modifican datos (crear censo, importar CSV, programar, revocar una clave, borrar datos):
  solo los estados que se alcanzan sin enviar cambios, salvo la búsqueda de privacidad y la validación del formulario.
- axe no certifica la conformidad con WCAG ni sustituye una revisión manual.

## Cómo repetirlo

En Windows (repetición local; la ejecución de referencia de este informe no se hizo en Windows):

```powershell
cd frontend
npx playwright test tests/e2e/admin-console.accessibility.spec.ts
```

Ejecución de referencia (entorno de la IA, Linux, Chrome for Testing 153 del sistema, 08/11/2026, barrido 2):

```text
npx playwright test tests/e2e/admin-console.accessibility.spec.ts
108 passed (4.4m)
```

Los JSON por pantalla quedan en `%TEMP%\verivoto-p7-e2e\admin-console-axe\` (con `E2E_SCREENSHOTS=1`, también una
captura por pantalla).
