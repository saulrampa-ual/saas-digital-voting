# Protocolo de prueba moderada de usabilidad

## Propósito

Observar si personas sin experiencia técnica pueden acceder a una votación de prueba, emitir un voto,
obtener el recibo y explicar qué comprueba. La sesión no evalúa una elección real ni recoge preferencias
políticas.

## Participantes y preparación

- Reclutar cinco personas adultas que no trabajen habitualmente en desarrollo o administración de sistemas, y hasta dos
  de reserva por si una sesión queda invalidada por el entorno. El montaje del escenario está en
  [`preparacion-sesion.md`](preparacion-sesion.md).
- Usar únicamente una organización, censo y proceso sintéticos, con opciones neutras y sin datos personales reales.
- Preparar una sesión individual, un dispositivo de escritorio o móvil y conexión estable. Anotar tamaño de pantalla,
  navegador y dispositivo sin registrar identificadores personales.
- Obtener consentimiento antes de iniciar. No grabar audio, vídeo ni pantalla salvo consentimiento adicional expreso.
- Usar un identificador aleatorio de participante (P01, P02, …); no anotar nombre, correo ni otros datos identificativos.

## Guion uniforme

1. Entregar el consentimiento y responder dudas antes de comenzar.
2. Leer literalmente: «Vas a participar en una votación de prueba. Completa las tareas como lo harías normalmente.
   Puedo observar y tomar notas, pero no te indicaré cómo usar la aplicación salvo que quieras abandonar».
3. Iniciar el cronómetro (T0) cuando se muestre la pantalla de bienvenida del votante. Seguir el guion literal de
   [`guion-tareas.md`](guion-tareas.md).
4. Pedir a la persona que acceda al proceso, complete la autenticación de prueba y emita una opción indicada por el
   moderador. No pedirle que revele una preferencia personal.
5. Anotar T1 cuando aparezca «Voto registrado» con el código de recibo: fin del tiempo de H1b. Pedirle que compruebe el
   recibo y avise cuando termine; anotar T2 al ver el resultado de la comprobación.
6. Preguntar sin sugerir la respuesta: «¿Qué te confirma este recibo y qué no te permite comprobar?».
7. Registrar finalización sin ayuda, duración, errores observados, ayudas necesarias y respuesta a la pregunta.
8. Agradecer la participación, explicar el objetivo de la observación y eliminar cualquier dato temporal usado en la
   sesión que no sea necesario para el análisis agregado.

## Registro de incidencias

Clasificar como error una acción que impida avanzar, requiera volver atrás o provoque una interpretación incorrecta.
Registrar la pantalla y la acción de forma general; no incluir tokens, códigos, recibos completos ni datos de personas.
Si se ofrece ayuda, anotar el momento y el contenido para distinguir éxito autónomo de éxito asistido.

## Criterios de evaluación

- Éxito de tarea: completa acceso, emisión y obtención del recibo sin ayuda en un máximo de 3 minutos (T0→T1). La
  comprobación del recibo se observa después y sirve de base a la pregunta de comprensión.
- Comprensión: explica que el recibo permite comprobar la presencia del registro y su integridad, y que no demuestra
  por sí solo que la opción registrada coincida con la intención de la persona.
- Criterios H1b (validados por el tutor): los 5 participantes alcanzan el éxito de tarea y al menos 4 de 5 superan la
  pregunta de comprensión. H0b: no se alcanza alguno de esos criterios. Informar los resultados como cantidades
  (por ejemplo, 5 de 5); con n = 5 no hay inferencia estadística y los resultados son evidencia descriptiva limitada
  al prototipo, la configuración y la población evaluados.
- El moderador no recalcula ni corrige las respuestas durante la tarea; las aclaraciones se dan al final.
- Sesiones válidas: una incidencia de entorno invalida la sesión y se sustituye por una reserva con otra persona; con
  menos de cinco sesiones válidas, H1b queda PENDIENTE.

## Límites

Una muestra pequeña y un entorno de prueba no representan a toda la población usuaria. La sesión no valida
accesibilidad WCAG ni seguridad del sistema; esas evidencias se recogen por separado.
