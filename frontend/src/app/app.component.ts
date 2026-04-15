import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <header>
        <div class="brand">Payment Routing Engine</div>
        <nav *ngIf="auth.isAuthenticated()">
          <a routerLink="/transactions" routerLinkActive="active">Transactions</a>
          <a routerLink="/recommend" routerLinkActive="active">Recommend</a>
          <a routerLink="/gateways" routerLinkActive="active">Gateways</a>
        </nav>
        <div class="spacer"></div>
        <div class="user" *ngIf="auth.isAuthenticated(); else loginLink">
          <span class="muted">{{ auth.currentUser()?.username }}</span>
          <button class="ghost" (click)="auth.logout()">Logout</button>
        </div>
        <ng-template #loginLink>
          <a routerLink="/login" class="login-link">Login</a>
        </ng-template>
      </header>
      <main>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-shell { min-height: 100vh; display: flex; flex-direction: column; }
    header {
      display: flex; align-items: center; gap: 1.5rem;
      padding: 0.75rem 2rem; background: #fff; border-bottom: 1px solid var(--color-border);
    }
    .brand { font-weight: 600; font-size: 1.05rem; }
    nav { display: flex; gap: 1rem; }
    nav a {
      text-decoration: none; color: var(--color-text-muted); font-size: 0.9rem;
      padding: 0.4rem 0.75rem; border-radius: var(--radius);
    }
    nav a.active, nav a:hover { background: #eef2ff; color: var(--color-primary); }
    .spacer { flex: 1; }
    .user { display: flex; align-items: center; gap: 0.75rem; }
    main { padding: 2rem; max-width: 1200px; width: 100%; margin: 0 auto; }
    .login-link { color: var(--color-primary); text-decoration: none; font-weight: 500; }
  `]
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
}
