import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';
import { callApi } from './api-error';

/** RF-21 (identity-service, D-47): a key is a Keycloak client of the organization; no secret here. */
export interface ApiKey {
  id: number;
  name: string;
  clientId: string;
  createdByName: string | null;
  createdAt: string;
  revokedAt: string | null;
  active: boolean;
}

export interface ApiKeyList {
  items: ApiKey[];
  maxActive: number;
}

/** The only moment the secret exists outside Keycloak. */
export interface CreatedApiKey {
  key: ApiKey;
  clientSecret: string;
  tokenUrl: string;
}

/** RF-21: keys of the organization of the ADMIN. The interceptor adds the Keycloak token. */
@Injectable({ providedIn: 'root' })
export class ApiKeysApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${APP_CONFIG.apiBaseUrl}/api/v1/api-keys`;

  list(): Promise<ApiKeyList> {
    return callApi(this.http.get<ApiKeyList>(this.base));
  }

  create(name: string): Promise<CreatedApiKey> {
    return callApi(this.http.post<CreatedApiKey>(this.base, { name }));
  }

  revoke(id: number): Promise<ApiKey> {
    return callApi(this.http.delete<ApiKey>(`${this.base}/${id}`));
  }
}
