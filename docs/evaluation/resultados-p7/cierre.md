# Cierre de P7 (15/11/2026)

Los demás ficheros de esta carpeta son la instantánea del 31/10/2026 y se conservan como estaban. Este documento
recoge el estado final de P7; el detalle y las fuentes están en `docs/evaluation/informe.md`.

## Hipótesis y requisitos medidos

| Elemento | Resultado | Fuente |
| --- | --- | --- |
| H1a · accesibilidad | **Sin decisión.** Parte automática favorable: 200 comprobaciones con Playwright y axe sin incumplimientos críticos ni serios (13/11/2026). Parte manual (teclado y lector de pantalla) no realizada | `accesibilidad-axe-*.md`, `accesibilidad-manual.md` |
| H1b · usabilidad del votante | **Aceptada** (09/11/2026): 5 de 5 votan y obtienen el recibo sin ayuda en 3 minutos o menos; 5 de 5 interpretan la verificación. Evidencia descriptiva | `usabilidad/` |
| H1c · rendimiento | **Rechazada** (11/11/2026): p95 de la emisión 4852, 2402 y 4849 ms con 500 usuarios simultáneos; cadena íntegra en las tres ejecuciones | `carga.md` |
| RNF-4 · usabilidad de administración | **Aceptado** (13/11/2026): 5 de 5 administradores sin formación previa crean, configuran y programan un proceso sin ayuda en 4 minutos o menos | `usabilidad/` |

## Pruebas automáticas

- Backend: 151 pruebas, 0 fallos (`mvnw verify`, 15/11/2026): gateway 16, identity 13, election 58, voter-access 38 y
  ballot-box 26. Dieciséis son de integración sobre PostgreSQL 18.6.
- Frontend: 92 pruebas en 12 ficheros, 0 fallos (`npm run test:ci`, 15/11/2026).
- Cobertura de líneas (15/11/2026): backend 52,4 % en conjunto (identity 26,3 %, ballot-box 41,6 %, voter-access
  50,4 %, election 74,1 %, gateway 91,2 %); frontend 33,7 % (14/11). Código nuevo desde el 04/11 según SonarQube: 85,8 % y 87,3 %.
- Calidad y seguridad: los dos *quality gates* de SonarQube en «Passed» con 0 incidencias abiertas (14/11/2026).
  Workflows `build` (incluido el job de accesibilidad E2E) y `security` en verde en `main` tras el PR #4 (15/11/2026).

## Lo que queda sin evidencia (limitaciones declaradas)

- Parte manual de H1a.
- RF-05a: inicio de sesión de gestión con un IdP externo (A9).
- Autorización HTTP de identity y repositorios sin prueba de integración propia.
- E2E funcional con Keycloak y Mailpit reales: los barridos usan servicios simulados.
- Escenarios k6 de acceso y escrutinio: solo se midió la emisión.

La evidencia por requisito está en la columna «Evidencia P7» de `docs/ai/TRACEABILITY.md`.
