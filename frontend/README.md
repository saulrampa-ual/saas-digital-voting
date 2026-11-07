# VeriVoto · frontend

Angular 22 + Ionic 9 (PWA), i18n es/en con ngx-translate, login de gestión con Keycloak (`verivoto-admin`, PKCE).

## Arrancar en local

Requisitos: Node `^22.22.3 || ^24.15.0`, infraestructura (`infra/compose.yaml`), api-gateway (8000) y servicios levantados.

```powershell
cd frontend
npm ci
npm start   # http://localhost:4200 (proxy /api -> http://localhost:8000)
```

- Zona de gestión: `http://localhost:4200/admin` (redirige al login de Keycloak).
- Zona de votante: `http://localhost:4200/votar/<token>` (sin Keycloak; se completa en P4.5).

## Build

```powershell
npm run build   # salida en dist/verivoto-web
```

Las traducciones están en `public/i18n/{es,en}.json`.
