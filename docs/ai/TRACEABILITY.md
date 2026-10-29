# TRACEABILITY · VeriVoto

Matriz RF → servicio → endpoint → pantalla → estado. Los endpoints se completan en P2 (contratos OpenAPI).
Pantallas: códigos de `docs/ai/PROTOTYPES.md`.

| RF | Nombre | MVP | Servicio | Endpoint | Pantalla | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| RF-01 | Gestionar organizaciones | Sí | identity-service | `POST /api/v1/tenants` (existente; enrutado por el gateway; se revisa en P2) | E1, E2 | Parcial: solo alta |
| RF-02 | Gestionar roles y permisos | Sí | identity-service + Keycloak | TBD | E3 | Pendiente |
| RF-03 | Gestionar censo | Sí | election-service | TBD | A2, G1 | Pendiente |
| RF-04 | Autenticar usuario de gestión | Sí | Keycloak (configuración) | Flujo OIDC de Keycloak | D1 | Parcial: entorno local listo, realm sin configurar |
| RF-05a | Iniciar sesión de gestión (SSO) | Sí | Keycloak (identity provider) | Flujo OIDC de Keycloak | D1 | Pendiente |
| RF-05b | Iniciar sesión de votante (SSO) | Sí (si da tiempo, P6) | voter-access-service | TBD | B1 (alternativa SSO) | Pendiente |
| RF-06 | Crear y configurar proceso | Sí | election-service | TBD | A3, F3, Gestión de candidatos | Pendiente |
| RF-07 | Programar y gestionar ciclo de vida | Sí | election-service | TBD | A4 | Pendiente |
| RF-08 | Acceder a la papeleta | Sí | voter-access-service | TBD | V1, B1, V2, C2, C3, C4, V5, V6 | Pendiente |
| RF-09 | Emitir voto | Sí | ballot-box-service | TBD | B2, B3, C1, V3, V7 | Pendiente |
| RF-10 | Verificar recibo | Sí | ballot-box-service | TBD | B4, B5 | Pendiente |
| RF-11 | Sustituir voto | **No** (D-03) | — | — | V1, V3, B4 (elementos de revoto) | Fuera del MVP |
| RF-12 | Consultar registro de urna | Sí | ballot-box-service | TBD | H1 | Pendiente |
| RF-13 | Escrutar y verificar resultado | Sí | ballot-box-service | TBD | F4, V8 | Pendiente |
| RF-14 | Consultar participación | Sí | voter-access-service | TBD | A1, A5 | Pendiente |
| RF-15 | Consultar y exportar resultados | Sí | ballot-box-service | TBD | F1, V8 | Pendiente |
| RF-16 | Generar acta | Sí | ballot-box-service | TBD | F1 | Pendiente |
| RF-17 | Gestionar notificaciones | Sí | voter-access-service | TBD | **Sin prototipo** | Pendiente |
| RF-18 | Atender derechos sobre datos personales | Sí | identity-service | TBD | F2 | Pendiente |
| RF-19 | Consultar trazabilidad administrativa | Sí | identity-service | TBD | H1 | Pendiente |
| RF-20 | Gestionar idioma de papeleta | Sí | election-service | TBD | Selector ES/EN de cabecera; textos de papeleta en A3 por confirmar | Pendiente |
| RF-21 | Exponer API documentada | Sí | api-gateway + identity-service | TBD | **Sin prototipo** (claves de API) | Pendiente |
| RF-22 | Aplicar métodos avanzados | No | — | — | — | Fuera del MVP |
| RF-23 | Cifrar papeleta en cliente | No | — | — | — | Fuera del MVP |
