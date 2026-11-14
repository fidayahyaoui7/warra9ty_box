import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserPayload, UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';
import { ConfirmService } from '../services/confirm.service';
import { errorMessage, fullName } from '../utils/format';
import { User } from '../models/models';
import { ToastService } from '../services/toast.service';
import { Avatar } from '../shared/avatar/avatar';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';

type Filter = 'ALL' | 'ACTIVE' | 'PENDING';
const norm = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

@Component({
  selector: 'app-utilisateurs',
  imports: [ReactiveFormsModule, Icon, Modal, Avatar],
  templateUrl: './utilisateurs.html',
  styleUrl: './utilisateurs.css',
})
export class Utilisateurs {
  private fb = inject(FormBuilder);
  private api = inject(UserService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private confirm = inject(ConfirmService);

  roleNames = ['CLIENT', 'ADMIN'];
  list = signal<User[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  q = signal('');
  filter = signal<Filter>('ALL');

  formOpen = signal(false);
  editing = signal<User | null>(null);
  saving = signal(false);
  formError = signal<string | null>(null);
  roles = signal<string[]>(['CLIENT']);
  rolesMissing = signal(false);

  form = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    tel: [''],
    password: [''],
    enabled: [true],
  });

  pending = computed(() => this.list().filter(u => !u.enabled));
  filtered = computed(() => {
    const q = norm(this.q()), f = this.filter();
    return this.list().filter(u =>
      (f === 'ALL' || (f === 'ACTIVE' ? u.enabled : !u.enabled)) &&
      (!q || norm(`${fullName(u)} ${u.email} ${u.tel ?? ''}`).includes(q)));
  });

  constructor() { this.load(); }

  name = fullName;
  isMe(u: User) { return u.id === this.auth.session()?.id; }
  bad(name: keyof typeof this.form.controls) { const c = this.form.controls[name]; return c.touched && c.invalid; }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().subscribe({
      next: l => { this.list.set(l); this.loading.set(false); },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  toggle(u: User, value: boolean) {
    this.api.setEnabled(u.id, value).subscribe({
      next: () => { this.toast.success(value ? `Compte de ${fullName(u)} activé.` : `Compte de ${fullName(u)} désactivé.`); this.load(); },
      error: e => { this.toast.error(errorMessage(e)); this.load(); },
    });
  }

  toggleRole(r: string, on: boolean) {
    this.roles.update(l => (on ? [...new Set([...l, r])] : l.filter(x => x !== r)));
    this.rolesMissing.set(false);
  }

  openForm(u?: User) {
    this.editing.set(u ?? null);
    this.formError.set(null);
    this.rolesMissing.set(false);
    this.roles.set(u ? u.roles.map(r => r.libelle) : ['CLIENT']);
    this.form.reset({ prenom: u?.prenom ?? '', nom: u?.nom ?? '', email: u?.email ?? '', tel: u?.tel ?? '', password: '', enabled: u?.enabled ?? true });
    const pw = this.form.controls.password;
    pw.setValidators(u ? [Validators.minLength(6)] : [Validators.required, Validators.minLength(6)]);
    pw.updateValueAndValidity();
    this.formOpen.set(true);
  }

  save() {
    this.form.markAllAsTouched();
    this.rolesMissing.set(this.roles().length === 0);
    if (this.form.invalid || this.rolesMissing()) return;
    const v = this.form.getRawValue();
    const payload: UserPayload = {
      email: v.email.trim(), nom: v.nom.trim(), prenom: v.prenom.trim(), tel: v.tel.trim() || null,
      enabled: v.enabled, roles: this.roles(), ...(v.password ? { password: v.password } : {}),
    };
    const editing = this.editing();
    this.saving.set(true);
    this.formError.set(null);
    (editing ? this.api.update(editing.id, payload) : this.api.create(payload)).subscribe({
      next: () => { this.saving.set(false); this.formOpen.set(false); this.toast.success(editing ? 'Utilisateur modifié.' : 'Utilisateur créé.'); this.load(); },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }

  async remove(u: User) {
    const ok = await this.confirm.ask({
      title: 'Supprimer cet utilisateur ?', danger: true, confirmLabel: 'Supprimer',
      message: `Le compte de ${fullName(u)} sera supprimé. Un utilisateur qui a des achats ne peut pas être supprimé : désactivez son compte à la place.`,
    });
    if (!ok) return;
    this.api.remove(u.id).subscribe({
      next: () => { this.toast.success('Utilisateur supprimé.'); this.load(); },
      error: e => this.toast.error(errorMessage(e)),
    });
  }
}
