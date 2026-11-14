import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, switchMap, tap, catchError, of } from 'rxjs';
import {  environment  } from '../../environments/environment';
import {  RegisterPayload, RoleName, Session, User  } from '../models/models';
import {  ToastService  } from './toast.service';

const KEY = 'wbx.session';

function tokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch { return true; }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private toast = inject(ToastService);
  private api = environment.apiUrl;

  readonly session = signal<Session | null>(this.restore());
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.session()?.roles.includes('ADMIN') ?? false);
  readonly token = computed(() => this.session()?.token ?? null);

  private restore(): Session | null {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return null;
      const s = JSON.parse(raw) as Session;
      if (!s.token || tokenExpired(s.token)) { localStorage.removeItem(KEY); return null; }
      return s;
    } catch { return null; }
  }

  private store(s: Session | null) {
    this.session.set(s);
    try { s ? localStorage.setItem(KEY, JSON.stringify(s)) : localStorage.removeItem(KEY); } catch { /* stockage indisponible */ }
  }

  hasRole(role: RoleName): boolean { return this.session()?.roles.includes(role) ?? false; }

  login(email: string, password: string): Observable<Session> {
    return this.http.post<{ token: string; id: number; email: string; nom: string; prenom: string; roles: RoleName[] }>(
      `${this.api}/auth/login`, { email, password }).pipe(
      tap(r => this.store({ token: r.token, id: r.id, email: r.email, nom: r.nom, prenom: r.prenom, roles: r.roles })),
      // la photo de profil n'est pas dans la réponse du login : on la récupère ensuite
      switchMap(() => this.refreshProfile().pipe(catchError(() => of(null)))),
      map(() => this.session() as Session));
  }

  register(p: RegisterPayload): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.api}/auth/register`, p);
  }

  refreshProfile(): Observable<User> {
    return this.http.get<User>(`${this.api}/profile`).pipe(tap(u => this.patch(u)));
  }

  patch(u: User) {
    const s = this.session();
    if (s) this.store({ ...s, nom: u.nom, prenom: u.prenom, email: u.email, photoProfil: u.photoProfil ?? null });
  }

  logout(message?: string) {
    this.store(null);
    if (message) this.toast.info(message);
    this.router.navigate(['/connexion']);
  }
}
