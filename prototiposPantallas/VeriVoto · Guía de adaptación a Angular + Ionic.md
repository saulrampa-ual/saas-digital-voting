# VeriVoto · Guía de adaptación a Angular + Ionic

Oct 24, 2026 · @Saul

Los prototipos se traducen casi por completo a componentes de Ionic; solo el gráfico circular, el asistente por pasos y las tablas grandes necesitan código propio o una librería.

## Mapa de pantallas, requisitos y componentes

Cada pantalla de los prototipos se construye con los componentes de la última columna; los estados de carga usan `ion-spinner` e `ion-progress-bar` (RNF-8).

| Pantalla | Rol | Requisitos | Componentes Ionic clave |
| --- | --- | --- | --- |
| Login y 2FA | Gestión | RF-04, RF-05, RNF-14 | `ion-input` (email, contraseña con `ion-input-password-toggle`), `ion-input-otp`, `ion-button` |
| Panel global | Superadministrador | RF-01 | `ion-split-pane`, `ion-menu`, `ion-card`, `ion-grid`, `ion-progress-bar` |
| Organizaciones | Superadministrador | RF-01, RNF-15 | `ion-list` e `ion-item` (móvil), rejilla CSS (escritorio), `ion-modal` (alta), `ion-alert` (suspender), `ion-badge` |
| Administradores y roles | Superadministrador, Administrador | RF-02 | `ion-select`, `ion-item`, `ion-alert`, `ion-toast` |
| Panel de la organización | Administrador | RF-14, RNF-13 | `ion-card`, `ion-grid`, `ion-progress-bar`, `ion-badge` |
| Crear proceso y éxito | Administrador | RF-06, RNF-4, RNF-8 | `ion-input`, `ion-textarea`, `ion-datetime-button`, `ion-radio-group`, `ion-checkbox`, `ion-toast` |
| Ciclo de vida | Administrador | RF-07, RF-19 | `ion-list`, `ion-badge`, `ion-button`, `ion-alert` (confirmar) |
| Participación | Administrador | RF-14, RNF-11 | `ion-card`, gráfico (ver librerías), `ion-chip` |
| Resultados y actas | Administrador | RF-13, RF-15, RF-16 | `ion-card`, `ion-progress-bar`, `ion-button`, `ion-spinner` |
| Privacidad RGPD | Administrador | RF-18 | `ion-input`, `ion-checkbox`, `ion-alert` (confirmar borrado) |
| Escrutinio en curso | Administrador | RF-13, RNF-8 | `ion-spinner`, `ion-progress-bar`, `ion-list` |
| Censo e importación CSV | Gestor de censo | RF-03, RNF-8 | `ion-searchbar`, `ion-select`, `ion-list`, `ion-infinite-scroll`, `ion-progress-bar` |
| Auditoría y trazabilidad | Auditor | RF-12, RF-19, RNF-13 | `ion-card`, `ion-list`, `ion-datetime`, `ion-select` |
| Acceso del votante | Votante | RF-08, RNF-14 | `ion-input-otp`, `ion-button` |
| Papeleta | Votante | RF-09, RNF-1, RNF-7 | `ion-radio-group` y `ion-radio` dentro de `ion-item`, `ion-button` |
| Resumen y confirmación | Votante | RF-09, RF-11, RNF-7 | `ion-card`, `ion-button` |
| Procesando voto | Votante | RNF-8, RNF-12 | `ion-spinner`, `ion-progress-bar` |
| Recibo y verificación | Votante | RF-10, RNF-6 | `ion-card`, `ion-input`, `ion-accordion`, `ion-button` |
| Errores (caducado, no abierto, bloqueo, 404, 500) | Todos | RF-08, RNF-8, RNF-14 | un componente propio `ErrorState` con `ion-icon`, `ion-text` e `ion-button` |

## Layout responsive y navegación

El menú lateral de escritorio y el menú hamburguesa del móvil son el mismo `ion-menu`; lo que cambia es cómo se muestra.

- **Escritorio y tablet:** `ion-split-pane` deja el menú fijo a la izquierda a partir de un ancho que eliges (`when="lg"`, 992 px, o `when="md"`, 768 px). Para el menú de solo iconos de la tablet, Ionic no trae un modo «rail»: se resuelve con una clase CSS que oculta las etiquetas y reduce `--side-width`.
- **Móvil:** por debajo de ese ancho el menú se oculta y se abre con `ion-menu-button` dentro de `ion-toolbar`, que es la hamburguesa de los prototipos.
- **Cabecera y contenido:** `ion-header` con `ion-toolbar`, y `ion-content` para el cuerpo; el contenido de las pantallas de gestión va dentro de un contenedor con `max-width`.
- **Columnas:** `ion-grid` e `ion-col` con `size-md` y `size-lg` sirven para las tarjetas de métricas; para las tablas y las rejillas de dos columnas es más simple CSS Grid con `repeat(auto-fit, minmax(...))`, como en los prototipos.
- **Tablas a tarjetas:** en móvil cada fila se convierte en un `ion-item` o `ion-card`. Conviene un componente único que decide con un `signal` de ancho, no dos plantillas duplicadas.
- **Objetivos táctiles:** los botones del votante deben medir al menos 44 px de alto (`--min-height` en `ion-button`).

## Tema con variables CSS

Ionic se tematiza en `src/theme/variables.scss`. Cada color necesita seis variables (base, `-rgb`, `-contrast`, `-contrast-rgb`, `-shade`, `-tint`); los valores `-shade` y `-tint` de abajo son aproximados y conviene generarlos con el Color Generator de la documentación de Ionic.

```css
:root {
  /* Azul marino: cabeceras, menú, botones secundarios */
  --ion-color-primary: #14365c;
  --ion-color-primary-rgb: 20, 54, 92;
  --ion-color-primary-contrast: #ffffff;
  --ion-color-primary-contrast-rgb: 255, 255, 255;
  --ion-color-primary-shade: #123051;
  --ion-color-primary-tint: #2c496a;

  /* Verde: acción principal (Votar, Crear, Confirmar) */
  --ion-color-secondary: #1f7a5a;
  --ion-color-secondary-rgb: 31, 122, 90;
  --ion-color-secondary-contrast: #ffffff;
  --ion-color-secondary-contrast-rgb: 255, 255, 255;
  --ion-color-secondary-shade: #1b6b4f;
  --ion-color-secondary-tint: #35876a;

  /* Rojo apagado: borrar, suspender, errores */
  --ion-color-danger: #8f1f14;
  --ion-color-danger-rgb: 143, 31, 20;
  --ion-color-danger-contrast: #ffffff;
  --ion-color-danger-contrast-rgb: 255, 255, 255;
  --ion-color-danger-shade: #7e1b12;
  --ion-color-danger-tint: #9a352b;

  --ion-background-color: #f3f5f8;
  --ion-text-color: #1b2733;
  --ion-font-family: 'Inter', system-ui, sans-serif;
}
```

- **Contraste (RNF-1):** blanco sobre el azul marino da más de 11:1, sobre el verde unos 5,3:1 y sobre el rojo unos 8:1, así que los tres pasan AA con texto normal. El gris de texto secundario de los prototipos (`#4A5868`) también.
- **Un color por significado:** usa `color="secondary"` para la acción principal de cada pantalla y `color="danger"` solo para acciones destructivas; no pongas el estado solo en el color, como hacen los prototipos con una etiqueta de texto.
- **Foco visible:** no quites el anillo de foco de Ionic; WCAG 2.2 AA exige que el foco sea claramente visible.
- **Modo oscuro:** si lo quieres después, Ionic trae paletas listas en `@ionic/angular/css/palettes/dark.system.css`; hay que volver a validar los contrastes.

## iOS frente a Android

Ionic aplica por defecto el estilo de cada plataforma: modo `ios` en iPhone e iPad, modo `md` (Material Design) en Android y en el navegador de escritorio. Las diferencias que importan para VeriVoto:

| Elemento | iOS | Android / Material |
| --- | --- | --- |
| Botón Atrás (`ion-back-button`) | Chevron con el título anterior; gesto de deslizar desde el borde izquierdo | Flecha; el botón físico o el gesto del sistema vuelve atrás |
| Cabecera | Título centrado; opción de título grande que se encoge al hacer scroll (`collapse="condense"`) | Título a la izquierda |
| Modales (`ion-modal`) | Pueden abrirse como hoja o tarjeta sobre la pantalla anterior (`presentingElement`, `breakpoints`) | Pantalla completa o diálogo centrado |
| Alertas y confirmaciones (`ion-alert`) | Botones en fila o apilados al estilo iOS | Botones de texto a la derecha |
| Botones y toques | Sin ondulación | Efecto ripple |
| Selectores de fecha | Rueda o calendario nativo de iOS | Calendario Material |

Recomendaciones:

- **Fija un solo modo** con `provideIonicAngular({ mode: 'md' })` si quieres que la aplicación se vea igual que los prototipos en todos los dispositivos. Es la opción más simple y ayuda a la consistencia (RNF-10); pierdes la apariencia nativa de iOS.
- **El votante entra desde un enlace del correo,** así que en la práctica lo usa una PWA o el navegador (RNF-18); aun así prueba el flujo en Safari de iOS y en Chrome de Android.
- **Atrás en el flujo de voto:** no dejes que Atrás vuelva a la pantalla de «Procesando voto» ni reenvíe el voto. Usa `replaceUrl: true` al navegar a la confirmación y desactiva el gesto de deslizar en esa pantalla.
- **Zonas seguras:** Ionic ya respeta `env(safe-area-inset-*)` en `ion-header` e `ion-footer`; si colocas botones fijos abajo, ponlos en un `ion-footer`.

## Qué requiere librería o código propio

Ionic cubre formularios, listas, tarjetas, modales y estados de carga. Estas piezas de los prototipos no vienen incluidas:

| Necesidad | Recomendación |
| --- | --- |
| Gráfico circular de participación | Para un solo valor, un componente propio con SVG, como en los prototipos (sin dependencias). Si habrá más gráficos, `ng2-charts` (Chart.js) con `doughnut` y `bar`; `ngx-charts` también vale, pero pesa más. |
| Barras de participación por hora | `ng2-charts`. Las barras horizontales de resultados se resuelven con `ion-progress-bar`. |
| Asistente por pasos (crear proceso) | Ionic no tiene stepper: componente propio con una `signal` de paso y `aria-current="step"`, o `@angular/cdk/stepper`. |
| Tablas grandes (censo de miles de filas) | `@angular/cdk/scrolling` (virtual scroll) con filas en `ion-item`; mejor paginar y filtrar en el servidor. AG Grid solo si hace falta ordenar columnas en el cliente. |
| Vista previa de un CSV grande | PapaParse en un Web Worker para no bloquear la pantalla; la validación definitiva la hace el servidor (RF-03). |
| Cambio de idioma sin perder datos (RNF-5) | `@ngx-translate/core` o Transloco. La i18n nativa de Angular genera una compilación por idioma y no cambia en caliente. |
| Aplicación instalable (RNF-18) | `@angular/pwa` (service worker y manifiesto). |
| Código de un solo uso | `ion-input-otp` existe desde Ionic 8.6; en una versión anterior, un `ion-input` con `inputmode="numeric"` y `autocomplete="one-time-code"`. |
| Copiar el código del recibo | `navigator.clipboard`; en una app nativa, el plugin Clipboard de Capacitor. |
| Descarga de acta PDF y CSV | El servidor genera el fichero (RF-15, RF-16); en el navegador es una descarga directa. |
| Línea de estados del proceso | Componente propio con CSS: Ionic no incluye una línea de tiempo. |
| Auditoría de accesibilidad (RNF-1) | `axe-core` con Playwright o Cypress, más Lighthouse; no sustituyen la prueba manual con teclado y lector de pantalla. |

## Roles y rutas

Un solo proyecto Angular con rutas separadas por área, cargadas bajo demanda, evita mezclar la gestión con la votación.

1. **Rutas públicas del votante** (`/votar/:token`): acceso, papeleta, resumen, recibo y verificación. No dependen del inicio de sesión de gestión: se identifican con el enlace personal y el código (RF-08).
2. **Login de gestión** (`/acceso`): correo, contraseña y código de verificación (RF-04), con la vía alternativa de SSO (RF-05).
3. **Áreas por rol** con `loadChildren`: `/global` (Superadministrador), `/org` (Administrador), `/censo` (Gestor de censo) y `/auditoria` (Auditor). Cada área se protege con un guard `canMatch` que lee el rol de la sesión, de modo que un rol no descarga el código de las demás.
4. **Menú por rol:** un único `ion-menu` que pinta los elementos según el rol, como en los prototipos, para mantener la misma ubicación de la navegación (RNF-10).
5. **Componentes compartidos:** `ErrorState`, `LoadingState` (spinner y barra de progreso), `RoleBanner`, `StatusBadge` y `StepIndicator`, usados por todas las pantallas.

Los guards solo ocultan pantallas; la seguridad real, el aislamiento entre organizaciones (RNF-15) y los permisos se comprueban siempre en el servidor.
