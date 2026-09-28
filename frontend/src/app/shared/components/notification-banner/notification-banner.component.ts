import { Component, inject } from '@angular/core';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-banner',
  standalone: true,
  templateUrl: './notification-banner.component.html',
  styleUrl: './notification-banner.component.css',
})
export class NotificationBannerComponent {
  private readonly notifications = inject(NotificationService);
  readonly items = this.notifications.notifications;

  dismiss(id: number): void {
    this.notifications.dismiss(id);
  }
}
