import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AchatService } from '../services/achat.service';
import { ReclamationService } from '../services/reclamation.service';
import { AuthService } from '../services/auth.service';
import { ConfirmService } from '../services/confirm.service';
import { DocViewerService } from '../services/doc-viewer.service';
import { FileService } from '../services/file.service';
import { FrDatePipe, errorMessage, fullName } from '../utils/format';
import { Achat, Reclamation, StatutReclamation } from '../models/models';
import { ToastService } from '../services/toast.service';
import { Badge } from '../shared/badge/badge';
import { FileDrop } from '../shared/file-drop/file-drop';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';

type Filter = 'ALL' | StatutReclamation;
type Dot = '' | 'is-done' | 'is-current' | 'is-ok' | 'is-bad';

// RG4 : OUVERTE -> EN_COURS -> RESOLUE ou REFUSEE
const NEXT: Record<StatutReclamation, StatutReclamation[]> = {
  OUVERTE: ['EN_COURS'], EN_COURS: ['RESOLUE', 'REFUSEE'], RESOLUE: [], REFUSEE: [],
};
const MOTIFS = ['Panne', 'Écran défectueux', 'Batterie', 'Problème de charge', 'Défaut de fabrication', 'Bruit anormal', 'Accessoire manquant'];

@Component({
  selector: 'app-reclamations',
  imports: [ReactiveFormsModule, Icon, Modal, FileDrop, Badge, FrDatePipe],
  templateUrl: './reclamations.html',
  styleUrl: './reclamations.css',
})
export class Reclamations {
  auth = inject(AuthService);
  docs = inject(DocViewerService);
  private fb = inject(FormBuilder);
  private files = inject(FileService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);
  private claimApi = inject(ReclamationService);
  private achatApi = inject(AchatService);

  motifs = MOTIFS;
  list = signal<Reclamation[]>([]);
  achats = signal<Achat[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  filter = signal<Filter>('ALL');

  formOpen = signal(false);
  saving = signal(false);
  formError = signal<string | null>(null);
  justificatif = signal<File | null>(null);

  form = this.fb.nonNullable.group({
    achatId: this.fb.control<number | null>(null, Validators.required),
    motif: ['', Validators.required],
    description: [''],
  });

  // RG3 : seulement les achats dont la garantie n'est pas expirée
  eligible = computed(() => this.achats().filter(a => a.statutGarantie === 'ACTIVE' || a.statutGarantie === 'BIENTOT_EXPIREE'));

  counts = computed(() => {
    const l = this.list();
    const n = (s: StatutReclamation) => l.filter(r => r.statut === s).length;
    return { ALL: l.length, OUVERTE: n('OUVERTE'), EN_COURS: n('EN_COURS'), RESOLUE: n('RESOLUE'), REFUSEE: n('REFUSEE') };
  });
  filtered = computed(() => {
    const f = this.filter();
    return [...this.list()].filter(r => f === 'ALL' || r.statut === f).sort((a, b) => b.id - a.id);
  });

  constructor() { this.load(); }

  name = fullName;
  img(r: Reclamation) { return this.files.produitUrl(r.achat.produit.photo); }
  next(r: Reclamation) { return NEXT[r.statut]; }
  bad(name: 'achatId' | 'motif') { const c = this.form.controls[name]; return c.touched && c.invalid; }

  steps(r: Reclamation): { label: string; dot: Dot }[] {
    const s = r.statut;
    const last = s === 'RESOLUE' ? { label: 'Résolue', dot: 'is-ok' as Dot }
      : s === 'REFUSEE' ? { label: 'Refusée', dot: 'is-bad' as Dot }
      : { label: 'Résolue ou refusée', dot: '' as Dot };
    return [
      { label: 'Ouverte', dot: s === 'OUVERTE' ? 'is-current' : 'is-done' },
      { label: 'En cours', dot: s === 'EN_COURS' ? 'is-current' : s === 'OUVERTE' ? '' : 'is-done' },
      last,
    ];
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ r: this.claimApi.list(), a: this.achatApi.list() }).subscribe({
      next: x => {
        this.list.set(x.r);
        this.achats.set(x.a);
        this.loading.set(false);
        const id = Number(this.route.snapshot.queryParamMap.get('achat'));
        if (id) { this.router.navigate([], { queryParams: {}, replaceUrl: true }); this.openForm(id); }
      },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  openForm(achatId: number | null = null) {
    this.justificatif.set(null);
    this.formError.set(null);
    this.form.reset({ achatId, motif: '', description: '' });
    this.formOpen.set(true);
  }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);
    this.claimApi.create(v.achatId!, { motif: v.motif.trim(), description: v.description?.trim() || null }, this.justificatif()).subscribe({
      next: () => { this.saving.set(false); this.formOpen.set(false); this.toast.success('Réclamation envoyée.'); this.load(); },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }

  // changement de statut (admin) : une fenêtre propose d'ajouter un message pour le client
  statutTarget = signal<{ r: Reclamation; s: StatutReclamation } | null>(null);
  statutNote = signal('');
  statutSaving = signal(false);

  setStatut(r: Reclamation, s: StatutReclamation) {
    this.statutNote.set('');
    this.statutTarget.set({ r, s });
  }

  statutTitle(s: StatutReclamation) {
    return s === 'EN_COURS' ? 'Prendre en charge la réclamation' : s === 'RESOLUE' ? 'Marquer comme résolue' : 'Refuser la réclamation';
  }

  confirmStatut() {
    const t = this.statutTarget();
    if (!t || this.statutSaving()) return;
    this.statutSaving.set(true);
    this.claimApi.setStatut(t.r.id, t.s, this.statutNote()).subscribe({
      next: () => {
        this.statutSaving.set(false);
        this.statutTarget.set(null);
        this.toast.success(t.s === 'EN_COURS' ? 'Réclamation prise en charge.' : t.s === 'RESOLUE' ? 'Réclamation résolue.' : 'Réclamation refusée.');
        this.load();
      },
      error: e => { this.statutSaving.set(false); this.toast.error(errorMessage(e)); },
    });
  }

  // vers la messagerie, avec la réclamation (et le client pour l'admin) déjà choisis
  write(r: Reclamation) {
    this.router.navigate(['/messages'], { queryParams: this.auth.isAdmin() ? { client: r.achat.user.id, reclamation: r.id } : { reclamation: r.id } });
  }

  async remove(r: Reclamation) {
    const ok = await this.confirm.ask({
      title: 'Supprimer la réclamation ?', danger: true, confirmLabel: 'Supprimer',
      message: `« ${r.motif} » et son justificatif PDF seront supprimés définitivement.`,
    });
    if (!ok) return;
    this.claimApi.remove(r.id).subscribe({
      next: () => { this.toast.success('Réclamation supprimée.'); this.load(); },
      error: e => this.toast.error(errorMessage(e)),
    });
  }
}
