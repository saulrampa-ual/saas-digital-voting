# Defectos observados

| ID | Severidad | RF/RNF | Evidencia y reproducción | Propuesta (no aplicada) |
| --- | --- | --- | --- | --- |
| DEF-01 | Seria / crítica según regla axe | RNF-1, H1a | Playwright: abrir `/recibo/7`; axe detecta `label-content-name-mismatch` en ES/EN y `aria-required-children` en ES. Reproducible en 1280×800 y 360×800. Evidencia HTML fuera del repo en `%TEMP%\verivoto-p7-e2e\playwright-report\index.html`. | Revisar semántica accesible de `ion-segment`/`ion-input` y repetir axe. No se cambió producción. |
| DEF-02 | Reclasificado: no es defecto | RNF-11 | Aclaración (31/10/2026): el enlace personal entra por `/votar/:linkToken` por diseño (es el enlace de la invitación, D-33); lo que nunca debe ir en la URL ni en el almacenamiento es el código, el token de voto o de resultados y el recibo. El criterio «sin enlace en la URL» del encargo era erróneo. | Aclarar la redacción de D-33; la alternativa de diseño queda como IMP-43 (P9). La aserción E2E comprueba que código, tokens y recibo no aparezcan en URL ni almacenamiento. |

No se detectaron fallos de backend en la batería base ni en las nuevas pruebas CSV/HTTP/urna ejecutadas. No se inventan otros defectos.
