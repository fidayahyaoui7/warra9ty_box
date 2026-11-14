import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CategorieService } from '../services/categorie.service';
import { ProduitService } from '../services/produit.service';
import { AuthService } from '../services/auth.service';
import { ConfirmService } from '../services/confirm.service';
import { FileService } from '../services/file.service';
import { DtPipe, errorMessage } from '../utils/format';
import { Categorie, Produit } from '../models/models';
import { ToastService } from '../services/toast.service';
import { FileDrop } from '../shared/file-drop/file-drop';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';
import { forkJoin } from 'rxjs';

const norm = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

@Component({
  selector: 'app-catalogue',
  imports: [ReactiveFormsModule, Icon, Modal, FileDrop, DtPipe],
  templateUrl: './catalogue.html',
  styleUrl: './catalogue.css',
})
export class Catalogue {
  auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private files = inject(FileService);
  private router = inject(Router);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);
  private produitApi = inject(ProduitService);
  private categorieApi = inject(CategorieService);

  produits = signal<Produit[]>([]);
  categories = signal<Categorie[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  q = signal('');
  cat = signal<number | null>(null);
  detail = signal<Produit | null>(null);

  formOpen = signal(false);
  editing = signal<Produit | null>(null);
  saving = signal(false);
  formError = signal<string | null>(null);
  photo = signal<File | null>(null);
  photoMissing = signal(false);

  form = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    marque: ['', Validators.required],
    modele: [''],
    categorieId: this.fb.control<number | null>(null, Validators.required),
    prix: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
    dureeGarantieStandard: this.fb.control<number | null>(24, [Validators.required, Validators.min(1)]),
    description: [''],
  });

  private matchesQuery = (p: Produit) => {
    const q = norm(this.q());
    return !q || norm(`${p.nom} ${p.marque} ${p.modele ?? ''}`).includes(q);
  };

  filtered = computed(() => this.produits().filter(p => this.matchesQuery(p) && (this.cat() === null || p.categorie.id === this.cat())));
  counts = computed(() => {
    const matching = this.produits().filter(this.matchesQuery);
    const by: Record<number, number> = {};
    for (const p of matching) by[p.categorie.id!] = (by[p.categorie.id!] ?? 0) + 1;
    return { total: matching.length, by };
  });

  constructor() { this.load(); }

  img(p: Produit) { return this.files.produitUrl(p.photo); }
  bad(name: keyof typeof this.form.controls) { const c = this.form.controls[name]; return c.touched && c.invalid; }
  reset() { this.q.set(''); this.cat.set(null); }

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ p: this.produitApi.list(), c: this.categorieApi.list() }).subscribe({
      next: r => { this.produits.set(r.p); this.categories.set(r.c); this.loading.set(false); },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  bought(p: Produit) {
    this.detail.set(null);
    this.router.navigate(['/achats'], { queryParams: { nouveau: 1, produit: p.id } });
  }

  openForm(p?: Produit) {
    this.editing.set(p ?? null);
    this.photo.set(null);
    this.photoMissing.set(false);
    this.formError.set(null);
    this.form.reset({
      nom: p?.nom ?? '', marque: p?.marque ?? '', modele: p?.modele ?? '',
      categorieId: p?.categorie.id ?? this.cat(), prix: p?.prix ?? null,
      dureeGarantieStandard: p?.dureeGarantieStandard ?? 24, description: p?.description ?? '',
    });
    this.formOpen.set(true);
  }
  closeForm() { this.formOpen.set(false); }

  save() {
    this.form.markAllAsTouched();
    const editing = this.editing();
    this.photoMissing.set(!editing && !this.photo());
    if (this.form.invalid || this.photoMissing()) return;
    const v = this.form.getRawValue();
    const payload: Produit = {
      nom: v.nom.trim(), marque: v.marque.trim(), modele: v.modele?.trim() || null,
      prix: v.prix!, dureeGarantieStandard: v.dureeGarantieStandard!, description: v.description?.trim() || null,
      categorie: { id: v.categorieId!, libelle: '' },
    };
    this.saving.set(true);
    this.formError.set(null);
    const req = editing ? this.produitApi.update(editing.id!, payload, this.photo()) : this.produitApi.create(payload, this.photo()!);
    req.subscribe({
      next: () => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.toast.success(editing ? 'Produit modifié.' : 'Produit ajouté au catalogue.');
        this.load();
      },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }

  async remove(p: Produit) {
    const ok = await this.confirm.ask({
      title: 'Supprimer ce produit ?', danger: true, confirmLabel: 'Supprimer',
      message: `« ${p.nom} » sera retiré du catalogue. Un produit déjà acheté par un utilisateur ne peut pas être supprimé.`,
    });
    if (!ok) return;
    this.produitApi.remove(p.id!).subscribe({
      next: () => { this.toast.success('Produit supprimé.'); this.load(); },
      error: e => this.toast.error(errorMessage(e)),
    });
  }
}
