import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  username: string;
  role: string;
  billerCode?: string;
}

const STORAGE_KEY = 'fawry.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<AuthSession | null>(this.readStoredSession());

  readonly currentUser = computed(() => this.session());
  readonly token = computed(() => this.session()?.accessToken ?? null);

  login(username: string, password: string): Observable<AuthSession> {
    return this.http
      .post<AuthSession>(`${environment.apiBaseUrl}/auth/login`, { username, password })
      .pipe(tap((session) => this.setSession(session)));
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(STORAGE_KEY);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return this.session() !== null;
  }

  private setSession(session: AuthSession): void {
    this.session.set(session);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  private readStoredSession(): AuthSession | null {
    const raw = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthSession;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
