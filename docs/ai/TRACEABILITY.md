# TRACEABILITY · VeriVoto

Matriz RF → servicio → endpoint → pantalla → estado. Endpoints de la rebanada 1 definidos en P2 (`backend/services/*/src/main/resources/openapi/`); el resto se completa en su hito.
Pantallas: códigos de `docs/ai/PROTOTYPES.md`.

| RF | Nombre | MVP | Servicio | Endpoint | Pantalla | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| RF-01 | Gestionar organizaciones | Sí | identity-service | `POST/GET /api/v1/organizations`, `GET/PATCH /api/v1/organizations/{id}`  | E1, E2 | Hecho (falta compilar y probar) |
| RF-02 | Gestionar roles y permisos | Sí | identity-service + Keycloak | `GET/POST /api/v1/organizations/{id}/users`, `PUT …/users/{userId}/role` | E3 | Hecho (falta compilar y probar) |
| RF-03 | Gestionar censo | Sí | election-service | `/api/v1/censuses`, `…/{censusId}/voters`, `…/{censusId}/imports` | A2, G1 | Hecho salvo congelación por proceso (P4.1); falta compilar y probar |
| RF-04 | Autenticar usuario de gestión | Sí | Keycloak (configuración) | Flujo `verivoto browser` (contraseña + Email OTP) | D1; login en `frontend/src/app/core/auth` | Hecho (configuración + UI; pendiente de prueba manual) |
| RF-05a | Iniciar sesión de gestión (SSO) | Sí | Keycloak (identity provider) | `identity-provider-redirector` en el flujo; IdP por organización | D1 | Parcial: falta un IdP externo de prueba |
| RF-05b | Iniciar sesión de votante (SSO) | Sí (si da tiempo, P6) | voter-access-service | TBD | B1 (alternativa SSO) | Pendiente |
| RF-06 | Crear y configurar proceso | Sí | election-service | `POST/GET /api/v1/processes`, `GET/PUT /api/v1/processes/{id}` | A3, F3, Gestión de candidatos | Pendiente |
| RF-07 | Programar y gestionar ciclo de vida | Sí | election-service | `POST /api/v1/processes/{id}/schedule`, `…/archive`, `GET …/status-history`; internos `PUT /internal/v1/processes/{id}/status` | A4 | Pendiente |
| RF-08 | Acceder a la papeleta | Sí | voter-access-service | `GET /api/v1/voter-access/links/{linkToken}`, `POST …/otp`, `POST …/otp/verify` | V1, B1, V2, C2, C3, C4, V5, V6 | Pendiente |
| RF-09 | Emitir voto | Sí | ballot-box-service | `GET /api/v1/ballot-box/processes/{id}/ballot`, `POST …/votes`; interno `POST /internal/v1/voting-tokens/consume` y `…/revert` | B2, B3, C1, V3, V7 | Pendiente |
| RF-10 | Verificar recibo | Sí | ballot-box-service | `GET /api/v1/ballot-box/processes/{id}/receipts/{receiptCode}` | B4, B5 | Pendiente |
| RF-11 | Sustituir voto | **No** (D-03) | — | — | V1, V3, B4 (elementos de revoto) | Fuera del MVP |
| RF-12 | Consultar registro de urna | Sí | ballot-box-service | TBD | H1 | Pendiente |
| RF-13 | Escrutar y verificar resultado | Sí | ballot-box-service | TBD | F4, V8 | Pendiente |
| RF-14 | Consultar participación | Sí | voter-access-service | TBD | A1, A5 | Pendiente |
| RF-15 | Consultar y exportar resultados | Sí | ballot-box-service | TBD | F1, V8 | Pendiente |
| RF-16 | Generar acta | Sí | ballot-box-service | TBD | F1 | Pendiente |
| RF-17 | Gestionar notificaciones | Sí | voter-access-service | TBD | **Sin prototipo** | Pendiente |
| RF-18 | Atender derechos sobre datos personales | Sí | identity-service | TBD | F2 | Pendiente |
| RF-19 | Consultar trazabilidad administrativa | Sí | identity-service | TBD | H1 | Pendiente |
| RF-20 | Gestionar idioma de papeleta | Sí | election-service | Textos `LocalizedText` (es, en) en `ProcessData` y `Ballot` | Selector ES/EN de cabecera (`frontend/src/app/shared/language-switcher`, `public/i18n`); textos de papeleta en A3 por confirmar | En curso (base i18n de la interfaz hecha) |
| RF-21 | Exponer API documentada | Sí | api-gateway + identity-service | TBD | **Sin prototipo** (claves de API) | Pendiente |
| RF-22 | Aplicar métodos avanzados | No | — | — | — | Fuera del MVP |
| RF-23 | Cifrar papeleta en cliente | No | — | — | — | Fuera del MVP |
