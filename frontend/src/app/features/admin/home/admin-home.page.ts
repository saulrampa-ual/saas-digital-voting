import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { IonIcon, IonSpinner, IonText } from '@ionic/angular';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { APP_CONFIG } from '../../../core/config/app-config';
import { LanguageService } from '../../../core/i18n/language.service';
import { AdminAction, AdminActionsApiService } from '../../../core/admin/admin-actions-api.service';
import { NotificationsApiService } from '../../../core/admin/notifications-api.service';
import { CensusSummary, ProcessApiService, ProcessSummary } from '../../../core/admin/process-api.service';
import { ChainCheck, Participation, ResultsApiService } from '../../../core/admin/results-api.service';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';
import { LocalizedPipe } from '../../../shared/pipes/localized.pipe';
import { StatusBadgeComponent } from '../../../shared/status-badge/status-badge.component';
import { AdminPageComponent } from '../admin-page.component';
import {
  CheckState, VERIFIABLE_STATUSES, censusOverview, checkState, countByStatus, cumulativeByDay, latestCheck, openNow,
  adminCount, participationTotals, recentlyClosed, upcoming,
} from './home-dashboard';

interface Organization {
  id: string;
  name: string;
  status: 'ACTIVE' | 'SUSPENDED';
}

interface OrganizationRow extends Organization {
  accounts: number | null;
  admins: number | null;
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

/** One line of «Needs your attention»: tone, icon and translated text, with an optional link. */
interface AttentionItem {
  tone: 'warn' | 'info' | 'ok';
  icon: string;
  chipKey: string;
  textKey: string;
  subKey?: string;
  params?: Record<string, string | number>;
  link?: string | (string | number)[];
  query?: Record<string, string | number>;
}

/** Days shown in the per-day chart (the last ones). */
const CHART_DAYS = 14;

/**
 * Console home (IMP-44): one panel per role of the person, built only from the existing read APIs. Participation is a
 * count of people, by day at most (D-18); nothing about options is shown before the tally and there is no revote (D-03).
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
  private readonly notificationsApi = inject(NotificationsApiService);
  private readonly actionsApi = inject(AdminActionsApiService);
  private readonly http = inject(HttpClient);
  protected readonly language = inject(LanguageService).current;
  private readonly localized = new LocalizedPipe();

  protected readonly isAdmin = computed(() => this.auth.hasAnyRole(['ADMIN']));
  protected readonly isCensusManager = computed(() => this.auth.hasAnyRole(['CENSUS_MANAGER']));
  protected readonly isAuditor = computed(() => this.auth.hasAnyRole(['AUDITOR']));
  protected readonly isSuperadmin = computed(() => this.auth.hasAnyRole(['SUPERADMIN']));

  /** Name of the person's organization; the identifier until it loads (or if it cannot be read). */
  protected readonly organizationName = signal<string | null>(null);
  protected readonly orgLabel = computed(() => this.organizationName() ?? this.auth.organizationId() ?? '');

  // ADMIN and AUDITOR
  protected readonly processes = signal<ProcessSummary[] | null>(null);
  protected readonly processesError = signal(false);
  protected readonly openCards = signal<OpenCard[] | null>(null);
  protected readonly failedEmails = signal<{ process: ProcessSummary; failed: number }[]>([]);
  protected readonly auditRows = signal<AuditRow[] | null>(null);
  protected readonly actions = signal<AdminAction[] | null>(null);
  // ADMIN and CENSUS_MANAGER
  protected readonly censuses = signal<CensusSummary[] | null>(null);
  protected readonly censusesError = signal(false);
  // SUPERADMIN
  protected readonly organizations = signal<OrganizationRow[] | null>(null);
  protected readonly organizationsTotal = signal(0);
  protected readonly organizationsError = signal(false);

  protected readonly counts = computed(() => countByStatus(this.processes() ?? []));
  protected readonly upcoming = computed(() => upcoming(this.processes() ?? []));
  protected readonly tallied = computed(() => recentlyClosed(this.processes() ?? [], ['TALLIED']));
  protected readonly census = computed(() => censusOverview(this.censuses() ?? []));

  private readonly openParticipation = computed(() =>
    (this.openCards() ?? []).map((c) => c.participation).filter((p): p is Participation => p !== null));
  protected readonly openTotals = computed(() => participationTotals(this.openParticipation()));
  protected readonly days = computed(() => cumulativeByDay(this.openParticipation()).slice(-CHART_DAYS));
  protected readonly chartMax = computed(() => Math.max(1, ...this.days().map((d) => d.pct)));

  protected readonly attention = computed<AttentionItem[]>(() => {
    const items: AttentionItem[] = [];
    for (const { process, failed } of this.failedEmails()) {
      items.push({ tone: 'warn', icon: 'mail-outline', chipKey: 'admin.home.chip.warn', textKey: 'admin.home.attention.failedEmails',
        subKey: 'admin.home.attention.failedEmailsSub', params: { count: failed, process: this.title(process) },
        link: '/admin/participacion', query: { proceso: process.id } });
    }
    for (const c of this.census().empty) {
      items.push({ tone: 'warn', icon: 'people-outline', chipKey: 'admin.home.chip.warn', textKey: 'admin.home.attention.emptyCensus',
        subKey: 'admin.home.attention.emptyCensusSub', params: { name: c.name }, link: ['/admin/censos', c.id] });
    }
    const drafts = this.counts().DRAFT;
    if (drafts > 0) {
      items.push({ tone: 'info', icon: 'create-outline', chipKey: 'admin.home.chip.info', textKey: 'admin.home.attention.drafts',
        subKey: 'admin.home.attention.draftsSub', params: { count: drafts }, link: '/admin/procesos' });
    }
    if (this.census().frozen > 0) {
      items.push({ tone: 'info', icon: 'lock-closed-outline', chipKey: 'admin.home.chip.info', textKey: 'admin.home.attention.frozen',
        subKey: 'admin.home.attention.frozenSub', params: { count: this.census().frozen } });
    }
    if (!items.some((i) => i.tone === 'warn')) {
      items.unshift({ tone: 'ok', icon: 'checkmark-circle-outline', chipKey: 'admin.home.chip.ok', textKey: 'admin.home.attention.allGood',
        subKey: 'admin.home.attention.allGoodSub' });
    }
    return items;
  });
  protected readonly warnings = computed(() => this.attention().filter((i) => i.tone === 'warn').length);

  protected readonly auditCounts = computed(() => {
    const rows = this.auditRows() ?? [];
    return {
      total: rows.length,
      intact: rows.filter((r) => r.state === 'INTACT').length,
      broken: rows.filter((r) => r.state === 'BROKEN').length,
      never: rows.filter((r) => r.state === 'NEVER').length,
    };
  });
  protected readonly lastAudit = computed(() => {
    const rows = this.auditRows() ?? [];
    const check = latestCheck(rows.map((r) => r.check));
    return check ? rows.find((r) => r.check === check) ?? null : null;
  });

  protected readonly activeOrganizations = computed(() => (this.organizations() ?? []).filter((o) => o.status === 'ACTIVE').length);
  protected readonly suspendedOrganizations = computed(() => (this.organizations() ?? []).filter((o) => o.status === 'SUSPENDED').length);
  protected readonly accountsTotal = computed(() => (this.organizations() ?? []).reduce((sum, o) => sum + (o.accounts ?? 0), 0));
  protected readonly singleAdminOrgs = computed(() => (this.organizations() ?? []).filter((o) => o.admins === 1).length);

  ionViewWillEnter(): void {
    if (this.auth.organizationId()) void this.loadOrganizationName();
    if (this.isAdmin() || this.isAuditor()) void this.loadProcesses();
    if (this.isAdmin() || this.isCensusManager()) void this.loadCensuses();
    if (this.isAuditor()) void this.loadActions();
    if (this.isSuperadmin()) void this.loadOrganizations();
  }

  private async loadOrganizationName(): Promise<void> {
    try {
      const org = await firstValueFrom(this.http.get<Organization>(
        `${APP_CONFIG.apiBaseUrl}/api/v1/organizations/${encodeURIComponent(this.auth.organizationId()!)}`));
      this.organizationName.set(org.name);
    } catch {
      this.organizationName.set(null); // the identifier stays visible
    }
  }

  protected async loadProcesses(): Promise<void> {
    this.processesError.set(false);
    try {
      const list = await this.processApi.list();
      this.processes.set(list);
      if (this.isAdmin()) {
        void this.loadOpenCards(list);
        void this.loadFailedEmails(list);
      }
      if (this.isAuditor()) void this.loadAuditRows(list);
    } catch {
      this.processesError.set(true);
    }
  }

  private async loadOpenCards(list: ProcessSummary[]): Promise<void> {
    const open = openNow(list);
    this.openCards.set(await Promise.all(open.map(async (process) => ({
      process,
      participation: await this.resultsApi.participation(process.id).catch(() => null),
    }))));
  }

  /** Invitations and notices that could not be delivered after every retry (RF-17); only counts, never who. */
  private async loadFailedEmails(list: ProcessSummary[]): Promise<void> {
    const live = list.filter((p) => p.status === 'SCHEDULED' || p.status === 'OPEN' || p.status === 'CLOSED');
    const found = await Promise.all(live.map(async (process) => {
      try {
        const summary = await this.notificationsApi.summary(process.id);
        const failed = Object.values(summary.counts).reduce((sum, c) => sum + c.failed, 0);
        return { process, failed };
      } catch {
        return { process, failed: 0 };
      }
    }));
    this.failedEmails.set(found.filter((f) => f.failed > 0));
  }

  private async loadAuditRows(list: ProcessSummary[]): Promise<void> {
    const closed = recentlyClosed(list, VERIFIABLE_STATUSES, 50);
    this.auditRows.set(await Promise.all(closed.map(async (process): Promise<AuditRow> => {
      try {
        const check = await this.resultsApi.lastCheck(process.id);
        return { process, check, state: checkState(check) };
      } catch {
        return { process, check: null, state: 'ERROR' };
      }
    })));
  }

  private async loadActions(): Promise<void> {
    try {
      this.actions.set((await this.actionsApi.search({}, 0, 5)).items);
    } catch {
      this.actions.set([]);
    }
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
        `${APP_CONFIG.apiBaseUrl}/api/v1/organizations`, { params: new HttpParams().set('size', 50) }));
      this.organizationsTotal.set(page.totalItems);
      this.organizations.set(page.items.map((o) => ({ ...o, accounts: null, admins: null })));
      this.organizations.set(await Promise.all(page.items.map(async (o): Promise<OrganizationRow> => {
        try {
          const users = await firstValueFrom(this.http.get<{ items: { role: string }[]; totalItems: number }>(
            `${APP_CONFIG.apiBaseUrl}/api/v1/organizations/${encodeURIComponent(o.id)}/users`, { params: new HttpParams().set('size', 100) }));
          return { ...o, accounts: users.totalItems, admins: adminCount(users.items) };
        } catch {
          return { ...o, accounts: null, admins: null };
        }
      })));
    } catch {
      this.organizationsError.set(true);
    }
  }

  protected title(process: ProcessSummary): string {
    return this.localized.transform(process.title, this.language());
  }

  protected pct(value: number | null): string {
    return value === null ? '—' : new Intl.NumberFormat(this.language(), { maximumFractionDigits: 1 }).format(value);
  }

  protected n(value: number): string {
    return new Intl.NumberFormat(this.language()).format(value);
  }

  protected shortDay(day: string): string {
    return new Intl.DateTimeFormat(this.language(), { day: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(`${day}T12:00:00Z`));
  }

  protected shortHash(hash: string | null): string {
    return hash ? `${hash.slice(0, 4)}…${hash.slice(-4)}` : '—';
  }

  protected participationPct(p: Participation | null): number {
    return p ? Math.max(0, Math.min(100, +p.participation)) : 0;
  }
}
