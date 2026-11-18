import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Reclamation, StatutReclamation } from '../models/models';
import { multipart } from './multipart';

const api = environment.apiUrl;

export interface ReclamationPayload { motif: string; description?: string | null; }

@Injectable({ providedIn: 'root' })
export class ReclamationService {
  private http = inject(HttpClient);
  list(): Observable<Reclamation[]> { return this.http.get<Reclamation[]>(`${api}/reclamations`); }
  create(achatId: number, r: ReclamationPayload, justificatif: File | null) {
    return this.http.post<Reclamation>(`${api}/reclamations`, multipart({ reclamation: r }, { justificatif }),
      { params: new HttpParams().set('achatId', achatId) });
  }
  setStatut(id: number, statut: StatutReclamation, commentaire?: string | null) {
    let params = new HttpParams().set('statut', statut);
    if (commentaire?.trim()) params = params.set('commentaire', commentaire.trim());
    return this.http.patch<Reclamation>(`${api}/reclamations/${id}/statut`, null, { params });
  }
  remove(id: number) { return this.http.delete<void>(`${api}/reclamations/${id}`); }
}
