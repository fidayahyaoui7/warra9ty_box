import { Injectable, inject, signal } from '@angular/core';
import {  Achat, Reclamation  } from '../models/models';
import {  FileService  } from './file.service';
import {  ToastService  } from './toast.service';

export interface ViewerDoc {
  title: string;
  kind: 'pdf' | 'image';
  path: string;        // ex. /achats/3/facture
  filename: string;    // nom proposé au téléchargement
}

@Injectable({ providedIn: 'root' })
export class DocViewerService {
  readonly doc = signal<ViewerDoc | null>(null);
  private files = inject(FileService);
  private toast = inject(ToastService);

  open(doc: ViewerDoc) { this.doc.set(doc); }
  close() { this.doc.set(null); }

  facture(a: Achat) {
    this.open({ title: `Facture · ${a.produit.nom}`, kind: 'pdf', path: `/achats/${a.id}/facture`, filename: `facture-${a.produit.nom}.pdf` });
  }
  photo(a: Achat) {
    this.open({ title: `Photo · ${a.produit.nom}`, kind: 'image', path: `/achats/${a.id}/photo`, filename: `photo-${a.produit.nom}.jpg` });
  }
  justificatif(r: Reclamation) {
    this.open({ title: `Justificatif · ${r.motif}`, kind: 'pdf', path: `/reclamations/${r.id}/justificatif`, filename: `justificatif-${r.id}.pdf` });
  }

  /** Téléchargement direct de la facture (bouton de la carte) */
  downloadFacture(a: Achat) {
    this.files.blob(`/achats/${a.id}/facture`).subscribe({
      next: b => this.files.save(b, `facture-${a.produit.nom.replace(/\s+/g, '-')}.pdf`),
      error: () => this.toast.error('Téléchargement impossible : facture introuvable.'),
    });
  }
}
