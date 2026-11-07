# Autorización HTTP y aislamiento

Prueba automatizada en `ElectionPostgresIntegrationTest`, con MockMvc y PostgreSQL 18.6:

| Endpoint | Rol / token | Esperado | Obtenido |
| --- | --- | ---: | ---: |
| `GET /api/v1/censuses` | Sin token | 401 | 401 |
| `GET /api/v1/censuses` | ADMIN, `organization_id=org-http-a` | 200; solo censo de A | 200; único resultado de A |
| `POST /api/v1/censuses` | INTEGRATION, organización A | 403 | 403 |
| `GET /api/v1/censuses` | INTEGRATION, organización B | 403 | 403 |

El test verifica además por JPA que el censo de B existe, aunque no aparece en la respuesta del ADMIN de A.

Cobertura limitada a un endpoint de election. PENDIENTE: matriz SUPERADMIN/ADMIN/AUDITOR/CENSUS_MANAGER/INTEGRATION por endpoint, identidad, ballot-box, voter-access, gateway, cruces de organización restantes, rutas `/internal/**` por `azp` y casos de `GET …/ballot` de D-43. Hallazgo documental: voter-access no tiene contrato OpenAPI versionado bajo `src/main/resources/openapi/`; su documentación dinámica no se consultó porque el servicio no estaba arrancado.
