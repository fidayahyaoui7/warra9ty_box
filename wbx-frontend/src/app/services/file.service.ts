import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {  environment  } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FileService {
  private http = inject(HttpClient);
  private api = environment.apiUrl;

  /** Images publiques du catalogue et des profils (<img> ne peut pas envoyer de token). */
  produitUrl(name?: string | null): string | null { return name ? `${this.api}/files/produits/${name}` : null; }
  profilUrl(name?: string | null): string | null { return name ? `${this.api}/files/profils/${name}` : null; }

  /** Fichiers protégés (facture, photo d'achat, justificatif) : récupérés avec le JWT. */
  blob(path: string): Observable<Blob> {
    return this.http.get(`${this.api}${path}`, { responseType: 'blob' });
  }

  save(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }
}
