# Elections · Recomendaciones de mejora para Ionic (TFM)

Oct 24, 2026 · @Saul

## Resumen

Las mejoras de este documento son cambios sobre los prototipos existentes, no un rediseño, y cada una indica el componente de Ionic que la implementa. Las cinco más urgentes son: confirmación con advertencia explícita y botón destacado, un sistema único de errores, estados de «ya votaste» y «votación cerrada», foco y orden de tabulación correctos al cambiar de pantalla, y subir los textos de 12–13 px a 14 px o más.

**Supuestos.** Trabajo sobre el archivo de prototipos adjunto, que corresponde a VeriVoto: acceso por enlace personal y código por email, papeleta de opción única, resumen, recibo y panel de gestión. No contiene el modal de Voter ID, la selección de categoría por cargo ni el contador de participación en la cabecera que mencionas. Para esos tres puntos doy la recomendación como cambio nuevo. La captura de «Próximos eventos» que has enviado la uso como referencia de estilo en las secciones 5 y 7.

**Prioridad.** C = crítica para el TFM (seguridad, confianza o WCAG AA); D = deseable.

## 1. Accesibilidad (WCAG 2.1 AA)

La base del votante ya cumple lo esencial (contrastes, `lang="es"`, etiquetas en los campos, `role="alert"` en errores, `role="status"` en la carga). Lo que queda son cuatro huecos y un par de textos pequeños.

| # | Prio. | Criterio | Hallazgo | Cambio en Ionic |
| --- | --- | --- | --- | --- |
| 1 | C | 1.4.3 Contraste (4,5:1) | Los pares de color del prototipo pasan: blanco sobre `#14365C` 12,3:1, sobre `#1F7A5A` 5,3:1, gris `#4A5868` sobre `#F3F5F8` 6,7:1. En la captura de eventos, el «JUN» de la ficha de fecha y la hora en gris claro parecen quedar por debajo de 4,5:1 | Medir con el Color Generator de Ionic o axe y oscurecer el gris de texto secundario a `#4A5868` |
| 2 | C | 1.4.4 Cambio de tamaño del texto | Hay textos de 12–13 px (etiquetas en mayúsculas, ayudas, «JUN») | Tamaño mínimo 14 px en `ion-note` y `ion-label`; usar `rem` y probar con zoom del 200 % |
| 3 | C | 2.4.7 Foco visible | No hay un estilo de foco propio definido: dependería del navegador | No eliminar el anillo de Ionic; definir `--ion-focus-ring` y un contorno de 3 px en `ion-button`, `ion-radio` e `ion-input` |
| 4 | C | 2.4.3 Orden del foco | En una app de una sola página, el foco no se mueve solo al cambiar de pantalla | Al entrar en cada página, `ionViewDidEnter` mueve el foco al `h1`; en errores, al contenedor con `role="alert"` |
| 5 | C | 3.3.1 y 3.3.3 Errores | El error de papeleta existe; falta el de código incorrecto | `ion-input` con `error-text` y `aria-describedby`; `aria-invalid="true"` cuando falle |
| 6 | C | 4.1.2 Nombre, función y valor | Botones solo con icono (ayuda, idioma) sin nombre | `aria-label="Ayuda"` en cada `ion-button` de icono; el selector ES/EN con `aria-pressed` |
| 7 | C | 2.5.5 y 2.5.8 Tamaño del objetivo | El selector ES/EN mide 40 px de alto; el resto cumple 44 px | `--min-height: 44px` en todos los `ion-button` y `ion-item` pulsables |
| 8 | D | 2.1.1 Teclado | La papeleta con radios es navegable; falta comprobar el orden con Tab y las flechas | `ion-radio-group` con `ion-radio` dentro de `ion-item`; probar con teclado y con TalkBack y VoiceOver |
| 9 | D | 1.3.1 Información y relaciones | El indicador de pasos es visual | `<nav aria-label="Pasos para votar">` y `aria-current="step"` en el paso actual |
| 10 | D | 1.4.1 Uso del color | Los avisos llevan icono y texto | Mantener la norma: nunca estado solo por color (la ficha «Apertura»/«Cierre» de la captura cumple porque lleva texto) |
| 11 | D | 2.2.1 Tiempo ajustable | Falta aviso previo a la caducidad de sesión | Diálogo con `role="alertdialog"` 2 minutos antes, con «Seguir aquí» |

Para comprobarlo en el desarrollo: `axe-core` con Playwright en cada pantalla, Lighthouse, y una pasada manual con teclado y lector de pantalla antes de entregar.

## 2. Responsive y mobile-first

Los prototipos ya tienen tres anchos (1280, 768 y 390 px) con botones apilados en móvil. Estos son los ajustes por pantalla; todos parten del móvil y crecen con `ion-grid` o CSS Grid.

| Pantalla | Ajuste para móvil | Componente Ionic |
| --- | --- | --- |
| Bienvenida (nueva) | Una sola columna, 3 pasos en lista numerada, botón «Empezar» fijo al pie | `ion-content` + `ion-footer` con `ion-button expand="block"` |
| Voter ID, si se mantiene | Pantalla completa en móvil, no modal: un modal tapa el teclado. `inputmode="numeric"` si el código es numérico y `autocapitalize="characters"` si no | Página con `ion-input`; `ion-modal` solo en tablet y escritorio |
| Código de 6 dígitos (2FA) | Campo de 60 px de alto con `autocomplete="one-time-code"` para que iOS y Android rellenen el código del email o SMS; que el botón no quede tapado por el teclado | `ion-input-otp` (Ionic 8.6 o superior) o `ion-input`; `ion-footer` con `keyboardAttach="bottom"` |
| Lista de candidatos | Tarjetas apiladas de 64 px o más de alto con todo el área pulsable; foto de 48 px a la izquierda; en tablet, 2 columnas | `ion-radio-group` con `ion-item` y `ion-thumbnail`; `ion-grid` con `size-md="6"` |
| Varios cargos, si los hay | Un cargo por pantalla con «Cargo 1 de 3»; evita desplazamiento largo | Asistente propio con `signal` de paso (Ionic no trae stepper) |
| Confirmación | Resumen arriba, aviso en el medio y botón «Confirmar» en el pie, a la altura del pulgar | `ion-footer` con `ion-toolbar`; respeta el área segura de iPhone |
| Recibo | Código en monoespaciada de 21 px, «Copiar» con respuesta «Copiado» | `navigator.clipboard` o plugin Clipboard de Capacitor; `ion-toast` |
| Administrador | En móvil, tablas a tarjetas (una fila = un `ion-item`); menú a hamburguesa | `ion-split-pane` y `ion-menu`; un componente que decide con una `signal` de ancho |

Reglas generales: contenido con `max-width` de 560 px centrado en tablet; separación de 12 px entre botones apilados; sin desplazamiento horizontal a 320 px; `ion-header` y `ion-footer` para las zonas seguras. Si quieres que la app se vea igual en iOS y Android, fija `mode: 'md'` con `provideIonicAngular`.

## 3. Pantalla de confirmación del voto

La pantalla actual ya separa la papeleta del resumen. Para máxima claridad falta una advertencia sobre cuándo el voto deja de poder cambiarse y un orden de acciones que no invite al toque accidental. Una nota sobre la «irreversibilidad»: tu regla permite cambiar el voto hasta el cierre o la huella final, así que la advertencia no debe decir «irreversible» a secas, sino «definitivo después del cierre».

Orden de arriba abajo:

1. **Título** «Revisa tu voto» (`h1`) y una frase: «Comprueba tu selección antes de confirmar».
2. **Resumen por cargo.** Una tarjeta por cargo con el nombre del cargo, la candidatura elegida en 24 px en negrita y su lista. Si hay varios cargos, el voto en blanco de un cargo se muestra con su etiqueta «Voto en blanco» y un icono, nunca como un hueco. Cada tarjeta lleva un enlace «Cambiar» que vuelve a ese cargo (`ion-item` con `button`).
3. **Advertencia** en `ion-card` ámbar con icono: «Aún no has votado. Tu voto se registra cuando pulses «Confirmar y votar». Podrás cambiarlo hasta el cierre (19 nov, 21:00) o hasta que se publique la huella final; después será definitivo». Texto de 15 px, contraste 6,8:1.
4. **Casilla opcional** «He revisado mi selección» (`ion-checkbox`) solo si el proceso es de alto riesgo; para una elección estudiantil normal basta con la pantalla de resumen aparte, y una casilla añade fricción.
5. **Botones apilados en `ion-footer`.** «Confirmar y votar» primero, `color="secondary"` (verde), 52 px de alto, ancho completo; debajo «Cambiar mi selección», `fill="outline"`. 12 px de separación entre ambos.

Al pulsar «Confirmar»: el botón pasa a `disabled` con `ion-spinner` dentro, se navega con `replaceUrl: true` a «Procesando» y se desactiva el gesto de deslizar atrás (`swipeGesture="false"` en `ion-router-outlet`). La petición lleva una clave de idempotencia para que un doble toque no cuente dos veces.

## 4. Sistema unificado de errores

Un solo componente `ErrorState` (icono, título, texto, «qué puedes hacer» y hasta dos botones) sirve para las pantallas completas, y un `ion-toast` o aviso en línea para los errores de campo. El prototipo ya usa ese patrón en enlace caducado, proceso no abierto, acceso bloqueado, 404 y 500; faltan los casos de la tabla marcados como nuevos.

Reglas de redacción: dice qué ha pasado, si el voto se ha registrado o no, y qué hacer; sin códigos técnicos; en español institucional y de tú.

| Situación | Mensaje | Presentación | Acción |
| --- | --- | --- | --- |
| Voter ID inválido (nuevo) | «No reconocemos este código. Revisa que lo has escrito igual que en tu carné» | En línea bajo el campo, `error-text`; no revela si el código existe | Reintentar; tras 5 fallos, bloqueo temporal |
| Código 2FA incorrecto (nuevo) | «El código no es correcto. Te quedan 3 intentos» | En línea, `role="alert"` | Reintentar; reenviar con cuenta atrás |
| Código 2FA caducado (nuevo) | «Este código ha caducado. Te enviamos uno nuevo a l•••@universidad.edu» | Aviso en línea ámbar | Escribir el código nuevo |
| Demasiados intentos | «Acceso bloqueado temporalmente» con cuenta atrás de 15 min | Pantalla completa (existe, C4) | Esperar o contactar |
| Error de red al votar (nuevo) | «Tu voto NO se ha registrado. Tu selección sigue guardada» | Pantalla completa, nunca solo un toast | «Reintentar» idempotente |
| Sin conexión (nuevo) | «Sin conexión. Reconecta para continuar» | Banda fija bajo la cabecera, `role="status"` | Se oculta sola al volver |
| Votación ya emitida (nuevo) | «Ya has votado el 12 nov a las 10:14» | Pantalla completa informativa, no de error | Verificar recibo; cambiar voto si la regla lo permite |
| Votación cerrada o no abierta | «La votación ha terminado» o «todavía no está abierta» con fechas | Pantalla completa (la segunda existe, C3) | Verificar recibo o añadir al calendario |
| Enlace caducado | «Este enlace ha caducado» | Pantalla completa (existe, C2) | Solicitar enlace nuevo |
| Error del servidor | «Algo no ha ido bien por nuestra parte» | Pantalla completa (existe, C5) | Reintentar o volver al inicio |

Colores: ámbar para avisos recuperables, rojo apagado `#8F1F14` para fallos, azul para información; siempre con icono y texto.

## 5. Carga y feedback

Regla de oro: toda acción que tarda más de 300 ms muestra algo, y toda acción que cambia el estado del voto termina con un mensaje de éxito o de fracaso que dice si el voto consta.

| Momento | Feedback | Componente Ionic |
| --- | --- | --- |
| Enviar el código por email | Botón «Enviando…» con spinner; después «Código enviado a l•••@universidad.edu» | `ion-button` deshabilitado + `ion-spinner`; `ion-toast` |
| Verificar el código | Botón «Verificando…»; banda verde «Enlace válido» al acertar | `ion-spinner`; `ion-card` verde |
| Cargar la papeleta | Esqueleto de 4 tarjetas con la forma de las opciones, nunca un spinner a pantalla completa | `ion-skeleton-text` con `animated` dentro de `ion-item` |
| Registrar el voto | Pantalla propia con spinner, barra indeterminada y «no cierres esta ventana» | `ion-spinner`, `ion-progress-bar type="indeterminate"`, `aria-live="polite"` |
| Éxito del voto | Icono, «¡Voto registrado!» y código de recibo | `ion-card`, `ion-icon` |
| Copiar el recibo | «Copiado ✓» durante 2,5 s, anunciado al lector de pantalla | `ion-toast` o cambio de etiqueta con `aria-live` |
| Fracaso | Pantalla de error con el voto marcado como NO registrado | `ErrorState` |
| Listas largas (censo, auditoría) | Esqueleto y `ion-infinite-scroll` | `ion-skeleton-text`, `ion-infinite-scroll` |

Si el usuario tiene activado «reducir movimiento», desactiva animaciones con `@media (prefers-reduced-motion: reduce)`.

### Contador de participación en la cabecera

El prototipo no lo tiene: la cabecera lleva marca, organización e idioma, y la participación solo aparece en el panel de administración. Si decides enseñárselo al votante, estas condiciones lo hacen seguro:

- Solo el porcentaje agregado del censo («64 % ha votado»), redondeado y nunca por candidatura.
- Activable por proceso (por defecto, apagado): puede influir en quien aún no ha votado y en votaciones con poca gente un número exacto puede identificar quién falta.
- Se actualiza cada 30 s, no en tiempo real, y con `aria-live="off"` para no interrumpir al lector de pantalla.
- Va en la barra de estado bajo la cabecera, junto a la fecha de cierre, no dentro de la cabecera: «Proceso abierto · 64 % ha votado · Cierra el 19 nov, 21:00».
- El recuento por opción no se muestra hasta el cierre, ni siquiera al administrador, salvo que el reglamento lo permita.

## 6. Pantallas faltantes

Seis pantallas completan el flujo; las dos de estado («ya votaste» y «cerrada») son las críticas y ya están dibujadas en el lienzo de prototipos.

| Pantalla | Prio. | Propósito | Elementos clave | Ionic |
| --- | --- | --- | --- | --- |
| Ya has votado | C | Evitar el doble voto y tranquilizar | Fecha y hora del voto, recibo parcial, «Verificar mi recibo», «Cambiar mi voto» solo si la regla lo permite | `ErrorState` variante informativa |
| Votación cerrada | C | Explicar que ya no se puede votar | Estado (cerrada o en escrutinio), fecha de cierre, enlace a verificar recibo o a resultados | `ErrorState` + `ion-list` de datos |
| Verificación de identidad | C | Confirmar que quien abre el enlace es la persona | Enlace personal válido, código de 6 dígitos al correo institucional, intentos restantes, reenvío, SSO opcional; si el centro usa Voter ID, se pide antes y solo sirve para decidir a qué correo enviar el código | `ion-input-otp`, `ion-button` |
| Ayuda y preguntas frecuentes | D | Resolver dudas sin salir del flujo | Acordeón con 6–8 preguntas («¿Puedo cambiar mi voto?», «¿Es secreto?», «¿No me llega el código?», «¿Qué es el recibo?»), contacto de la secretaría, acceso desde el icono de ayuda de la cabecera | `ion-accordion-group`, `ion-modal` en móvil |
| Términos, privacidad y accesibilidad | D | Información legal que se pide en el acceso | Qué datos se tratan (RGPD), que el voto no se vincula a la identidad, plazo de conservación, contacto del responsable; casilla de aceptación solo la primera vez | Página de texto con `ion-content`; `ion-checkbox` |
| Resultados públicos | D | Mostrar el resultado al cerrar, si la organización lo decide | Resultado por opción, participación, huella de verificación; nunca antes del cierre | `ion-card`, `ion-progress-bar` |

La ayuda debe estar a un toque desde cualquier paso del flujo, y los términos enlazados en el pie de la pantalla de acceso, no en una pantalla propia obligatoria.

## 7. Guía de estilo mínima

Todo se define una vez en `src/theme/variables.scss` y se reutiliza con componentes compartidos; ninguna pantalla pone colores o tamaños a mano.

### Color

| Uso | Valor | Variable Ionic | Contraste |
| --- | --- | --- | --- |
| Cabecera y menú | `#0F2A47` | `tertiary` | 14,6:1 con blanco |
| Estructura y selección | `#14365C` | `primary` | 12,3:1 |
| Acción principal | `#1F7A5A` | `secondary` | 5,3:1 |
| Error y borrado | `#8F1F14` | `danger` | 8,8:1 |
| Aviso | `#7A4B00` sobre `#FFF4DC` | `warning` | 6,8:1 |
| Éxito | `#14573F` sobre `#E6F4EE` | `success` | 7,5:1 |
| Texto y fondo | `#1B2733` sobre `#F3F5F8` | `--ion-text-color` | 13,9:1 |
| Texto secundario | `#4A5868` sobre `#F3F5F8` | `medium` | 6,7:1 |

Regla: verde solo para la acción principal de cada pantalla, rojo solo para errores y acciones destructivas, azul marino para estructura.

### Tipografía, espaciado y forma

- **Fuente:** Inter (400, 500, 600, 700) con `system-ui` de reserva; códigos de recibo en monoespaciada.
- **Escala:** `h1` 28 px escritorio y 24–26 px móvil; subtítulo 18 px; cuerpo 16 px; ayudas 14 px (mínimo); botones 17 px en 600.
- **Espaciado:** múltiplos de 4 px (4, 8, 12, 16, 20, 24, 32). Tarjeta con 20 px de relleno en móvil y 24–32 px en escritorio; 12 px entre botones apilados y 18 px entre bloques.
- **Forma:** radio de 10 px en campos y botones, 12–14 px en tarjetas; borde de 1 px `#C9D1DB`; foco con contorno de 3 px `#14365C`.
- **Objetivos táctiles:** 44 px como mínimo, 52 px en las acciones del votante.

### Componentes reutilizables

| Componente Angular | Contenido | Usado en |
| --- | --- | --- |
| `AppHeader` | Marca, organización, ayuda, idioma | Todo el flujo |
| `StatusBar` | «Proceso abierto», cierre, participación opcional | Votante |
| `StepIndicator` | «Paso N de 4» y barras; `aria-current="step"` | Votante |
| `ErrorState` | Icono, título, texto, consejos, 2 botones | Errores y estados |
| `LoadingState` | Spinner, barra y texto con `aria-live` | Procesando, carga |
| `StatusBadge` | Etiqueta con texto y color | Estado de proceso, eventos |
| `ReceiptCard` | Código, copiar, aviso de sustituido | Recibo, verificación |
| `VoteOption` | `ion-radio` en tarjeta de 64 px | Papeleta |

### Tarjeta de «Próximos eventos» de tu captura

Se integra bien en este sistema con cuatro retoques: el enlace «Ver calendario» está en un verde azulado que no está en la paleta, así que pásalo a `primary` (azul marino) con subrayado; las fichas «Apertura» (verde) y «Cierre» (ámbar) ya siguen la regla de color con texto, así que se conservan como `StatusBadge`; el título «Próximos eventos» debe ir en 600 como los demás `h2`; y el mes «JUN» y la hora, en `#4A5868` y 14 px. Cada evento sería un `ion-item` con `button`, de 64 px de alto como mínimo.
