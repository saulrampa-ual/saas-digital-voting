# Hoja de registro de usabilidad

Una fila por sesión. Usar solo identificadores aleatorios. No anotar nombres, correos, códigos, tokens, recibos completos
ni la opción seleccionada. Tiempos según el [guion](guion-tareas.md): T0 = bienvenida, T1 = «Voto registrado».

| ID | Válida | Navegador y ancho | Duración T0→T1 (s) | Sin ayuda | Éxito (sin ayuda y ≤ 180 s) | Nº de errores | Ayuda ofrecida | Comprobó el recibo | Comprensión correcta | Incidencia de entorno | Observación anónima |
| --- | --- | --- | ---: | --- | --- | ---: | --- | --- | --- | --- | --- |
| P01 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| P02 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| P03 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| P04 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| P05 | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE |
| R1 (solo si se usa) | — | — | — | — | — | — | — | — | — | — | — |
| R2 (solo si se usa) | — | — | — | — | — | — | — | — | — | — | — |

## Resumen agregado

Informar siempre como cantidades sobre las sesiones válidas, sin porcentajes.

- Sesiones válidas: PENDIENTE de 5 (reservas usadas: PENDIENTE; sesiones invalidadas por el entorno: PENDIENTE).
- Éxito sin ayuda en ≤ 180 s: PENDIENTE de 5. Criterio: 5 de 5.
- Comprensión correcta: PENDIENTE de 5. Criterio: al menos 4 de 5.
- Duración T0→T1 (descriptiva): mediana PENDIENTE s; mínimo PENDIENTE s; máximo PENDIENTE s.
- Decisión H1b: PENDIENTE.
  - Se acepta H1b solo si se cumplen los dos criterios con 5 sesiones válidas.
  - H0b si no se alcanza alguno de ellos.
  - PENDIENTE si hay menos de 5 sesiones válidas.
- Con n = 5 no hay inferencia estadística: el resultado es evidencia descriptiva limitada al prototipo, la configuración
  y la población evaluados.

## Registro en CSV y cálculo del resumen (05/11/2026)

Para el día de las sesiones, la misma hoja en formato tabla: `hoja-registro-h1b.csv` (una fila por sesión, solo
identificadores P01–P05, R1 y R2; valores `si`/`no` y la duración T0→T1 en segundos). Al terminar:

```powershell
node scripts/evaluation/h1b-summary.mjs
```

Comprueba que no falten datos en las filas válidas y escribe el resumen agregado de arriba con la decisión (aceptada,
rechazada o pendiente si hay menos de 5 sesiones válidas). No anotar nombres, correos, códigos ni recibos.
