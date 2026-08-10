import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

export interface RegisterCommand {
  name: string;
  email: string;
  password: string;
}

export interface LoginCommand {
  email: string;
  password: string;
}

export interface CurrentUser {
  id: number;
  name: string;
  email: string;
}

interface AuthenticationResult {
  token: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly tokenStorageKey = 'sudolife.jwt';

  private readonly http = inject(HttpClient);

  register(command: RegisterCommand): Observable<void> {
    return this.http.post<void>('/api/users/register', command);
  }

  login(command: LoginCommand): Observable<void> {
    return this.http.post<AuthenticationResult>('/api/users/login', command).pipe(
      tap((result) => localStorage.setItem(AuthService.tokenStorageKey, result.token)),
      map(() => undefined),
    );
  }

  currentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>('/api/users/me');
  }

  token(): string | null {
    return localStorage.getItem(AuthService.tokenStorageKey);
  }

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  logout(): void {
    localStorage.removeItem(AuthService.tokenStorageKey);
  }
}
