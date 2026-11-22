import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { adminProviders } from '../../../../testing/admin-test-doubles';
import { ApiError } from '../../../core/admin/api-error';
import { NotificationSummary, NotificationsApiService } from '../../../core/admin/notifications-api.service';
import { NotificationsPanelComponent } from './notifications-panel.component';

type Panel = {
  data(): NotificationSummary | null;
  error(): boolean;
  busy(): boolean;
  confirming: { set(v: boolean): void; (): boolean };
  when: { set(v: string): void; (): string };
  formError(): string | null;
  message(): string | null;
  retryMessage(): string | null;
  retryError(): string | null;
  open(): boolean;
  used(): number;
  full(): boolean;
  totalFailed(): number;
  minLocal(): string;
  maxLocal(): string;
  sendNow(): Promise<void>;
  schedule(): Promise<void>;
  cancel(reminder: { id: number }): Promise<void>;
  retry(): Promise<void>;
};

const HOUR = 3_600_000;
const counts = (failed = 0) => ({ pending: 1, delivered: 2, failed, cancelled: 0 });
const SUMMARY: NotificationSummary = {
  processId: 4, status: 'OPEN', opensAt: new Date(Date.now() - HOUR).toISOString(), closesAt: new Date(Date.now() + 48 * HOUR).toISOString(),
  counts: { INVITATION: counts(2), REMINDER: counts(), CLOSING: counts(1) },
  failed: [], reminders: [{ id: 1, sendAt: new Date().toISOString(), status: 'SENT', recipients: 3, createdAt: new Date().toISOString() },
    { id: 2, sendAt: new Date().toISOString(), status: 'CANCELLED', recipients: null, createdAt: new Date().toISOString() }],
  maxReminders: 2,
};
const localInput = (date: Date) => {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

/** A5 (RF-17): invitations, reminders and failed messages of a process; counts only, never who voted. */
describe('NotificationsPanelComponent (avisos)', () => {
  let api: Record<'summary' | 'scheduleReminder' | 'cancelReminder' | 'retryFailed', ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    api = {
      summary: vi.fn().mockResolvedValue(SUMMARY),
      scheduleReminder: vi.fn().mockResolvedValue({ id: 3 }),
      cancelReminder: vi.fn().mockResolvedValue({ id: 1 }),
      retryFailed: vi.fn().mockResolvedValue({ retried: 3 }),
    };
    TestBed.configureTestingModule({ providers: [...adminProviders(), { provide: NotificationsApiService, useValue: api }] });
  });

  async function open(): Promise<Panel> {
    const fixture = TestBed.createComponent(NotificationsPanelComponent);
    fixture.componentRef.setInput('processId', 4);
    TestBed.tick();
    await new Promise((resolve) => setTimeout(resolve, 0));
    return fixture.componentInstance as unknown as Panel;
  }

  it('loads the summary and its counts when the process is set', async () => {
    const panel = await open();
    expect(api.summary).toHaveBeenCalledWith(4);
    expect(panel.open()).toBe(true);
    expect(panel.used()).toBe(1);
    expect(panel.full()).toBe(false);
    expect(panel.totalFailed()).toBe(3);
    expect(panel.minLocal()).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    expect(panel.maxLocal()).toMatch(/^\d{4}-/);
  });

  it('marks the panel as failed when the summary cannot be read', async () => {
    api.summary.mockRejectedValue(new Error('down'));
    const panel = await open();
    expect(panel.error()).toBe(true);
    expect(panel.totalFailed()).toBe(0);
  });

  it('sends a reminder now and closes the confirmation', async () => {
    const panel = await open();
    panel.confirming.set(true);
    await panel.sendNow();
    expect(api.scheduleReminder).toHaveBeenCalledWith(4);
    expect(panel.message()).toBe('admin.notifications.sentNow');
    expect(panel.confirming()).toBe(false);
    expect(panel.busy()).toBe(false);
  });

  it('schedules a reminder inside the voting period and rejects one outside it', async () => {
    const panel = await open();
    await panel.schedule();
    expect(api.scheduleReminder).not.toHaveBeenCalled();

    panel.when.set(localInput(new Date(Date.now() + 100 * HOUR)));
    await panel.schedule();
    expect(panel.formError()).toBe('admin.notifications.outOfPeriod');

    panel.when.set(localInput(new Date(Date.now() + 5 * HOUR)));
    await panel.schedule();
    expect(api.scheduleReminder).toHaveBeenCalledWith(4, expect.any(String));
    expect(panel.message()).toBe('admin.notifications.scheduled');
    expect(panel.when()).toBe('');
  });

  it('shows the detail of the service when an action is refused, or a generic error', async () => {
    const panel = await open();
    api.cancelReminder.mockRejectedValueOnce(new ApiError(409, 'Ya enviado', [])).mockRejectedValueOnce(new Error('down'));
    await panel.cancel({ id: 1 });
    expect(panel.formError()).toBe('Ya enviado');
    await panel.cancel({ id: 1 });
    expect(panel.formError()).toBe('common.loadError');
    expect(panel.message()).toBeNull();
  });

  it('retries the failed messages and reports how many, or why not', async () => {
    const panel = await open();
    await panel.retry();
    expect(panel.retryMessage()).toBe('admin.notifications.retried');
    api.retryFailed.mockRejectedValueOnce(new ApiError(409, 'Nada que reintentar', [])).mockRejectedValueOnce(new Error('down'));
    await panel.retry();
    expect(panel.retryError()).toBe('Nada que reintentar');
    expect(panel.retryMessage()).toBeNull();
    await panel.retry();
    expect(panel.retryError()).toBe('common.loadError');
  });
});
