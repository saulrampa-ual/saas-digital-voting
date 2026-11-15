/**
 * Runtime settings of the web app. In development the API is reached through the Angular dev-server proxy
 * (proxy.conf.json), so API calls use relative URLs and need no CORS.
 *
 * Keycloak: on localhost the browser talks to http://localhost:8080 directly (as before). Served under any other
 * host (the HTTPS tunnel of the online RNF-4 session), Keycloak is reached on the same origin through the
 * dev-server proxy, which forwards only /realms/verivoto and /resources (never Keycloak's /admin or /realms/master).
 */
const LOCAL_HOSTS = ['localhost', '127.0.0.1'];
const pageHost = globalThis.location?.hostname ?? 'localhost';

export const APP_CONFIG = {
  apiBaseUrl: '',
  keycloak: {
    url: LOCAL_HOSTS.includes(pageHost) ? 'http://localhost:8080' : globalThis.location.origin,
    realm: 'verivoto',
    clientId: 'verivoto-admin',
    /** RF-05b (D-48): public client of the voter PWA for the institutional account (PKCE). */
    voterClientId: 'verivoto-voter',
  },
  languages: ['es', 'en'] as const,
  defaultLanguage: 'es' as const,
};

export type AppLanguage = (typeof APP_CONFIG.languages)[number];
