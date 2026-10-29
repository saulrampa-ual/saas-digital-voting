# DECISIONS · VeriVoto

Registro de decisiones cerradas. Una decisión solo cambia con una entrada nueva que la sustituya.

| ID | Fecha | Decisión | Motivo | Alternativas descartadas |
| --- | --- | --- | --- | --- |
| D-01 | 2026-10-29 | Java 25 LTS en todos los servicios | LTS vigente; Spring Boot 4.1.2 soporta Java 17 a 27 | Java 17 (el que usaba `tenant-service`) |
| D-02 | 2026-10-29 | Keycloak 26.8.1 como proveedor de identidad de gestión; RF-04 y RF-05a se resuelven por configuración del realm | Ya integrado en `infra/keycloak` con Email OTP; evita programar autenticación propia | Spring Security + OIDC con autenticación propia |
| D-03 | 2026-10-29 | Revoto (RF-11) fuera del MVP; se retoma tras la entrega | Prioridad *Could* en el catálogo RF del TFM; fuera del MVP por alcance | Revoto hasta el cierre o la huella final (prototipos) |
| D-04 | 2026-10-29 | Se conserva `tenant-service` y se adapta a la plantilla hexagonal, renombrado a `identity-service` | Su lógica es correcta; solo cambian paquetes. Menos fricción que adaptar el plan y la plantilla a una convención única | Reescribirlo; adaptar la plantilla a su estructura |
| D-05 | 2026-10-29 | 5 servicios propios + Keycloak: gateway, identity, election (con censo), voter-access (con notificaciones), ballot-box (con escrutinio) | Menos BD, contratos y llamadas para un TFM individual; se conserva la separación participación/urna | 8 servicios (census, tally y notification separados) |
| D-06 | 2026-10-29 | Comunicación REST síncrona; sin Kafka en el MVP | Menos infraestructura; el volumen del MVP no lo exige | Eventos con Kafka |
| D-07 | 2026-10-29 | Numeración RF-01..RF-23 del TFM | Evita confusión con la numeración RF-01..RF-30 del informe | Numeración del informe |
| D-08 | 2026-10-29 | Dos instancias PostgreSQL 18.6: `pg-ballot` solo para la urna; `pg-main` para el resto (una BD lógica por servicio) | Aislamiento físico de la urna con coste operativo bajo | Una instancia por servicio; una sola instancia |
| D-09 | 2026-10-29 | Puertos locales: Keycloak 8080 (sin cambios), gateway 8000, servicios 8081–8084, frontend 4200 | No tocar la configuración de Keycloak ya documentada | Mover Keycloak a otro puerto |
| D-10 | 2026-10-29 | Desarrollo por rebanadas verticales; la rebanada del voto (P4) debe funcionar el 30/11/2026 | Calendario del Gantt: desarrollo hasta el 31/12/2026 | Backend completo y después frontend |
| D-11 | 2026-10-29 | Sin Lombok ni MapStruct; `record` y mappers manuales | El código existente ya sigue ese estilo | Lombok + MapStruct |
| D-12 | 2026-10-29 | Frontend PWA sin Capacitor en el MVP | RNF-18 (PWA instalable) no requiere app nativa | Capacitor |
