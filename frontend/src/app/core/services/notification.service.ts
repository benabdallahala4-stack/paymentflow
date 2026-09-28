import { Injectable, signal } from '@angular/core';

export type NotificationLevel = 'error' | 'info' | 'success';

export interface Notification {
  id: number;
  message: string;
  level: NotificationLevel;
}

/** Simple in-memory banner/toast queue used by the error interceptor and screens. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 1;
  readonly notifications = signal<Notification[]>([]);

  show(message: string, level: NotificationLevel = 'info', timeoutMs = 6000): void {
    const id = this.nextId++;
    this.notifications.update((list) => [...list, { id, message, level }]);
    if (timeoutMs > 0) {
      setTimeout(() => this.dismiss(id), timeoutMs);
    }
  }

  dismiss(id: number): void {
    this.notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
