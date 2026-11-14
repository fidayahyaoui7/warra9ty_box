import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { errorMessage } from '../utils/format';
import { AuthHero } from '../shared/auth-hero/auth-hero';
import { Icon } from '../shared/icon/icon';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, AuthHero, Icon],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  form = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    tel: [''],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });
  loading = signal(false);
  error = signal<string | null>(null);
  done = signal(false);

  invalid(name: 'prenom' | 'nom' | 'email' | 'password') { const c = this.form.controls[name]; return c.touched && c.invalid; }

  submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.auth.register({ ...v, email: v.email.trim(), tel: v.tel.trim() || undefined }).subscribe({
      next: () => { this.done.set(true); this.loading.set(false); },
      error: e => { this.error.set(errorMessage(e, 'Inscription impossible.')); this.loading.set(false); },
    });
  }
}
