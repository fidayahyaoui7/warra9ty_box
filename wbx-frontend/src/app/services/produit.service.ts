import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Produit } from '../models/models';
import { multipart } from './multipart';

const api = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class ProduitService {
  private http = inject(HttpClient);
  list(): Observable<Produit[]> { return this.http.get<Produit[]>(`${api}/produits`); }
  create(p: Produit, photo: File) {
    return this.http.post<Produit>(`${api}/produits`, multipart({ produit: p }, { photo }));
  }
  update(id: number, p: Produit, photo: File | null) {
    return this.http.put<Produit>(`${api}/produits/${id}`, multipart({ produit: p }, { photo }));
  }
  remove(id: number) { return this.http.delete<void>(`${api}/produits/${id}`); }
}
