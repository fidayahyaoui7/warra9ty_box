import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Conversation, Message } from '../models/models';
import { AuthService } from './auth.service';

const api = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class MessageService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  /** Messages non lus (badge du menu) : réponses de l'équipe pour un client, messages des clients pour l'admin */
  readonly unread = signal(0);

  refreshUnread() {
    const url = this.auth.isAdmin() ? `${api}/admin/messages/non-lus` : `${api}/messages/non-lus`;
    this.http.get<{ count: number }>(url).subscribe({ next: r => this.unread.set(r.count), error: () => undefined });
  }

  // ----- client : sa conversation avec l'équipe -----
  mine(): Observable<Message[]> { return this.http.get<Message[]>(`${api}/messages`); }
  send(contenu: string, reclamationId: number | null) {
    return this.http.post<Message>(`${api}/messages`, { contenu, reclamationId });
  }
  markRead() { return this.http.patch<{ updated: number }>(`${api}/messages/lu`, null); }

  // ----- admin : boîte de réception -----
  conversations(): Observable<Conversation[]> { return this.http.get<Conversation[]>(`${api}/admin/messages/conversations`); }
  thread(clientId: number): Observable<Message[]> { return this.http.get<Message[]>(`${api}/admin/messages/${clientId}`); }
  sendTo(clientId: number, contenu: string, reclamationId: number | null) {
    return this.http.post<Message>(`${api}/admin/messages/${clientId}`, { contenu, reclamationId });
  }
  markReadAdmin(clientId: number) { return this.http.patch<{ updated: number }>(`${api}/admin/messages/${clientId}/lu`, null); }
}
