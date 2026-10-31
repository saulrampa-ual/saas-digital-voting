# Recuento de pruebas

La ejecución base completa de `mvnw verify` produjo 49 pruebas backend: gateway 1, identity 12, election 10, voter-access 9 y ballot-box 17. Las cuatro clases de integración PostgreSQL (8 pruebas) están incluidas en esas 49.

Se añadieron 11 métodos `@Test`: election 9 (5 parser CSV, 3 importación CSV, 1 método de autorización HTTP con tres aserciones) y ballot-box 2 (integridad). Los XML Surefire de la verificación final confirman 60 pruebas: gateway 1, identity 12, election 19, voter-access 9 y ballot-box 19; 0 fallos, 0 errores y 0 omitidas.

Vitest: 10 pruebas existentes, todas pasan. Playwright: 4 casos de comprobación inicial de recibo, contabilizados por separado y no incluidos en Vitest; los 4 se ejecutan y fallan el umbral de axe.
