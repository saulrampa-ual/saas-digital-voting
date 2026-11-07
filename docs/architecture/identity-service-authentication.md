# Identity service authentication contract

The identity service (formerly `tenant-service`) acts as an OAuth 2.0 resource server. It accepts bearer access
tokens from the OIDC issuer configured by `IDENTITY_OIDC_ISSUER_URI` and validates
them through the issuer's discovery metadata and signing keys.

The access token must contain a `roles` claim with an array of role names. The
service maps each role to a Spring Security authority by adding the `ROLE_`
prefix. For example, `SUPERADMIN` becomes `ROLE_SUPERADMIN`.

All HTTP requests require a valid bearer token. Creating a tenant additionally
requires the `SUPERADMIN` role. The identity provider must issue that role only
to authorized platform super-administrators.

This service validates tokens; it does not implement the management login,
password storage, email one-time codes, or token issuance required by RF-04.
Those responsibilities belong to the identity provider. The local development
provider is Keycloak with the Email OTP Authenticator extension; its setup is
documented in `keycloak-local-email-otp.md`. The realm `verivoto` (browser flow with email OTP, SMTP, OIDC clients and the `roles` and
`organization_id` claims) is versioned in `infra/keycloak/realm/verivoto-realm.json`; see
`keycloak-realm.md`.
