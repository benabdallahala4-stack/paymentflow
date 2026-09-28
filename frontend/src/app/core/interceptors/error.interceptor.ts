import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/** Set on a request's HttpContext to opt out of the global error toast (e.g. a form
 * that renders its own inline error state for expected business-rule failures). */
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_TOAST)) {
        if (error.status === 401) {
          auth.logout();
          notifications.show('Your session has expired. Please log in again.', 'error');
          router.navigate(['/login']);
        } else {
          notifications.show(messageFor(error), 'error');
        }
      }
      return throwError(() => error);
    }),
  );
};

function messageFor(error: HttpErrorResponse): string {
  const backendMessage = (error.error as { message?: string } | null)?.message;
  if (backendMessage) return backendMessage;
  switch (error.status) {
    case 403:
      return 'You do not have permission to do that.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'That request conflicted with another update. Please retry.';
    case 422:
      return 'The request could not be processed (business rule rejection).';
    case 503:
      return 'The service is temporarily unavailable. Please try again shortly.';
    case 0:
      return 'Could not reach the server. Is the backend running?';
    default:
      return `Something went wrong (HTTP ${error.status}).`;
  }
}
