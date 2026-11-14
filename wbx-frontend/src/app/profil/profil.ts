import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';
import { errorMessage, fullName } from '../utils/format';
import { ToastService } from '../services/toast.service';
import { Avatar } from '../shared/avatar/avatar';
import { FileDrop } from '../shared/file-drop/file-drop';
import { Icon } from '../shared/icon/icon';

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule, Avatar, FileDrop, Icon],
  templateUrl: './profil.html',
  styleUrl: './profil.css',
})
export class Profil {
  auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private api = inject(UserService);
  private toast = inject(ToastService);

  photo = signal<File | null>(null);
  saving = signal(false);
  formError = signal<string | null>(null);
  name = computed(() => fullName(this.auth.session()));

  form = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    tel: [''],
    password: ['', Validators.minLength(6)],
  });

  constructor() {
    const s = this.auth.session();
    if (s) this.form.patchValue({ prenom: s.prenom, nom: s.nom });
    this.api.profile().subscribe({
      next: u => { this.form.patchValue({ prenom: u.prenom, nom: u.nom, tel: u.tel ?? '' }); this.auth.patch(u); },
      error: () => undefined,
    });
  }

  bad(name: keyof typeof this.form.controls) { const c = this.form.controls[name]; return c.touched && c.invalid; }

  save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);
    this.api.updateProfile({ prenom: v.prenom.trim(), nom: v.nom.trim(), tel: v.tel.trim() || null, password: v.password || null }, this.photo()).subscribe({
      next: u => {
        this.auth.patch(u);
        this.photo.set(null);
        this.form.patchValue({ password: '' });
        this.saving.set(false);
        this.toast.success('Profil enregistré.');
      },
      error: e => { this.saving.set(false); this.formError.set(errorMessage(e)); },
    });
  }
}
