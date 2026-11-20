import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { CensusApiService } from '../../../core/admin/census-api.service';
import { CensusesPage } from './censuses.page';

type Page = {
  ionViewWillEnter(): void;
  create(): Promise<void>;
  name: { set(v: string): void; (): string };
  censuses(): unknown[];
  loading(): boolean;
  error(): boolean;
  nameError(): string | null;
  canEdit: boolean;
};

/** A1 (RF-03): list of censuses and creation of a new one by the census manager. */
describe('CensusesPage (censos)', () => {
  let api: { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn> };
  let page: Page;
  let fixture: ComponentFixture<unknown>;
  const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

  beforeEach(() => {
    api = { list: vi.fn().mockResolvedValue([{ id: 1, name: 'Censo', voterCount: 2, frozen: false }]), create: vi.fn().mockResolvedValue({ id: 5 }) };
    TestBed.configureTestingModule({ providers: [...adminProviders(['CENSUS_MANAGER']), { provide: CensusApiService, useValue: api }] });
    fixture = TestBed.createComponent(CensusesPage);
    page = fixture.componentInstance as unknown as Page;
  });
  // Render the last state of each test, so the template is checked with the data too
  afterEach(async () => {
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('lists the censuses each time the page is shown', async () => {
    expect(page.canEdit).toBe(true);
    page.ionViewWillEnter();
    await settle();
    expect(page.censuses()).toHaveLength(1);
    expect(page.loading()).toBe(false);
  });

  it('flags a list that cannot be read', async () => {
    api.list.mockRejectedValue(new Error('down'));
    page.ionViewWillEnter();
    await settle();
    expect(page.error()).toBe(true);
  });

  it('asks for a name, creates the census and opens it', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await page.create();
    expect(page.nameError()).toBe('admin.censuses.nameRequired');
    page.name.set(' Nuevo ');
    await page.create();
    expect(api.create).toHaveBeenCalledWith('Nuevo');
    expect(navigate).toHaveBeenCalledWith(['/admin/censos', 5]);
    expect(page.name()).toBe('');
  });

  it('shows why the census could not be created', async () => {
    page.name.set('Repetido');
    api.create.mockRejectedValueOnce(new ApiError(409, 'Ya existe', [])).mockRejectedValueOnce(new Error('down'));
    await page.create();
    expect(page.nameError()).toBe('Ya existe');
    await page.create();
    expect(page.nameError()).toBe('common.loadError');
  });
});
