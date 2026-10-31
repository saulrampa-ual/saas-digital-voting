# Informe de ejecución P7

Estado: parcial. Se registran únicamente ejecuciones observadas en esta worktree. Los bloques que dependen del stack real o k6 siguen como `PENDIENTE`.

| Bloque | Evidencia añadida | Ejecución observada | Estado |
| --- | --- | --- | --- |
| 1. Autorización HTTP | MockMvc sobre `/api/v1/censuses`: 401 sin token, ADMIN limitado a su organización, escritura/lectura de censos denegada a INTEGRATION | Prueba integrada con PostgreSQL: 2 métodos de integración pasan; los casos HTTP forman un método | Parcial; faltan matriz completa, todos los endpoints, rutas internas y papeleta |
| 2. Concurrencia/outbox/repositorios | Ninguna nueva prueba de concurrencia ni outbox | PENDIENTE | PENDIENTE |
| 3. Reglas unitarias | 5 pruebas de `VoterCsvReader`; 3 de importación de censo | 8 pruebas nuevas pasan | Parcial; faltan áreas de privacidad, avisos, SSO, tokens de resultado, acta, trazabilidad, gateway y frontend |
| 4. Secreto/integridad | Inspección de columnas de todas las tablas de `ballot_box_db`; casos ChainVerifier de papeleta modificada y desajuste de recibos | Incluido en `mvnw verify` final | Parcial; flujo y logs de servicios reales pendientes |
| 5. E2E/axe | Playwright recorre el estado inicial de comprobación de recibo en 1280×800 y 360×800, ES/EN; axe ejecutado | 4 casos fallan por reglas `aria-required-children` y `label-content-name-mismatch` (críticos/serios); HTML fuera del repo | Parcial; no fue contra backend real ni cubre los flujos completos |
| 6. Carga | Script previo `tests/load/voting.js`; no se añadió preparación automatizada | PENDIENTE; k6 no está instalado y no hay tokens sintéticos de prueba | PENDIENTE |
| 7. Cobertura | CSV JaCoCo por servicio y resumen homogéneo | Ver `cobertura.md` y CSV adjuntos | Hecho para cobertura de líneas/instrucciones; cobertura funcional sigue baja |

Comandos ejecutados:

- `backend/mvnw.cmd -B -ntp -f backend/pom.xml verify` con `JAVA_HOME` apuntando a Temurin 25.0.5: pasada final, BUILD SUCCESS, 60 pruebas backend, 0 fallos/errores/omitidas, 2:42 min.
- `backend/mvnw.cmd -B -ntp -f backend/pom.xml -pl services/election-service -Dtest=VoterCsvReaderTest,CensusImportTest,ElectionPostgresIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`: BUILD SUCCESS, 10 pruebas seleccionadas.
- `frontend/npm run test:ci`: 10 pruebas Vitest pasan.
- `frontend/npm run build`: BUILD SUCCESS; bundle inicial 1,07 MB, transferencia estimada 215,26 kB.
- `frontend/npm run test:e2e -- --workers=1`: 4 casos ejecutados; fallan en el criterio cero de axe.

Vitest y build frontend se ejecutaron tras instalar las dependencias declaradas; 10/10 pruebas pasan y el build compila. La pasada final confirma que los dos nuevos casos de inspección de esquema/cadena de urna también pasan.
