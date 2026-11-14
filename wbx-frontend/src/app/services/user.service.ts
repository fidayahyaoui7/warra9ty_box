import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/models';
import { multipart } from './multipart';

const api = environment.apiUrl;

export interface UserPayload {
  email: string;
  password?: string;
  nom: string;
  prenom: string;
  tel?: string | null;
  enabled?: boolean;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  /** Comptes en attente d'activation (affiché dans le menu de l'admin) */
  readonly pending = signal(0);
  list(): Observable<User[]> {
    return this.http.get<User[]>(`${api}/users`).pipe(tap(l => this.pending.set(l.filter(u => !u.enabled).length)));
  }
  create(u: UserPayload) { return this.http.post<User>(`${api}/users`, u); }
  update(id: number, u: UserPayload) { return this.http.put<User>(`${api}/users/${id}`, u); }
  setEnabled(id: number, value: boolean) {
    return this.http.patch<User>(`${api}/users/${id}/enabled`, null, { params: new HttpParams().set('value', value) });
  }
  remove(id: number) { return this.http.delete<void>(`${api}/users/${id}`); }

  profile() { return this.http.get<User>(`${api}/profile`); }
  updateProfile(p: { nom: string; prenom: string; tel?: string | null; password?: string | null }, photo: File | null) {
    return this.http.put<User>(`${api}/profile`, multipart({ user: p }, { photo }));
  }
}
