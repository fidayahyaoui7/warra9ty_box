import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Achat } from '../models/models';
import { multipart } from './multipart';

const api = environment.apiUrl;

export interface AchatPayload {
  dateAchat: string;
  prixPaye: number;
  magasin?: string | null;
  numeroSerie?: string | null;
  dureeGarantie?: number | null;
  produit: { id: number };
}

@Injectable({ providedIn: 'root' })
export class AchatService {
  private http = inject(HttpClient);
  list(): Observable<Achat[]> { return this.http.get<Achat[]>(`${api}/achats`); }
  create(a: AchatPayload, facture: File, photo: File | null, userId?: number | null) {
    let params = new HttpParams();
    if (userId) params = params.set('userId', userId);
    return this.http.post<Achat>(`${api}/achats`, multipart({ achat: a }, { facture, photo }), { params });
  }
  update(id: number, a: AchatPayload, facture: File | null, photo: File | null) {
    return this.http.put<Achat>(`${api}/achats/${id}`, multipart({ achat: a }, { facture, photo }));
  }
  remove(id: number) { return this.http.delete<void>(`${api}/achats/${id}`); }
}
