# PROTOTYPES · Inventario de `prototiposPantallas/`

Inventario de solo lectura (2026-10-29). La carpeta no se modifica. Los prototipos ceden ante el TFM
cuando hay conflicto (ver «Incoherencias»).

## 1. Ficheros y formato

| Fichero | Formato | Cómo abrirlo | Contenido |
| --- | --- | --- | --- |
| `Prototipos UI UX · Plataforma de votación digital.html` | HTML autocontenido (12,5 MB, fuente Inter embebida) | Doble clic en el navegador | 25 pantallas A1–H1 en tres anchos (1280, 768 y 390 px), sin enlaces entre ellas |
| `Elections · Prototipos de pantallas mejoradas.html` | HTML autocontenido con 3 páginas empaquetadas (gzip + base64) | Navegador | Flujo del votante interactivo con bienvenida (V1); estados V2–V8; gestión de candidatos |
| `Elections · Revisión UI UX y flujo definitivo.md` | Markdown | Editor | Revisión de UX, mejoras priorizadas, paleta, flujo definitivo del votante |
| `Elections · Recomendaciones de mejora para Ionic (TFM).md` | Markdown | Editor | Recomendaciones de componentes Ionic y estados |
| `VeriVoto · Guía de adaptación a Angular + Ionic.md` | Markdown | Editor | Mapa pantalla → RF → componente Ionic, layout responsive, tema |
| `Mejora.png` | Imagen | Visor | Componente «Próximos eventos» (aperturas y cierres con fecha y hora peninsular) para el panel |

## 2. Inventario de pantallas

| Código | Pantalla | Actor | Flujo | RF / RNF |
| --- | --- | --- | --- | --- |
| A1 | Panel de la organización | Administrador | Gestión | RF-14 |
| A2 | Censo e importación CSV | Gestor de censo | Censo | RF-03 |
| A3 | Crear proceso, con éxito final | Administrador | Procesos | RF-06 |
| A4 | Ciclo de vida | Administrador | Procesos | RF-07 |
| A5 | Participación | Administrador | Seguimiento | RF-14 |
| B1 | Acceso del votante (código de 6 dígitos; SSO como alternativa) | Votante | Voto | RF-08, RF-05b |
| B2 | Papeleta | Votante | Voto | RF-09 |
| B3 | Resumen y confirmación | Votante | Voto | RF-09, RNF-7 |
| B4 | Voto registrado y recibo | Votante | Voto | RF-09, RF-10 |
| B5 | Verificación del recibo | Votante | Voto | RF-10, RNF-6 |
| C1 | Selección no válida y procesando voto | Votante | Voto | RNF-7, RNF-8 |
| C2 | Enlace caducado | Votante | Excepción | RF-08 |
| C3 | Proceso no abierto | Votante | Excepción | RF-08 |
| C4 | Acceso bloqueado | Votante | Excepción | RNF-14 |
| C5 | Errores 404 y 500 | Todos | Excepción | RNF-8 |
| D1 | Login de gestión y 2FA | Gestión | Autenticación | RF-04 |
| E1 | Panel global | Superadministrador | Plataforma | RF-01 |
| E2 | Organizaciones | Superadministrador | Plataforma | RF-01 |
| E3 | Administradores y roles | Superadministrador | Plataforma | RF-02 |
| F1 | Resultados y acta | Administrador | Resultados | RF-15, RF-16 |
| F2 | Privacidad RGPD | Administrador | Privacidad | RF-18 |
| F3 | Proceso creado: confirmación | Administrador | Procesos | RF-06 |
| F4 | Escrutinio en curso | Administrador | Resultados | RF-13, RNF-8 |
| G1 | Importando un CSV grande | Gestor de censo | Censo | RF-03 |
| H1 | Registro de la urna y trazabilidad | Auditor | Auditoría | RF-12, RF-19 |
| V1 | Bienvenida | Votante | Voto | RF-08, RNF-9 |
| V2 | Código incorrecto con intentos y reenvío | Votante | Voto | RF-08, RNF-14 |
| V3 | Ya has votado | Votante | Voto | RF-09, RNF-12 |
| V4 | Votación cerrada | Votante | Excepción | RF-07, RF-08 |
| V5 | No habilitado/a | Votante | Excepción | RF-08 |
| V6 | Sesión caducada | Votante | Excepción | RNF-14 |
| V7 | Voto no registrado (reintento seguro) | Votante | Excepción | RF-09, RNF-8, RNF-12 |
| V8 | Resultados públicos (tras el cierre) | Votante | Resultados | RF-13, RF-15 |
| — | Gestión de candidatos | Administrador | Procesos | RF-06 |

## 3. Cobertura del MVP

| RF | Pantallas | Cobertura |
| --- | --- | --- |
| RF-01 | E1, E2 | Sí |
| RF-02 | E3 | Sí |
| RF-03 | A2, G1 | Sí (implementadas, D-36) |
| RF-04 | D1 | Sí |
| RF-05a | D1 | Parcial: no consta un botón de SSO de gestión; por confirmar |
| RF-05b | B1 (alternativa SSO) | Parcial |
| RF-06 | A3, F3, Gestión de candidatos | Sí |
| RF-07 | A4, V4 | Sí |
| RF-08 | V1, B1, V2, C2, C3, C4, V5, V6 | Sí |
| RF-09 | B2, B3, C1, V3, V7 | Sí |
| RF-10 | B4, B5 | Sí |
| RF-12 | H1 | Sí |
| RF-13 | F4, V8 | Sí |
| RF-14 | A1, A5 | Sí |
| RF-15 | F1, V8 | Sí |
| RF-16 | F1 | Sí |
| RF-17 | A5 | Parcial: A5 dibuja «Enviar recordatorio»; la vista de correos y fallidos se diseñó sin prototipo con el estilo de A5 (P6.2, D-45) |
| RF-18 | F2 | Sí: hecha en P6.3 (D-46), sin la línea «Ha votado en» (fila 18) |
| RF-19 | H1 | Sí |
| RF-20 | Selector ES/EN en la cabecera; A3 | Sí: la edición bilingüe se añade en A3 (P4.6, D-35) |
| RF-21 | — | **No**: «Claves de API» se diseñó sin prototipo con el estilo de la consola (P6.4, D-47) |

## 4. Incoherencias con el TFM, los requisitos o las decisiones

| # | Incoherencia | Dónde | Resolución |
| --- | --- | --- | --- |
| 1 | Revoto: «Cambiar mi voto», recibo «sustituido» y aviso «puedes cambiarlo hasta la huella final» | V1, V3, B4 y flujo definitivo (paso 7) del documento de revisión | D-03: fuera del MVP. Se implementan V1, V3 y B4 sin esos elementos (IMP-05) |
| 2 | Las guías citan Ionic 8.6 (`ion-input-otp` «desde 8.6», importaciones `@ionic/angular/standalone`) | Guía de adaptación y recomendaciones | Se usa Ionic 9: importaciones desde `@ionic/angular`, zoneless y `OnPush` por defecto (skill `ionic-angular-screen`) |
| 3 | Selector ES/EN de 40 px de alto | Cabecera del votante | Subir a 44 px (objetivo táctil de RNF-1/RNF-2) |
| 4 | Textos de 13 px | B2, B3, B4 | Subir a 14 px o más |
| 5 | V8 «Resultados públicos» dependía de qué significa «público» en RF-13 | V8 | Resuelto (D-16): V8 se muestra con `PUBLIC_LINK`; con `CENSUS_ONLY` el votante entra con enlace + OTP |
| 8 | V3 «Ya has votado el 12 nov a las 10:14» muestra la hora | V3 | D-18: mostrar solo la fecha («Ya has votado el 12 nov») |
| 6 | El campo de 2FA se llama «Código de verificación» y el recibo «código de recibo» | B1, B4 | Usar «código de acceso» y «código de recibo» (propuesta de la propia revisión) |
| 7 | La marca aparece como «Elections» y como «VeriVoto» | Títulos de los ficheros | Usar VeriVoto (nombre del proyecto) |
| 9 | La tabla del censo muestra filas «Duplicado» y «Error» con «Editar» | A2 | D-27/D-36: las líneas con error o duplicadas nunca se guardan; se muestran en el resumen de la importación y la tabla solo lista personas guardadas |
| 10 | «12.400 de 20.000 líneas · 62 %» y «puedes seguir usando la aplicación» | G1 | La importación es síncrona: indicador indeterminado y cancelar solo durante la validación (D-36); progreso real en IMP-25 |
| 11 | Teléfonos «+34 600 100 201» | A2 | RI-3 solo admite cifras (`^[0-9]{1,15}$`): el formulario lo indica y la tabla muestra el valor guardado |
| 12 | El filtro «Todos» de la tabla del censo | A2 | Sin estados que filtrar en las personas guardadas: solo búsqueda por nombre, correo o identificador |
| 13 | «Participación acumulada por hora» | A5 | D-18/D-38: la participación solo guarda el día; el gráfico es por día. La hora permitiría cruzarla con el orden de los eventos de la urna |
| 14 | Fecha y hora en cada «Voto recibido» y evento «Voto sustituido» | H1 | D-18: los eventos de voto no llevan hora; revoto fuera del MVP (D-03) |
| 15 | «Enviar recordatorio a quien no ha votado» (A5) y trazabilidad de acciones (H1) | A5, H1 | Trazabilidad hecha en P6.1 (D-44) como pantalla propia «Trazabilidad» para ADMIN y AUDITOR (H1 sigue siendo el registro de la urna). Recordatorios en P6.2 (D-45): «Enviar recordatorio ahora» con confirmación (dice a cuántas personas), programación con fecha y hora, lista con estado y número de destinatarios, y una sección «Correos» con recuentos, fallidos y reintento; solo con la votación abierta y sin mostrar quién no ha votado |
| 16 | F4 enumera pasos con su estado («Recuento por opción · completado») | F4 | La urna no informa del avance del escrutinio (dura segundos): F4 lista los pasos sin estado y se sustituye por F1 en cuanto hay resultado (D-39) |
| 17 | V8 no está dibujado | V8 | Diseñado con el estilo del votante a partir de la descripción de la revisión: resultado por opción, participación, huellas y fecha de publicación, con verificación en el navegador (D-39) |
| 18 | F2 muestra «Ha votado en 1 proceso» y avisa «Como esta persona ya ha votado, sus datos se anonimizarán» | F2 | D-46: la pantalla no dice si la persona votó (sería participación individual a la vista del administrador). Se anonimiza toda entrada de un censo que usó algún proceso y se borra el resto, igual para todos; se añade la lista de censos y procesos, y el borrado se bloquea con un proceso programado o abierto. La participación solo va en el fichero exportado, que es para la persona |
| 18 | «Generar acta (PDF)» en F1 | F1 | Hecho en P5.4 (D-40): botón en la cabecera (junto al CSV) y tarjeta «Acta del proceso» con la huella SHA-256 del fichero; solo ADMIN |
| 19 | No hay pantalla para ver el resultado con el enlace personal (`CENSUS_ONLY`) | V1, V8, C2 | Diseñado en P5.5 (D-41) con las piezas existentes: bienvenida tipo V1 «Resultado de la votación» con «Ver el resultado», el paso B1/V2 del código sin indicador de pasos y el resumen de V8 con «Tu voto quedó registrado el …»; C2 añade que el resultado aparecerá en el mismo enlace |

Comprobado sin incoherencias: indicador de 4 pasos (RNF-9), confirmación explícita en pantalla propia
(RNF-7), ES/EN (RNF-5), anchos de 390 px (cubre los 360 px de RNF-2 con diseño fluido), recuento por
opción oculto hasta el cierre (RF-14).

## 5. Sistema visual (para P3.4)

Tipografía Inter con `system-ui` de reserva. Paleta: cabecera `#0F2A47` (`tertiary`), selección
`#14365C` (`primary`), acción principal `#1F7A5A` (`secondary`), error `#8F1F14` sobre `#FCE9E7`
(`danger`), aviso `#7A4B00` sobre `#FFF4DC` (`warning`), éxito `#14573F` sobre `#E6F4EE` (`success`).
Detalle y contrastes en `Elections · Revisión UI UX y flujo definitivo.md`.

## 6. Aplicación en P3.4 (frontend base)

Revisado el 2026-10-29 contra la guía de adaptación, la revisión UI/UX y las recomendaciones para Ionic.

| Punto de los prototipos | Estado |
| --- | --- |
| Paleta en variables Ionic (`primary`, `secondary`, `tertiary`, `success`, `warning`, `danger`, `medium`), fondo y texto | Hecho (`src/theme/variables.scss`) |
| Tipografía Inter con `system-ui` de reserva | Hecho, autoalojada (`@fontsource-variable/inter`) |
| Escala (h1 24/28 px, h2 18 px, cuerpo 16 px, ayuda ≥ 14 px), radios 10/12 px, borde `#C9D1DB`, foco 3 px `#14365C`, botones ≥ 44 px | Hecho (`src/styles.scss`) |
| `mode: 'md'` en todos los dispositivos | Hecho |
| `ion-split-pane` desde 992 px + hamburguesa en móvil; menú único filtrado por rol | Hecho (`AdminShellComponent`) |
| Cabecera de gestión con organización, rol y usuario, distinta de la del votante | Hecho desde 768 px (`AdminPageComponent`) |
| Selector ES/EN de 44 px; cambio en caliente (ngx-translate) | Hecho |
| «Conexión segura» en el pie del votante | Hecho (`VoterEntryPage`) |
| Foco al `h1` al cambiar de pantalla | Hecho (`FocusOnNavigationService`) |
| Áreas por rol con carga bajo demanda y `canMatch` | Hecho (rutas `/admin/...`, ver D-28) |
| `ErrorState` | Hecho; en P4.5: `StepIndicator`, barra de estado, recibo (B4), opciones de 64 px (B2) y campo de código `ion-input-otp` (B1). `LoadingState` y `StatusBadge` con las pantallas de gestión |
| Flujo del votante V1, B1/V2, B2/C1, B3, B4, B5 y estados C2–C4, V3–V7 | Hecho (P4.5, D-33). V5 se muestra como «enlace no válido»; sin «Cambiar mi voto» (D-03) |
| Login D1 con el aspecto de los prototipos | Pendiente: lo pinta Keycloak con su tema (IMP-14) |
| Aviso antes de caducar la sesión | Pendiente (IMP-15) |
| Tarjeta «Próximos eventos» (`Mejora.png`) | En el panel A1 (P5) |
