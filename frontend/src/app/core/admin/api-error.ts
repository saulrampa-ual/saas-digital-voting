import { HttpErrorResponse } from '@angular/common/http';
import { Observable, firstValueFrom } from 'rxjs';

export interface FieldError {
  field: string;
  message: string;
}

/** Problem details of the API: status, Spanish `detail` and field errors (RNF-8). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string | null,
    readonly errors: FieldError[],
  ) {
    super(detail ?? `API error ${status}`);
  }
}

/** Turns an HTTP error into an {@link ApiError} with the problem details of the service; other errors pass through. */
export function toApiError(error: unknown): unknown {
  if (error instanceof HttpErrorResponse) {
    const body = (typeof error.error === 'object' && error.error) || {};
    return new ApiError(error.status, (body['detail'] as string | undefined) ?? null,
      (body['errors'] as FieldError[] | undefined) ?? []);
  }
  return error;
}

/** Runs a request and rejects with an {@link ApiError} when the service answers with an error. */
export async function callApi<T>(request: Observable<T>): Promise<T> {
  try {
    return await firstValueFrom(request);
  } catch (error) {
    throw toApiError(error);
  }
}
