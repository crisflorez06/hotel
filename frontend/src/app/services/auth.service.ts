import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { FeedbackToastService } from '../core/services/feedback-toast.service';
import { extractBackendErrorMessage } from '../core/utils/http-error.util';

export interface LoginRequest {
  usuario: string;
  password: string;
}

export interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly feedbackToast = inject(FeedbackToastService);

  private readonly storageKey = 'auth_token';

  readonly authState = signal<AuthState>({
    token: this.getStoredToken(),
    isAuthenticated: Boolean(this.getStoredToken()),
  });

  login(request: LoginRequest) {
    return this.http
      .post<{ token: string }>(`${environment.authUrl}/login`, request)
      .pipe(
        tap((response) => {
          this.storeToken(response.token);
          this.authState.set({ token: response.token, isAuthenticated: true });
          this.router.navigate(['/recepcion']);
          this.feedbackToast.showSuccess('Sesión iniciada correctamente.');
        })
      );
  }

  logout(): void {
    this.clearToken();
    this.authState.set({ token: null, isAuthenticated: false });
    this.router.navigate(['/login']);
    this.feedbackToast.showSuccess('Sesión cerrada.');
  }

  getToken(): string | null {
    return this.authState().token;
  }

  isAuthenticated(): boolean {
    return this.authState().isAuthenticated;
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.storageKey, token);
  }

  private getStoredToken(): string | null {
    return localStorage.getItem(this.storageKey);
  }

  private clearToken(): void {
    localStorage.removeItem(this.storageKey);
  }
}
