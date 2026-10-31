/**
 * Runtime settings of the web app. In development the API is reached through the Angular dev-server proxy
 * (proxy.conf.json), so API calls use relative URLs and need no CORS.
 */
export const APP_CONFIG = {
  apiBaseUrl: '',
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'verivoto',
    clientId: 'verivoto-admin',
    /** RF-05b (D-48): public client of the voter PWA for the institutional account (PKCE). */
    voterClientId: 'verivoto-voter',
  },
  languages: ['es', 'en'] as const,
  defaultLanguage: 'es' as const,
};

export type AppLanguage = (typeof APP_CONFIG.languages)[number];
