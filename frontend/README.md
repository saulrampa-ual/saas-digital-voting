# VeriVoto · frontend

Angular 22 + Ionic 9 (PWA), i18n es/en con ngx-translate, login de gestión con Keycloak (`verivoto-admin`, PKCE).

## Arrancar en local

Requisitos: Node.js `^22.22.3 || ^24.15.0 || ^26.0.0` y npm 10.9.9. Antes del frontend, prepara
`infra/.env`, arranca Docker Compose y levanta los cinco servicios backend, incluido el gateway. Sigue
las instrucciones de la [raíz](../README.md) y de [backend](../backend/README.md).

```powershell
Set-Location frontend
npm ci
npm start   # http://localhost:4200 (proxy /api -> http://localhost:8000)
```

- Zona de gestión: `http://localhost:4200/admin` (redirige al login de Keycloak).
- Zona de votante: `http://localhost:4200/votar/<enlace-personal>` (sin Keycloak; requiere un enlace de invitación válido).
- Los códigos de acceso enviados por correo se consultan en Mailpit: `http://localhost:8025`.

Para iniciar sesión hacen falta cuentas del realm local. El primer acceso de `superadmin` se configura
con `VERIVOTO_SUPERADMIN_EMAIL` y `VERIVOTO_SUPERADMIN_INITIAL_PASSWORD` de `infra/.env`; Keycloak
solicita cambiar esa contraseña. Para crear cuentas de organización y revisar permisos, sigue el
[Manual de roles y simulación](../docs/evaluation/resultados-p7/manual-simulacion-roles.md).

El frontend usa el proxy de desarrollo de Angular para enviar `/api` a `http://localhost:8000`; por
tanto, el API gateway debe estar activo. Los detalles para levantar infraestructura y backend están en
[backend/README.md](../backend/README.md) y [Infraestructura local](../docs/architecture/local-infrastructure.md).

## Build

```powershell
npm run build   # salida en dist/verivoto-web
```

Las traducciones están en `public/i18n/{es,en}.json`.
