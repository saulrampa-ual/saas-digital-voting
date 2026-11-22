import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders, stubDownloads } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { ApiKey, ApiKeysApiService, CreatedApiKey } from '../../../core/admin/api-keys-api.service';
import { ApiKeysPage } from './api-keys.page';

type Page = {
  ionViewWillEnter(): void;
  name: { set(v: string): void; (): string };
  list(): { items: ApiKey[] } | null;
  loadError(): boolean;
  created(): CreatedApiKey | null;
  copied(): string | null;
  confirming: { set(v: number | null): void; (): number | null };
  formError(): string | null;
  listError(): string | null;
  message(): string | null;
  activeCount(): number;
  full(): boolean;
  create(): Promise<void>;
  revoke(key: ApiKey): Promise<void>;
  copy(value: string, what: 'id' | 'secret'): Promise<void>;
  example(c: CreatedApiKey): string;
};

const key = (id: number, active = true): ApiKey => ({
  id, name: `Clave ${id}`, clientId: `client-${id}`, createdByName: 'Ana', createdAt: '2026-11-01T10:00:00Z',
  revokedAt: active ? null : '2026-11-02T10:00:00Z', active,
});
const CREATED: CreatedApiKey = { key: key(9), clientSecret: 'secreto-sintetico', tokenUrl: 'http://localhost:8080/token' };

/** G1 (RF-21): API keys of the organization; the secret is shown once and never stored by the console. */
describe('ApiKeysPage (claves de API)', () => {
  let api: Record<'list' | 'create' | 'revoke', ReturnType<typeof vi.fn>>;
  let page: Page;
  let fixture: ComponentFixture<unknown>;
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  beforeEach(() => {
    stubDownloads();
    api = {
      list: vi.fn().mockResolvedValue({ items: [key(1), key(2, false)], maxActive: 1 }),
      create: vi.fn().mockResolvedValue(CREATED),
      revoke: vi.fn().mockResolvedValue(key(1, false)),
    };
    TestBed.configureTestingModule({ providers: [...adminProviders(), { provide: ApiKeysApiService, useValue: api }] });
    fixture = TestBed.createComponent(ApiKeysPage);
    page = fixture.componentInstance as unknown as Page;
  });
  // Render the last state of each test, so the template is checked with the data too
  afterEach(async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('lists the keys and knows when the limit of active keys is reached', async () => {
    expect(page.full()).toBe(false);
    page.ionViewWillEnter();
    await settle();
    expect(page.list()?.items).toHaveLength(2);
    expect(page.activeCount()).toBe(1);
    expect(page.full()).toBe(true);
  });

  it('flags a list that cannot be read', async () => {
    api.list.mockRejectedValue(new Error('down'));
    page.ionViewWillEnter();
    await settle();
    expect(page.loadError()).toBe(true);
  });

  it('creates a key with a name, shows its secret once and reloads the list', async () => {
    await page.create();
    expect(api.create).not.toHaveBeenCalled();
    page.name.set(' Integración ');
    await page.create();
    expect(api.create).toHaveBeenCalledWith('Integración');
    expect(page.created()).toEqual(CREATED);
    expect(page.name()).toBe('');
    expect(api.list).toHaveBeenCalled();
    expect(page.example(CREATED)).toContain('client_id=client-9');
  });

  it('shows why a key cannot be created', async () => {
    page.name.set('Otra');
    api.create.mockRejectedValueOnce(new ApiError(409, 'Límite de claves activas', []));
    await page.create();
    expect(page.formError()).toBe('Límite de claves activas');
    api.create.mockRejectedValueOnce(new Error('down'));
    await page.create();
    expect(page.formError()).toBe('common.loadError');
  });

  it('revokes a key and reports it, or shows why not', async () => {
    page.confirming.set(1);
    await page.revoke(key(1));
    expect(api.revoke).toHaveBeenCalledWith(1);
    expect(page.confirming()).toBeNull();
    expect(page.message()).toBe('admin.apiKeys.revoked');
    api.revoke.mockRejectedValueOnce(new ApiError(401, 'x', []));
    await page.revoke(key(1));
    expect(page.listError()).toBe('common.loadError');
  });

  it('copies the identifier or the secret, and forgets it when the clipboard is not allowed', async () => {
    await page.copy('client-9', 'id');
    expect(page.copied()).toBe('id');
    stubDownloads('denied');
    await page.copy('secreto', 'secret');
    expect(page.copied()).toBeNull();
  });
});
