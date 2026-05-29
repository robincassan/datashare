  import { Injectable } from '@angular/core';
  import { HttpClient } from '@angular/common/http';
  import { Observable, tap } from 'rxjs';

  @Injectable({ providedIn: 'root' })
  export class AuthService {
    private apiUrl = 'http://localhost:8080/api/auth';
    private tokenKey = 'jwt_token';

    constructor(private http: HttpClient) {}

    register(email: string, password: string): Observable<any> {
      return this.http.post(`${this.apiUrl}/register`, { email, password });
    }

    login(email: string, password: string): Observable<any> {
      return this.http.post<{ token: string }>(`${this.apiUrl}/login`, { email, password }).pipe(
        tap(response => localStorage.setItem(this.tokenKey, response.token))
      );
    }

    getToken(): string | null {
      return localStorage.getItem(this.tokenKey);
    }

    isLoggedIn(): boolean {
      return !!this.getToken();
    }

    logout(): void {
      localStorage.removeItem(this.tokenKey);
    }
  }