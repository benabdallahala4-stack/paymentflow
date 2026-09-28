import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavBarComponent } from './shared/components/nav-bar/nav-bar.component';
import { NotificationBannerComponent } from './shared/components/notification-banner/notification-banner.component';

@Component({
  imports: [RouterOutlet, NavBarComponent, NotificationBannerComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {}
