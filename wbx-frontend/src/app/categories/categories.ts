import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CategorieService } from '../services/categorie.service';
import { ProduitService } from '../services/produit.service';
import { ConfirmService } from '../services/confirm.service';
import { errorMessage } from '../utils/format';
import { Categorie, Produit } from '../models/models';
import { ToastService } from '../services/toast.service';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';

@Component({
  selector: 'app-categories',
  imports: [ReactiveFormsModule, Icon, Modal],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
})
export class Categories {
  private fb = inject(FormBuilder);
  private api = inject(CategorieService);
  private produitApi = inject(ProduitService);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  list = signal<Categorie[]>([]);
  produits = signal<Produit[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  formOpen = signal(false);
  editing = signal<Categorie | null>(null);
  saving = signal(false);
  formError = signal<string | null>(null);

  form = this.fb.nonNullable.group({ libelle: ['', Validators.required], description: [''] });
  counts = computed(() => {
    const by: Record<number, number> = {};
    for (const p of this.produits()) by[p.categorie.id!] = (by[p.categorie.id!] ?? 0) + 1;
    return by;
  });

  constructor() { this.load(); }

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({ c: this.api.list(), p: this.produitApi.list() }).subscribe({
      next: r => { this.list.set(r.c); this.produits.set(r.p); this.loading.set(false); },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  openForm(c?: Categorie) {
    this.editing.set(c ?? null);
    this.formError.set(null);
    this.form.reset({ libelle: c?.libelle ?? '', description: c?.description ?? '' });
    this.formOpen.set(true);
  }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const payload: Categorie = { libelle: v.libelle.trim(), description: v.description.trim() || null };
    const editing = this.editing();
    this.saving.set(true);
    this.formError.set(null);
    (editing ? this.api.update(editing.id!, payload) : this.api.create(payload)).subscribe({
      next: () => { this.saving.set(false); this.formOpen.set(false); this.toast.success(editing ? 'Catégorie modifiée.' : 'Catégorie créée.'); this.load(); },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }

  async remove(c: Categorie) {
    const ok = await this.confirm.ask({
      title: 'Supprimer cette catégorie ?', danger: true, confirmLabel: 'Supprimer',
      message: `« ${c.libelle} » sera supprimée. Une catégorie qui contient des produits ne peut pas être supprimée.`,
    });
    if (!ok) return;
    this.api.remove(c.id!).subscribe({
      next: () => { this.toast.success('Catégorie supprimée.'); this.load(); },
      error: e => this.toast.error(errorMessage(e)),
    });
  }
}
