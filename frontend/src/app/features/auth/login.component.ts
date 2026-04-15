import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-wrapper">
      <form class="card stack" [formGroup]="form" (ngSubmit)="submit()">
        <h2>Sign in</h2>
        <div>
          <label for="username">Username</label>
          <input id="username" type="text" formControlName="username" autocomplete="username" />
        </div>
        <div>
          <label for="password">Password</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" />
        </div>
        <button type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Signing in...' : 'Sign in' }}
        </button>
        <p class="error" *ngIf="errorMessage()">{{ errorMessage() }}</p>
      </form>
    </div>
  `,
  styles: [`
    .login-wrapper { display: flex; justify-content: center; padding-top: 4rem; }
    form { width: 360px; }
    h2 { margin: 0 0 0.5rem; }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(64)]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/transactions']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Unable to sign in');
      }
    });
  }
}
