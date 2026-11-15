# Hoja de registro de usabilidad

Una fila por sesión. Usar solo identificadores aleatorios. No anotar nombres, correos, códigos, tokens, recibos completos
ni la opción seleccionada. Tiempos según el [guion](guion-tareas.md): T0 = bienvenida, T1 = «Voto registrado».

| ID | Válida | Navegador y ancho | Duración T0→T1 (s) | Sin ayuda | Éxito (sin ayuda y ≤ 180 s) | Nº de errores | Ayuda ofrecida | Comprobó el recibo | Comprensión correcta | Incidencia de entorno | Observación anónima |
| --- | --- | --- | ---: | --- | --- | ---: | --- | --- | --- | --- | --- |
| P01 | No | — | — | — | — | — | — | — | — | Sí | Su entrada del censo ya tenía un voto del 06/11 al empezar; sesión no realizada; sustituida por R1 |
| P02 | Sí | Navegador, 440×956 | 69 | Sí | Sí | 0 | No | Sí | Sí | No | Sin observaciones |
| P03 | Sí | Navegador, 360×800 | 36 | Sí | Sí | 0 | No | Sí | Sí | No | Sin observaciones |
| P04 | Sí | Navegador, 1440 px | 43 | Sí | Sí | 0 | No | Sí | Sí | No | Rápido y fácil de usar |
| P05 | Sí | 960×1440 | 51 | Sí | Sí | 0 | No | Sí | Sí | No | No aparece el escudo de VeriVoto arriba; mucho margen vacío tras «Empezar» |
| R1 | Sí | Navegador, 400×800 | 79 | Sí | Sí | 0 | No | Sí | Sí | No | Reserva usada en sustitución de P01 |
| R2 | No (fuera del cómputo) | 768×1024 | 33 | Sí | Sí | 0 | No | Sí | Sí | No | Reserva no necesaria (solo se invalidó P01); no aparece el logo de VeriVoto arriba |

## Resumen agregado

Informar siempre como cantidades sobre las sesiones válidas, sin porcentajes.

- Sesiones válidas: 5 de 5 (reservas usadas: 1; sesiones invalidadas por el entorno: 1).
- Éxito sin ayuda en ≤ 180 s: 5 de 5. Criterio: 5 de 5.
- Comprensión correcta: 5 de 5. Criterio: al menos 4 de 5.
- Duración T0→T1 (descriptiva): mediana 51 s; mínimo 36 s; máximo 79 s. Errores observados: 0.
- Decisión H1b: **ACEPTADA** (`node scripts/evaluation/h1b-summary.mjs`, 10/11/2026).
  - Se acepta H1b solo si se cumplen los dos criterios con 5 sesiones válidas.
  - H0b si no se alcanza alguno de ellos.
  - PENDIENTE si hay menos de 5 sesiones válidas.
- Con n = 5 no hay inferencia estadística: el resultado es evidencia descriptiva limitada al prototipo, la configuración
  y la población evaluados.

## Notas de las sesiones del 09/11/2026

- Duraciones leídas del cronómetro en formato min:s,cc (por ejemplo, «1,08,97» = 68,97 s) y redondeadas al segundo
  más próximo, porque el script exige segundos enteros.
- P01 es una incidencia de entorno (preparación del censo), no un error de la persona: no llegó a hacer la tarea. Se
  usó la primera reserva, R1, como indica el protocolo.
- R2 también se realizó, pero no era necesaria: queda fuera del cómputo (`valida = no`, sin incidencia de entorno). Su
  resultado (sin ayuda, 33 s, comprensión correcta) no cambia la decisión.
- La columna «Comprobó el recibo» solo indica sí o no: los códigos de recibo anotados en la hoja original se quitaron
  antes de guardarla en el repositorio.
- Observaciones de interfaz (P05 y R2): falta el escudo de VeriVoto en la cabecera de la pantalla del votante y queda
  mucho margen vacío tras «Empezar». No impidieron la tarea; registradas como IMP-56.

## Registro en CSV y cálculo del resumen (05/11/2026)

Para el día de las sesiones, la misma hoja en formato tabla: `hoja-registro-h1b.csv` (una fila por sesión, solo
identificadores P01–P05, R1 y R2; valores `si`/`no` y la duración T0→T1 en segundos). Al terminar:

```powershell
node scripts/evaluation/h1b-summary.mjs
```

Comprueba que no falten datos en las filas válidas y escribe el resumen agregado de arriba con la decisión (aceptada,
rechazada o pendiente si hay menos de 5 sesiones válidas). No anotar nombres, correos, códigos ni recibos.
