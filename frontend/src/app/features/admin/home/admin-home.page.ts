import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { APP_CONFIG } from '../../../core/config/app-config';
import { LanguageService } from '../../../core/i18n/language.service';
import { CensusSummary, ProcessApiService, ProcessSummary } from '../../../core/admin/process-api.service';
import { ChainCheck, Participation, ResultsApiService } from '../../../core/admin/results-api.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';
import {
  CheckState, STRIP_STATUSES, VERIFIABLE_STATUSES, censusOverview, checkState, countByStatus, openNow,
  recentlyClosed, upcoming,
} from './home-dashboard';

interface Organization {
  id: string;
  name: string;
  status: 'ACTIVE' | 'SUSPENDED';
}

interface OpenCard {
  process: ProcessSummary;
  participation: Participation | null;
}

interface AuditRow {
  process: ProcessSummary;
  check: ChainCheck | null;
  state: CheckState | 'ERROR';
}

/** Open processes whose participation is loaded on the home page (the rest are one click away). */
const OPEN_CARDS = 3;

/**
 * Console home (IMP-44): one block per role of the person, built only from the existing read APIs. Participation is
 * a count of people, never of options, and the per-day breakdown stays on its own page (D-18).
 */
@Component({
  selector: 'app-admin-home',
  imports: [AdminPageComponent, NgTemplateOutlet, RouterLink, IonIcon, IonSpinner, IonText, TranslatePipe, LocalDatePipe, LocalizedPipe,
    StatusBadgeComponent],
  templateUrl: './admin-home.page.html',
  styleUrl: './admin-home.page.scss',
})
export class AdminHomePage {
  protected readonly auth = inject(AuthService);
  private readonly processApi = inject(ProcessApiService);
  private readonly resultsApi = inject(ResultsApiService);
  private readonly http = inject(HttpClient);
  protected readonly language = inject(LanguageService).current;

  protected readonly isAdmin = computed(() => this.auth.hasAnyRole(['ADMIN']));
  protected readonly isCensusManager = computed(() => this.auth.hasAnyRole(['CENSUS_MANAGER']));
  protected readonly isAuditor = computed(() => this.auth.hasAnyRole(['AUDITOR']));
  protected readonly isSuperadmin = computed(() => this.auth.hasAnyRole(['SUPERADMIN']));

  protected readonly stripStatuses = STRIP_STATUSES;
  protected readonly processes = signal<ProcessSummary[] | null>(null);
  protected readonly processesError = signal(false);
  protected readonly openCards = signal<OpenCard[]>([]);
  protected readonly censuses = signal<CensusSummary[] | null>(null);
  protected readonly censusesError = signal(false);
  protected readonly auditRows = signal<AuditRow[] | null>(null);
  protected readonly organizations = signal<Organization[] | null>(null);
  protected readonly organizationsTotal = signal(0);
  protected readonly organizationsError = signal(false);

  protected readonly counts = computed(() => countByStatus(this.processes() ?? []));
  protected readonly openTotal = computed(() => openNow(this.processes() ?? []).length);
  protected readonly upcoming = computed(() => upcoming(this.processes() ?? []));
  protected readonly tallied = computed(() => recentlyClosed(this.processes() ?? [], ['TALLIED']));
  protected readonly census = computed(() => censusOverview(this.censuses() ?? []));
  protected readonly activeOrganizations = computed(() => (this.organizations() ?? []).filter((o) => o.status === 'ACTIVE').length);
  protected readonly suspendedOrganizations = computed(() => (this.organizations() ?? []).filter((o) => o.status === 'SUSPENDED').length);

  ionViewWillEnter(): void {
    if (this.isAdmin() || this.isAuditor()) void this.loadProcesses();
    if (this.isCensusManager()) void this.loadCensuses();
    if (this.isSuperadmin()) void this.loadOrganizations();
  }

  protected async loadProcesses(): Promise<void> {
    this.processesError.set(false);
    try {
      const list = await this.processApi.list();
      this.processes.set(list);
      if (this.isAdmin()) void this.loadOpenCards(list);
      if (this.isAuditor()) void this.loadAuditRows(list);
    } catch {
      this.processesError.set(true);
    }
  }

  private async loadOpenCards(list: ProcessSummary[]): Promise<void> {
    const open = openNow(list).slice(0, OPEN_CARDS);
    this.openCards.set(open.map((process) => ({ process, participation: null })));
    const loaded = await Promise.all(open.map(async (process) => ({
      process,
      participation: await this.resultsApi.participation(process.id).catch(() => null),
    })));
    this.openCards.set(loaded);
  }

  private async loadAuditRows(list: ProcessSummary[]): Promise<void> {
    const closed = recentlyClosed(list, VERIFIABLE_STATUSES, 5);
    this.auditRows.set(await Promise.all(closed.map(async (process): Promise<AuditRow> => {
      try {
        const check = await this.resultsApi.lastCheck(process.id);
        return { process, check, state: checkState(check) };
      } catch {
        return { process, check: null, state: 'ERROR' };
      }
    })));
  }

  protected async loadCensuses(): Promise<void> {
    this.censusesError.set(false);
    try {
      this.censuses.set(await this.processApi.censuses());
    } catch {
      this.censusesError.set(true);
    }
  }

  protected async loadOrganizations(): Promise<void> {
    this.organizationsError.set(false);
    try {
      const page = await firstValueFrom(this.http.get<{ items: Organization[]; totalItems: number }>(
        `${APP_CONFIG.apiBaseUrl}/api/v1/organizations`));
      this.organizations.set(page.items);
      this.organizationsTotal.set(page.totalItems);
    } catch {
      this.organizationsError.set(true);
    }
  }

  protected pct(value: number): string {
    return new Intl.NumberFormat(this.language(), { maximumFractionDigits: 1 }).format(value);
  }

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }
}
