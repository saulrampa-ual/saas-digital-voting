# VeriVoto · guía rápida para probar el prototipo

Trabajo Fin de Máster de Saul David Ramos Pacheco (Máster en Ingeniería Informática, UAL). Prototipo de
plataforma SaaS de votación interna. **Solo se ejecuta en local y con datos ficticios**: no hay despliegue público.
Los comandos son para Windows (PowerShell) con Docker Desktop; en Linux o macOS son equivalentes (`./mvnw`, `export`).

Si solo quieres ver las pantallas sin instalar nada: `docs/prototipos/index.html` (prototipos visuales estáticos).

## 1. Código

La rama con el trabajo más reciente es `dev` (`main` solo recibe `dev` al cerrar cada hito).

```powershell
git clone https://github.com/saulrampa-ual/saas-digital-voting.git
cd saas-digital-voting
git switch dev
```

## 2. Requisitos

| Herramienta | Versión |
| --- | --- |
| Docker Desktop | reciente |
| Java (Temurin) | 25 |
| Node.js | 24 LTS |

Conviene tener unos 8 GB de RAM libres: dos PostgreSQL, Keycloak, Mailpit, LibreTranslate y cinco servicios Java.

## 3. Infraestructura

```powershell
Copy-Item infra/.env.example infra/.env
# Rellena infra/.env con contraseñas locales cualesquiera (no se usan fuera de tu equipo)
docker compose -f infra/compose.yaml --env-file infra/.env up -d --build
docker compose -f infra/compose.yaml ps
```

Espera a que `pg-main`, `pg-ballot` y `keycloak` estén en `healthy`. La primera vez LibreTranslate descarga sus
modelos de traducción y tarda unos minutos. Detalle: `docs/architecture/local-infrastructure.md`.

| Servicio | Dirección |
| --- | --- |
| Keycloak (consola de administración) | http://localhost:8080 |
| Mailpit (correo de pruebas: códigos y enlaces de voto) | http://localhost:8025 |

## 4. Backend (cinco servicios)

En una consola de PowerShell, desde la raíz del repositorio:

```powershell
. .\scripts\Import-DotEnv.ps1          # carga infra/.env en esta consola
$env:IDENTITY_DB_USERNAME = 'identity'; $env:ELECTION_DB_USERNAME = 'election'
$env:VOTER_ACCESS_DB_USERNAME = 'voter_access'; $env:BALLOT_BOX_DB_USERNAME = 'ballot_box'
backend\mvnw.cmd -f backend\pom.xml -DskipTests package
```

Después, arranca cada servicio (en consolas distintas, todas con las variables anteriores cargadas, o con
`Start-Process`):

```powershell
java -jar (Get-Item backend\services\identity-service\target\*.jar -Exclude *plain*)       # puerto 8081
java -jar (Get-Item backend\services\election-service\target\*.jar -Exclude *plain*)       # puerto 8082
java -jar (Get-Item backend\services\voter-access-service\target\*.jar -Exclude *plain*)   # puerto 8083
java -jar (Get-Item backend\services\ballot-box-service\target\*.jar -Exclude *plain*)     # puerto 8084
java -jar (Get-Item backend\services\api-gateway\target\*.jar -Exclude *plain*)            # puerto 8000
```

Cada uno está listo cuando `http://localhost:<puerto>/actuator/health` responde `UP`.

## 5. Frontend

```powershell
cd frontend
npm ci
npm start                               # http://localhost:4200
```

## 6. Datos de demostración

Con todo lo anterior en marcha, desde la raíz del repositorio (usa Playwright del frontend; la primera vez puede
hacer falta `npx playwright install chromium` dentro de `frontend`):

```powershell
$env:DEMO_PASSWORD = '<12 caracteres o más>'
node scripts/demo/seed-ual.mjs
```

Crea la organización de demostración, sus usuarios de gestión y tres procesos (uno ya escrutado, uno abierto y uno
programado). Los usuarios y el resto de datos de acceso quedan en `.local/demo-ual.md` (fichero local, no se sube al
repositorio); la contraseña de las cuentas de gestión es la de `DEMO_PASSWORD`.

## 7. Qué probar

1. **Consola de gestión:** http://localhost:4200/admin. Tras la contraseña, Keycloak pide un código que llega a
   Mailpit (http://localhost:8025).
2. **Crear y programar un proceso:** menú «Procesos» → «Crear proceso» (cuatro pasos) → en la ficha del proceso,
   «Programar».
3. **Votar:** cuando el proceso se abra, el enlace personal de cada votante llega a Mailpit. Papeleta, confirmación y
   recibo.
4. **Resultados:** tras el cierre, el escrutinio es automático. En la consola: resultados, acta y registro de la
   urna; como votante, comprobación del recibo.

## 8. Pruebas automáticas

```powershell
backend\mvnw.cmd -f backend\pom.xml verify     # unitarias, arquitectura e integración (Testcontainers: Docker en marcha)
cd frontend; npm run test:ci; npm run build
```

## Documentación

- Memoria del TFM: `Plantilla-TFM-Informatica-UAL/` (fuentes LaTeX; `main.pdf` compilado).
- Decisiones y estado del trabajo: `docs/ai/DECISIONS.md`, `docs/ai/PROGRESS.md`, `docs/ai/PLAN.md`.
- Arquitectura y decisiones de diseño: `docs/architecture/`, `docs/adr/`.
- Evaluación: `docs/evaluation/`.
