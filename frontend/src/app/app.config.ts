import { ApplicationConfig, provideBrowserGlobalErrorListeners, isDevMode } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { RouteReuseStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { provideServiceWorker } from '@angular/service-worker';
import { IonicRouteStrategy, provideIonicAngular } from '@ionic/angular';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import {
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  createInterceptorCondition,
  includeBearerTokenInterceptor,
  provideKeycloak,
  IncludeBearerTokenCondition,
} from 'keycloak-angular';
import { APP_CONFIG } from './core/config/app-config';
import { routes } from './app.routes';

/**
 * The Keycloak token is only sent to management APIs (including results, audit chain and participation), never to the
 * voter or public endpoints (RNF-11).
 */
const managementApiCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern:
    /^(\/|https?:\/\/[^/]+\/)api\/v1\/(organizations|admin-actions|censuses|processes|privacy|ballot-box\/processes\/\d+\/(results|audit-events)|voter-access\/processes)(\/.*)?(\?.*)?$/i,
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideIonicAngular({ mode: 'md' }),
    // Keycloak is not initialized here: AuthService does it lazily for the management console.
    provideKeycloak({ config: APP_CONFIG.keycloak }),
    { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [managementApiCondition] },
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: APP_CONFIG.defaultLanguage,
      lang: APP_CONFIG.defaultLanguage,
    }),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
  ],
};
