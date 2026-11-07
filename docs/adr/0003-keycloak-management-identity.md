# ADR-0003: Keycloak 26.8.1 como proveedor de identidad de gestión

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-02

## Contexto

RF-04 exige usuario, contraseña con hash y sal, segundo factor por correo, bloqueo tras 5 intentos y sesión con caducidad; RF-05a, SSO OIDC.

## Decisión

Keycloak 26.8.1 con la extensión Email OTP v1.8.1 autentica a los usuarios de gestión. Los servicios son *resource servers* JWT. El realm se versiona como fichero e importa al arrancar (P3.2). Los votantes no son usuarios de Keycloak: su acceso lo gestiona voter-access.

## Consecuencias

No se programa autenticación propia. Datos de usuario (RI-2) en Keycloak: desviación documentada en el modelo de datos.

## Alternativas descartadas

Spring Security con autenticación propia: más código y más superficie de ataque, sin aportar al TFM.
