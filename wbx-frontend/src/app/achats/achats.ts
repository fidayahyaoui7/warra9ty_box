import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of, startWith, catchError } from 'rxjs';
import { AchatPayload, AchatService } from '../services/achat.service';
import { ProduitService } from '../services/produit.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';
import { ConfirmService } from '../services/confirm.service';
import { DocViewerService } from '../services/doc-viewer.service';
import { FileService } from '../services/file.service';
import { DtPipe, FrDatePipe, addMonthsIso, errorMessage, frDate, fullName, parseDate, todayIso } from '../utils/format';
import { Achat, Produit, StatutGarantie, User } from '../models/models';
import { ToastService } from '../services/toast.service';
import { FileDrop } from '../shared/file-drop/file-drop';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';
import { Ticket } from '../shared/ticket/ticket';

type Filter = 'ALL' | StatutGarantie;
const norm = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

@Component({
  selector: 'app-achats',
  imports: [ReactiveFormsModule, Icon, Modal, FileDrop, Ticket, DtPipe, FrDatePipe],
  templateUrl: './achats.html',
  styleUrl: './achats.css',
})
export class Achats {
  auth = inject(AuthService);
  docs = inject(DocViewerService);
  private fb = inject(FormBuilder);
  private files = inject(FileService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);
  private achatApi = inject(AchatService);
  private produitApi = inject(ProduitService);
  private userApi = inject(UserService);

  today = todayIso();
  list = signal<Achat[]>([]);
  produits = signal<Produit[]>([]);
  users = signal<User[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  q = signal('');
  filter = signal<Filter>('ALL');

  formOpen = signal(false);
  editing = signal<Achat | null>(null);
  saving = signal(false);
  formError = signal<string | null>(null);
  facture = signal<File | null>(null);
  photo = signal<File | null>(null);
  factureMissing = signal(false);

  form = this.fb.nonNullable.group({
    produitId: this.fb.control<number | null>(null, Validators.required),
    dateAchat: ['', Validators.required],
    prixPaye: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.001)]),
    magasin: [''],
    numeroSerie: [''],
    dureeGarantie: this.fb.control<number | null>(null, Validators.min(1)),
    userId: this.fb.control<number | null>(null),
  });
  private value = toSignal(this.form.valueChanges.pipe(startWith(this.form.getRawValue())), { initialValue: this.form.getRawValue() });

  selected = computed(() => this.produits().find(p => p.id === this.value().produitId) ?? null);
  /** RG1 : aperçu de la date de fin de garantie */
  endPreview = computed(() => {
    const v = this.value();
    const months = v.dureeGarantie || this.selected()?.dureeGarantieStandard || 0;
    return v.dateAchat ? addMonthsIso(v.dateAchat, months) : null;
  });
  groups = computed(() => {
    const by = new Map<string, Produit[]>();
    for (const p of this.produits()) by.set(p.categorie.libelle, [...(by.get(p.categorie.libelle) ?? []), p]);
    return [...by.entries()].map(([label, items]) => ({ label, items }));
  });

  counts = computed(() => {
    const l = this.list();
    const n = (s: StatutGarantie) => l.filter(a => a.statutGarantie === s).length;
    return { ALL: l.length, ACTIVE: n('ACTIVE'), BIENTOT_EXPIREE: n('BIENTOT_EXPIREE'), EXPIREE: n('EXPIREE') };
  });

  filtered = computed(() => {
    const q = norm(this.q());
    const f = this.filter();
    const rows = this.list().filter(a =>
      (f === 'ALL' || a.statutGarantie === f) &&
      (!q || norm(`${a.produit.nom} ${a.produit.marque} ${a.magasin ?? ''} ${a.numeroSerie ?? ''} ${fullName(a.user)}`).includes(q)));
    const end = (a: Achat) => parseDate(a.dateFinGarantie)?.getTime() ?? 0;
    const live = rows.filter(a => a.statutGarantie !== 'EXPIREE').sort((a, b) => end(a) - end(b));
    const dead = rows.filter(a => a.statutGarantie === 'EXPIREE').sort((a, b) => end(b) - end(a));
    return [...live, ...dead];
  });

  constructor() {
    // Un produit choisi pré-remplit le prix payé avec le prix du catalogue
    this.form.controls.produitId.valueChanges.pipe(takeUntilDestroyed()).subscribe(id => {
      const p = this.produits().find(x => x.id === id);
      if (p && !this.editing() && !this.form.controls.prixPaye.value) this.form.controls.prixPaye.setValue(p.prix);
    });
    this.load();
  }

  img(p: Produit) { return this.files.produitUrl(p.photo); }
  name = fullName;
  bad(name: keyof typeof this.form.controls) { const c = this.form.controls[name]; return c.touched && c.invalid; }

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      achats: this.achatApi.list(),
      produits: this.produitApi.list(),
      users: this.auth.isAdmin() ? this.userApi.list().pipe(catchError(() => of([] as User[]))) : of([] as User[]),
    }).subscribe({
      next: r => {
        this.list.set(r.achats);
        this.produits.set(r.produits);
        this.users.set(r.users.filter(u => u.enabled));
        this.loading.set(false);
        this.openFromQuery();
      },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  // /achats?nouveau=1&produit=3 : depuis le catalogue ou le tableau de bord
  private openFromQuery() {
    const qp = this.route.snapshot.queryParamMap;
    if (qp.get('nouveau')) {
      const produit = Number(qp.get('produit')) || null;
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
      this.openForm(undefined, produit);
    }
  }

  openForm(a?: Achat, produitId: number | null = null) {
    this.editing.set(a ?? null);
    this.facture.set(null);
    this.photo.set(null);
    this.factureMissing.set(false);
    this.formError.set(null);
    this.form.reset({
      produitId: a?.produit.id ?? produitId,
      dateAchat: a?.dateAchat ?? '',
      prixPaye: a?.prixPaye ?? null,
      magasin: a?.magasin ?? '',
      numeroSerie: a?.numeroSerie ?? '',
      dureeGarantie: a?.dureeGarantie ?? null,
      userId: null,
    });
    const p = this.produits().find(x => x.id === produitId);
    if (!a && p) this.form.controls.prixPaye.setValue(p.prix);
    this.formOpen.set(true);
  }
  closeForm() { this.formOpen.set(false); }

  save() {
    this.form.markAllAsTouched();
    const editing = this.editing();
    this.factureMissing.set(!editing && !this.facture());
    const v = this.form.getRawValue();
    if (v.dateAchat && v.dateAchat > this.today) this.form.controls.dateAchat.setErrors({ future: true });
    if (this.form.invalid || this.factureMissing()) return;

    const payload: AchatPayload = {
      dateAchat: v.dateAchat,
      prixPaye: v.prixPaye!,
      magasin: v.magasin?.trim() || null,
      numeroSerie: v.numeroSerie?.trim() || null,
      dureeGarantie: v.dureeGarantie || null,
      produit: { id: v.produitId! },
    };
    this.saving.set(true);
    this.formError.set(null);
    const req = editing
      ? this.achatApi.update(editing.id, payload, this.facture(), this.photo())
      : this.achatApi.create(payload, this.facture()!, this.photo(), v.userId);
    req.subscribe({
      next: a => {
        this.saving.set(false);
        this.formOpen.set(false);
        this.toast.success(editing ? 'Achat modifié.' : `Achat enregistré. Garantie jusqu’au ${frDate(a.dateFinGarantie)}.`);
        this.load();
      },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }

  async remove(a: Achat) {
    const ok = await this.confirm.ask({
      title: 'Supprimer cet achat ?', danger: true, confirmLabel: 'Supprimer',
      message: `« ${a.produit.nom} » et sa facture PDF seront supprimés définitivement. Un achat qui a des réclamations ne peut pas être supprimé.`,
    });
    if (!ok) return;
    this.achatApi.remove(a.id).subscribe({
      next: () => { this.toast.success('Achat supprimé.'); this.load(); },
      error: e => this.toast.error(errorMessage(e)),
    });
  }

  claim(a: Achat) { this.router.navigate(['/reclamations'], { queryParams: { achat: a.id } }); }
}
