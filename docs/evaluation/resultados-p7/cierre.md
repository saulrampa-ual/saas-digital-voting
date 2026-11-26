# Cierre de P7 (15/11/2026)

Los demás ficheros de esta carpeta son la instantánea del 31/10/2026 y se conservan como estaban. Este documento
recoge el estado final de P7; el detalle y las fuentes están en `docs/evaluation/informe.md`.

## Hipótesis y requisitos medidos

| Elemento | Resultado | Fuente |
| --- | --- | --- |
| H1a · accesibilidad | **Aceptada** (22/11/2026). Parte automática: 200 comprobaciones con Playwright y axe sin incumplimientos críticos ni serios (13/11/2026); el 16/11/2026 el flujo del votante suma los 3 estados de IMP-50 (100 de 100 sin críticos ni serios). Parte manual con el Narrador de Windows: 39 de 39 obligatorias, 0 bloqueos, 2 incidencias; contraste con foco de los botones verdes corregido y re-medido | `accesibilidad-axe-*.md`, `accesibilidad-manual.md` |
| H1b · usabilidad del votante | **Aceptada** (09/11/2026): 5 de 5 votan y obtienen el recibo sin ayuda en 3 minutos o menos; 5 de 5 interpretan la verificación. Evidencia descriptiva | `usabilidad/` |
| H1c · rendimiento | **Rechazada** (11/11/2026): p95 de la emisión 4852, 2402 y 4849 ms con 500 usuarios simultáneos; cadena íntegra en las tres ejecuciones | `carga.md` |
| RNF-4 · usabilidad de administración | **Aceptado** (13/11/2026): 5 de 5 administradores sin formación previa crean, configuran y programan un proceso sin ayuda en 4 minutos o menos | `usabilidad/` |

## Pruebas automáticas

- Backend: 175 pruebas, 0 fallos (`mvnw verify` completo del 22/11/2026): gateway 16, identity 30, election 58,
  voter-access 38 y ballot-box 33. Veinticuatro son de integración sobre PostgreSQL 18.6. Al cerrar P7 eran 168.
- Frontend: 262 pruebas en 31 ficheros, 0 fallos (`npm run test:coverage`, 26/11/2026; 261 el 24/11 y 259 el 20/11). Al cerrar P7 eran 144.
- Cobertura de líneas: backend 59,7 % en conjunto el 22/11/2026 (identity 75,2 %, ballot-box 44,9 %, voter-access
  50,4 %, election 73,6 %, gateway 90,3 %); frontend 87,9 % (2566/2919, 26/11/2026; 2560/2913 el 24/11; 87,8 % el 20/11; 42,0 % el 16/11). Código nuevo desde el 04/11 según SonarQube: 85,8 % y 87,3 %.
- Calidad y seguridad: los dos *quality gates* de SonarQube en «Passed» con 0 incidencias abiertas (14/11/2026).
  Workflows `build` (incluido el job de accesibilidad E2E) y `security` en verde en `main` tras el PR #4 (15/11/2026).

## Lo que queda sin evidencia (limitaciones declaradas)

- H1a con otros lectores de pantalla (NVDA, JAWS) y con personas usuarias de lectores de pantalla.
- RF-05a: inicio de sesión de gestión con un IdP externo (A9).
- Repositorios sin prueba de integración propia (la autorización HTTP de identity se cubrió el 17/11/2026).
- E2E funcional con Keycloak y Mailpit reales: los barridos usan servicios simulados.
- Escenarios k6 de acceso y escrutinio: solo se midió la emisión.

La evidencia por requisito está en la columna «Evidencia P7» de `docs/ai/TRACEABILITY.md`.
