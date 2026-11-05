import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments';

interface TokenResponse {
  token: string;
  expiresInSeconds: number;
}

/**
 * Holds the JWT in memory only (not localStorage), so a script injected into the page cannot
 * read a token that persists across sessions. A refresh means logging in again.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private token = signal<string | null>(null);
  private expiresAt = 0;

  readonly loggedIn = computed(() => this.token() !== null);

  getToken(): string | null {
    if (this.token() && Date.now() > this.expiresAt) {
      this.token.set(null);
    }
    return this.token();
  }

  register(username: string, password: string, phone: string): Observable<unknown> {
    return this.http.post(`${environment.apiUrl}/auth/register`, { username, password, phone: phone || null });
  }

  login(username: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, { username, password }).pipe(
      tap((res) => {
        this.token.set(res.token);
        this.expiresAt = Date.now() + res.expiresInSeconds * 1000;
      }),
    );
  }

  logout(): void {
    this.token.set(null);
  }
}
