# Elections · Revisión UI/UX y flujo definitivo

Oct 24, 2026 · @Saul

## Resumen

El archivo adjunto ya resuelve buena parte de lo que pides, pero no coincide con la descripción del encargo: son los prototipos de **VeriVoto** (28 pantallas, tres anchos cada una), no un flujo con modal de Voter ID. Esta revisión se basa en lo que contiene el archivo; lo que falta de verdad son 8 pantallas del votante, la gestión de candidatos y un prototipo enlazado.

| Tu descripción | Lo que hay en el archivo | Consecuencia |
| --- | --- | --- |
| Modal de Voter ID | Enlace personal por email + código de 6 dígitos (B1), con SSO como alternativa | No hay Voter ID: la identificación ya está resuelta de otra forma |
| Categorías y lista de candidatos | Una papeleta de opción única con voto en blanco (B2) | Si «Elections» necesita varias categorías, hay que añadir la papeleta por pasos |
| Contador de votos en la cabecera | No existe; la cabecera lleva marca, organización e idioma ES/EN. La participación solo está en el panel de administración (A5) | Correcto: no mostrar recuentos al votante |
| Sin rol de administrador | Superadministrador, Administrador, Gestor de censo y Auditor (A1–A5, D1, E1–E3, F1–F4, G1, H1) | Ya prototipado; falta solo la gestión de candidatos |
| Mockups estáticos | Escritorio 1280 px, tablet 768 px y móvil 390 px, sin enlaces entre pantallas | Falta el prototipo clicable |

Por tanto, la lista de mejoras de abajo distingue lo que ya está bien (y debe conservarse) de lo que falta. Si tu equipo trabaja con otra versión de los prototipos, dímelo y rehago la revisión sobre ella.

## Revisión del flujo del votante

El orden actual es lógico (Acceso → Papeleta → Resumen → Procesando → Recibo → Verificación, con un indicador de 4 pasos), pero le faltan la entrada y los caminos de excepción.

- **¿Falta alguna pantalla?** Sí. No hay bienvenida con instrucciones: B1 salta directamente al código. Tampoco existen «Ya has votado», «Votación cerrada», «No estás habilitado/a», «Sesión caducada» ni «Tu voto no se ha registrado». Los errores actuales cubren solo enlace caducado, proceso no abierto, acceso bloqueado, 404 y 500.
- **¿Voter ID antes o después de un login?** Ninguno de los dos. El votante no necesita cuenta: el enlace personal único identifica y el código de 6 dígitos por email confirma que quien abre el enlace es la persona. Un login con contraseña para votar añade una contraseña que olvidar y vincula la sesión a una cuenta, justo lo contrario de un voto secreto. Email y contraseña quedan solo para gestión (D1). Si en un centro no todos tienen email institucional, el código estudiantil puede servir como paso previo para enviar ese código al correo registrado, nunca como único factor, porque se comparte con facilidad.
- **¿El 2FA explica qué pasa?** A medias. Bien: «Te lo hemos enviado por email a a•••@ejemplo.org. Te quedan 5 intentos». Falta decir cuánto dura el código, un reenvío con cuenta atrás, el estado de código incorrecto y el de «enviando». Además, el campo se llama «Código de verificación» y más tarde el recibo tiene un «código de recibo»: conviene llamarlos «código de acceso» y «código de recibo». Texto propuesto: «Hemos enviado un código de 6 dígitos a l•••@universidad.edu. Caduca en 10 minutos.»
- **¿La confirmación evita votos accidentales?** Es la parte más sólida: pantalla propia, selección en grande, aviso «Aún no has votado» y botones con verbo explícito («Confirmar y votar», «Cambiar mi selección»). Se puede reforzar desactivando el botón al pulsarlo y aclarando el efecto del voto en blanco.
- **¿Navegación hacia atrás?** Antes de confirmar, sí: «Cambiar mi selección» ya existe y debe mantenerse. Tras confirmar, no: Atrás no debe devolver a la papeleta ni reenviar el voto, sino llevar a «Ya has votado». Falta decidir una regla única sobre cambiar el voto: B4 dice «si el proceso lo permite», y eso es ambiguo para quien vota.

### Feedback visual por paso

| Paso | Carga | Error | Éxito | Valoración |
| --- | --- | --- | --- | --- |
| Acceso (B1) | Falta «Verificando…» | Solo el bloqueo final (C4); falta código incorrecto | Banner verde «Enlace personal válido» | Incompleto |
| Papeleta (B2) | No aplica | «Todavía no has elegido una opción», con borde rojo y `role=alert` (C1) | Opción marcada con borde y fondo | Bien |
| Resumen (B3) | Falta estado del botón al pulsar | Falta fallo de red | Cuadro con la selección | Incompleto |
| Procesando | Spinner, barra y aviso de no cerrar la ventana | Solo el 500 genérico | Pasa a B4 | Falta error propio |
| Recibo (B4) | No aplica | No aplica | Icono, código y «copiar» | Falta «Copiado» |
| Verificación (B5) | Botón Comprobar | Por revisar: código no encontrado | «Voto registrado y verificado» | Bien |

## Mejoras priorizadas

Siete mejoras son críticas porque dejan huecos de seguridad o de confianza en el voto; el resto son deseables y pueden ir después del primer prototipo clicable.

| # | Prioridad | Mejora | Pantalla |
| --- | --- | --- | --- |
| 1 | Crítica | Crear «Ya has votado» y usarla también al recargar o pulsar Atrás tras confirmar | B2, B4 |
| 2 | Crítica | Mostrar desde la bienvenida la regla de cambio de voto: se puede cambiar hasta el cierre o la publicación de la huella final, y después no | B3, B4 |
| 3 | Crítica | Estado de código incorrecto con intentos restantes, caducidad del código y reenvío con cuenta atrás | B1 |
| 4 | Crítica | Pantalla «Tu voto no se ha registrado» con reintento seguro (sin riesgo de duplicar) en lugar del 500 genérico | Procesando, C5 |
| 5 | Crítica | Variantes «Votación cerrada» (incluido escrutinio en curso) y «No estás habilitado/a»; hoy C3 solo cubre «no abierta» | C3 |
| 6 | Crítica | Bienvenida con instrucciones antes de pedir el código | B1 |
| 7 | Crítica | Sesión caducada por inactividad, con aviso previo | B2, B3 |
| 8 | Deseable | Gestión de candidatos como pantalla propia (hoy son un paso del asistente) | A3 |
| 9 | Deseable | Vista previa de la papeleta para el administrador antes de programar el proceso | A3, A4 |
| 10 | Deseable | Prototipo clicable de punta a punta con las 28 pantallas enlazadas | Todas |
| 11 | Deseable | «Copiado» tras copiar el recibo, y descargar o imprimir el recibo | B4 |
| 12 | Deseable | Subir los textos de 13 px a 14 px o más (ayudas, etiquetas en mayúsculas, «Todavía no se registra nada») | B2, B3, B4 |
| 13 | Deseable | Enlace «¿Necesitas ayuda?» en la cabecera del votante | Cabecera |
| 14 | Deseable | Resultados públicos al cierre, si la organización los activa | Nueva |
| 15 | Deseable | Aviso de «sin conexión» para la PWA | Todas |

### Ya está bien: conservar

- La participación solo aparece como dato agregado en el panel de administración, con el aviso «El recuento por opción no se muestra hasta el cierre» (A5, F1).
- «Cambiar mi selección» antes de confirmar y pantalla de resumen separada de la papeleta.
- Estados de carga con `role=status`, error con `role=alert`, objetivos táctiles de 44 px o más (salvo el selector ES/EN, de 40 px: subirlo a 44), `lang="es"` y campo de código con `autocomplete="one-time-code"`.
- Mensaje del pie «Conexión segura · Tu voto es secreto» en todas las pantallas del votante.

## Pantallas a prototipar

Hay que dibujar 8 pantallas o estados del votante y 1 del administrador (gestión de candidatos); el resto de lo que pides (login de gestión, panel, procesos, resultados) ya existe.

### Votante

| Pantalla | Propósito | Elementos clave |
| --- | --- | --- |
| V1 · Bienvenida | Que la persona sepa qué va a hacer antes de pedirle el código | Título del proceso, cierre («cierra el 19 nov a las 21:00»), 3 pasos en una frase cada uno, duración aproximada, «Tu voto es secreto», regla de cambio de voto, botón «Empezar», enlace de ayuda |
| V2 · Código incorrecto (estado de B1) | Corregir sin perder el contexto | Mensaje junto al campo («El código no es correcto. Te quedan 3 intentos»), campo vacío y enfocado, «Reenviar en 0:45», caducidad del código |
| V3 · Ya has votado | Evitar el doble voto y dar tranquilidad | Mensaje («Ya has votado el 12 nov a las 10:14»), código de recibo parcial, «Verificar mi recibo», «Cambiar mi voto» solo si la regla lo permite, salir |
| V4 · Votación cerrada | Explicar que ya no se puede votar | Estado («Cerrada» o «Escrutinio en curso»), fecha de cierre, enlace a verificar recibo, enlace a resultados si son públicos, contacto |
| V5 · No habilitado/a | Dar salida a quien no figura en el censo | Mensaje neutro que no revela el motivo exacto, contacto de la organización, plazo de reclamación si existe |
| V6 · Sesión caducada | Tras inactividad, sin perder el trabajo ya hecho | Aviso previo a los 2 min, «Tu selección no se ha guardado», botón para volver a entrar con el mismo enlace |
| V7 · Voto no registrado | Fallo de red o servidor al votar | Mensaje claro («Tu voto NO se ha registrado»), «Reintentar» idempotente, garantía de no duplicar, estado de conexión |
| V8 · Resultados públicos (opcional) | Mostrar el resultado cuando la organización lo decida | Resultado por opción, participación, huella de verificación, fecha de publicación; nunca antes del cierre |

### Administrador

| Pantalla | Estado | Qué hacer |
| --- | --- | --- |
| Login de gestión y 2FA | Existe (D1) | Mantener; añadir estado de código incorrecto como en V2 |
| Panel / dashboard | Existe (A1, E1) | Mantener |
| Gestión de elecciones | Existe (A3 crear, A4 ciclo de vida) | Añadir vista previa de la papeleta |
| Gestión de candidatos | **Falta**: solo hay «Opciones de la papeleta» dentro del asistente (A3) | Pantalla propia: lista, alta y edición con nombre, lista o candidatura, foto y descripción, orden (fijo o aleatorio), bloqueo de edición al programar o abrir |
| Resultados | Existe (F1, F4, H1) | Mantener; verificar que el recuento por opción solo sea visible tras el cierre |

## Accesibilidad y consistencia visual

La base ya cumple AA en contraste y en tamaño táctil; lo que queda son textos pequeños, una cabecera que conviene fijar como componente único y una paleta que hay que trasladar a variables de Ionic.

### Accesibilidad y usabilidad

- **Contraste:** todos los pares del prototipo pasan AA (4,5:1) con margen; los avisos también (ver tabla). Los colores nunca llevan solo el significado: cada aviso tiene icono y texto.
- **Tamaños:** cuerpo de 16 px en móvil y escritorio; ayudas a 14 px como mínimo. Hoy hay textos de 13 px (etiquetas en mayúsculas, «Todavía no se registra nada») que deben subir.
- **Botones táctiles:** al menos 44 px de alto (WCAG 2.2 exige 24 px; Apple y Material, 44 y 48 px). En la papeleta y en el resumen, 48 px o más y 12 px de separación entre «Cambiar» y «Confirmar» para evitar toques accidentales.
- **Código de 6 dígitos:** un solo campo con `letter-spacing` funciona en el prototipo; en Ionic usar `ion-input-otp` (Ionic 8.6 o superior) o, si no, `inputmode="numeric"` con `autocomplete="one-time-code"`.
- **Foco y teclado:** mantener el anillo de foco de Ionic; orden de tabulación igual al visual; al cambiar de pantalla, mover el foco al `h1`.
- **Textos en español institucional:** tratar de tú de forma constante (el prototipo ya lo hace), frases cortas, verbos concretos, y evitar anglicismos («Voter ID» → «código estudiantil», «Secured Voting» → «Votación segura»).

### Cabecera y consistencia

- Cabecera única del votante: marca, nombre de la organización y selector ES/EN (como ahora), más «Ayuda». Sin contador de votos ni recuentos.
- Barra de estado bajo la cabecera («Proceso abierto · Cierra el 19 nov a las 21:00») solo en el flujo de voto, como ahora; en las pantallas de error se oculta.
- «Conexión segura» va en el pie, no en la cabecera: la cabecera de gestión incluye organización, rol y usuario, y no debe parecerse a la del votante.
- El indicador de pasos (4 pasos: Acceso, Selección, Confirmación, Recibo) debe aparecer en todas las pantallas del flujo, incluido V1 como paso 0 sin numerar.

### Paleta y tipografía base para Angular + Ionic

Tipografía: **Inter** (la que ya usa el prototipo), con `system-ui` de reserva. Escala: `h1` 28 px escritorio y 24 px móvil, subtítulo 18 px, cuerpo 16 px, ayuda 14 px, código de recibo en monoespaciada.

Los tres colores de `variables.scss` de la guía de adaptación coinciden con el prototipo; faltan el azul oscuro de la cabecera y los avisos. Proponemos añadirlos como `tertiary` (cabecera) y `warning`.

| Uso | Color | Variable Ionic | Contraste |
| --- | --- | --- | --- |
| Cabecera y menú | `#0F2A47` | `tertiary` | 14,6:1 con blanco |
| Acción secundaria y selección | `#14365C` | `primary` | 12,3:1 con blanco |
| Acción principal (Votar, Crear) | `#1F7A5A` | `secondary` | 5,3:1 con blanco |
| Errores y borrado | `#8F1F14` sobre `#FCE9E7` | `danger` | 8,8:1 con blanco; 7,6:1 sobre su fondo |
| Aviso | `#7A4B00` sobre `#FFF4DC` | `warning` | 6,8:1 |
| Éxito | `#14573F` sobre `#E6F4EE` | `success` | 7,5:1 |
| Texto secundario | `#4A5868` sobre `#F3F5F8` | `medium` | 6,7:1 |
| Texto y fondo | `#1B2733` sobre `#F3F5F8` | `--ion-text-color`, `--ion-background-color` | 13,9:1 |

Una regla de uso evita el color decorativo: verde solo para la acción principal de cada pantalla, rojo solo para errores y acciones destructivas, azul marino para estructura y selección.

## Flujo definitivo del votante

El votante entra con su enlace personal, confirma con un código por email, vota una sola vez y sale con un recibo verificable; sin cuenta ni contraseña.

1. **Enlace personal por email.** Llega con el aviso de apertura y el cierre. Abre `/votar/:token`; si el enlace caducó, va a «Enlace caducado».
2. **Bienvenida (nueva, V1).** Qué se vota, hasta cuándo, 3 pasos, voto secreto y regla de cambio de voto. Botón «Empezar».
3. **Acceso (B1, paso 1).** Se envía el código de 6 dígitos al correo enmascarado. Estados: enviando, código incorrecto con intentos restantes (V2), reenvío con cuenta atrás, bloqueo a los 5 intentos (C4). SSO institucional como alternativa si la organización lo activa.
4. **Papeleta (B2, paso 2).** Opciones en tarjetas grandes con voto en blanco. Si se intenta continuar sin elegir, error en la propia pantalla (C1).
5. **Resumen (B3, paso 3).** «Revisa tu voto» con la selección en grande. Dos salidas: «Cambiar mi selección» (vuelve al paso 4) o «Confirmar y votar».
6. **Procesando.** Spinner y barra, botón desactivado, aviso de no cerrar. Si falla, «Tu voto no se ha registrado» con reintento seguro (V7).
7. **Voto registrado y recibo (B4, paso 4).** Código de recibo con «Copiar» que responde «Copiado». Desde aquí, Atrás ya no vuelve a la papeleta. Mientras no haya cierre ni huella final, «Cambiar mi voto» crea un voto nuevo que sustituye al anterior y marca el recibo como sustituido; después desaparece el botón.
8. **Verificación del recibo (B5, opcional).** Comprobar el código en la lista pública de recibos.
9. **Salida.** Cerrar sesión y volver al inicio. Si vuelve a abrir el enlace: «Ya has votado» (V3).

### Caminos de excepción

| Situación | Pantalla | Salida para la persona |
| --- | --- | --- |
| Enlace caducado | C2 | Solicitar un enlace nuevo |
| Proceso aún no abierto | C3 | Añadir al calendario |
| Proceso cerrado o en escrutinio | V4 (nueva) | Verificar recibo o ver resultados públicos |
| No consta en el censo | V5 (nueva) | Contactar con la organización |
| 5 códigos incorrectos | C4 | Esperar al fin de la cuenta atrás |
| Ya votó | V3 (nueva) | Verificar recibo; cambiar voto solo si se permite |
| Inactividad | V6 (nueva) | Volver a entrar con el mismo enlace |
| Fallo al registrar | V7 (nueva) | Reintentar |
| Página inexistente o error de servidor | C5 | Volver al inicio |

### Siguientes pasos

- Decidir la regla de cambio de voto : decidida. El voto se puede cambiar mientras el proceso no haya cerrado ni se haya publicado la huella final; después, no. Falta reflejarla en V1, V3 y B4.
- Dibujar V1 a V8 y la pantalla de candidatos con los tres anchos del prototipo.
- Enlazar las pantallas en un prototipo clicable y probarlo con 5 estudiantes antes de pasar a Ionic.
