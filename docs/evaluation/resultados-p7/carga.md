# Carga P7.5

Estado: `PENDIENTE`. k6 no está instalado en el equipo (`k6 version` no se reconoce) y no se generaron tokens sintéticos ni procesos de carga. No se ejecutó `tests/load/voting.js`; no hay percentiles, errores, duración, recursos Docker ni verificación posterior que informar.

El script existente requiere 500 tokens distintos, proceso y opción válidos; el token de voto vive 15 minutos y es de un uso (D-31). Deben prepararse justo antes de cada una de las tres ejecuciones, fuera del repositorio. No afirmar H1c hasta disponer de las tres mediciones completas con p95 < 2 s.
