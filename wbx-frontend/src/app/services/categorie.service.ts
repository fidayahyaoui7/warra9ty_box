import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Categorie } from '../models/models';

const api = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class CategorieService {
  private http = inject(HttpClient);
  list(): Observable<Categorie[]> { return this.http.get<Categorie[]>(`${api}/categories`); }
  create(c: Categorie) { return this.http.post<Categorie>(`${api}/categories`, c); }
  update(id: number, c: Categorie) { return this.http.put<Categorie>(`${api}/categories/${id}`, c); }
  remove(id: number) { return this.http.delete<void>(`${api}/categories/${id}`); }
}
