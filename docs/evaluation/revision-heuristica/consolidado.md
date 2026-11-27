# Revisión heurística (M3) · consolidación de las hojas E1 y E2

Fecha: 27/11/2026. Versión evaluada: commit `41140b0` (las dos personas, cada una con su demo «· E1» y «· E2»).
Fuentes: `hoja-E1-autor.xlsx`, `hoja-E2.xlsx` y las capturas de E2 (`E2-capturas.docx`). Criterios: `protocolo.md`.

## Quién y cómo

| Evaluador | Perfil | Pruebas | Fecha |
| --- | --- | --- | --- |
| E1 | Autor del sistema | Votante V1–V15 (360 px; V1, V4 y V6 también a 1280 px) y consola C1–C11 (1280 px) | 27/11/2026 |
| E2 | Compañero del máster, sin participación en el desarrollo | Las mismas pruebas, con el manual generado por `scripts/demo/manual-m3.mjs` | 27/11/2026 |

Cada persona revisó por separado. E2 entregó además un documento con capturas; sus cuatro observaciones que no
estaban en la hoja se pasaron a `hoja-E2.xlsx` con sus palabras, y la gravedad (1) y la heurística (8) las confirmó
E2 a través del autor. En C8, el primer intento de E1 falló por el entorno local (faltaba el rol `INTEGRATION` en el
realm, IMP-78); tras crearlo, C8 se repitió con éxito. No es un problema de la versión evaluada.

## Problemas, una vez unidas las hojas

Ningún problema aparece en las dos hojas, así que no hay filas fusionadas: 11 problemas distintos.

| # | Evaluador | Pantalla | Ancho | Heurística | Problema (resumen) | Gravedad | RNF | Mejora |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | E1 | Votante · Bienvenida | 360 | 8 | Espacio vacío bajo «Empezar»: la página se puede desplazar sin contenido | 1 | Otro | IMP-56 |
| 2 | E2 | Votante · Revisión | 360 | 8 | El texto del aviso amarillo se ve demasiado grande | 1 | Otro | IMP-79 |
| 3 | E2 | Votante · Comprobación del recibo | 360 | 8 | La casilla aparece con el código ya escrito; espera que esté vacía para pegarlo | 1 | Otro | IMP-80 |
| 4 | E2 | Consola · Inicio de sesión | 1280 | 8 | El logo del panel azul debería ir centrado | 1 | Otro | IMP-81 |
| 5 | E2 | Consola · Participación | 1280 | 8 | Las tarjetas de cifras son grandes para su texto | 1 | Otro | IMP-82 |
| 6 | E2 | Consola · Participación | 1280 | 8 | Redacción del aviso («Aquí solo ves…»): propone forma impersonal | 1 | Otro | IMP-82 |
| 7 | E2 | Consola · Resultados y acta | 1280 | 8 | Mucho espacio en las tarjetas de cifras | 1 | Otro | IMP-82 |
| 8 | E2 | Consola · Resultados y acta | 1280 | 8 | Hueco gris bajo «Resultado verificado» | 1 | Otro | IMP-82 |
| 9 | E2 | Consola · Inicio (gestor de censo) | 1280 | 8 | Mucho espacio en blanco bajo «Necesita tu atención» | 1 | Otro | IMP-82 |
| 10 | E2 | Consola · Asistente de proceso | 1280 | 7 | Sobran el selector de idioma de escritura y el desplegable de traducción automática | 1 | RNF-9 (E2) | IMP-83 |
| 11 | E1 | Consola · Censos | 360 | 8 | Los correos quedan cortados en la vista móvil | 2 | Otro | IMP-74 |

Resumen: 11 problemas (10 de gravedad 1 y 1 de gravedad 2); **ninguno de gravedad 3 o 4**; ninguna prueba
`no revisado`. 9 de los 11 son de diseño estético (heurística 8).

La fila 10 la marcó E2 como RNF-9, pero es una pantalla de la consola, no del votante: no afecta a la orientación
del votante, que es lo que mide RNF-9.

## Decisión (protocolo.md)

| RNF | Criterio | Resultado | Estado |
| --- | --- | --- | --- |
| RNF-8 | Toda acción da realimentación en ≤ 1 s y todo error dice qué pasó y qué hacer | Cronometraje del 25/11 (`rnf8-cronometraje.csv`): 16 mediciones, primera señal ≤ 35 ms. Ninguna de las dos personas anotó un mensaje de error poco claro ni una acción lenta sin señal | **Aceptado** |
| RNF-9 | En todas las pantallas del votante se ven el paso y el estado del proceso | Ninguna de las dos personas anotó una pantalla del votante sin paso o sin estado | **Aceptado** |
| RNF-10 | Tras juntar las hojas, ningún problema de gravedad 3 o 4 | Gravedad máxima 2 | **Aceptado** |

## Limitaciones

- E1 es el autor del sistema y conoce la interfaz; por eso la decisión se toma con las dos hojas juntas.
- Dos evaluadores es el mínimo que pide la especificación; Nielsen recomienda de tres a cinco para encontrar la
  mayoría de los problemas.
- Entorno local con datos sintéticos; solo Chrome de escritorio con emulación a 360 px.

## Correcciones (27/11/2026, después de la revisión)

Aprobadas por el autor y aplicadas sin commit: IMP-56, IMP-74, IMP-75, IMP-78, IMP-79, IMP-80, IMP-81 e IMP-82
(detalle en `docs/ai/IMPROVEMENTS.md`). IMP-83 (quitar el selector de idioma del asistente) se descarta: contradice D-62.
Las correcciones no cambian la decisión, que se tomó sobre `41140b0`.
