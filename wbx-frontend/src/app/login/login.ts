import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { errorMessage } from '../utils/format';
import { AuthHero } from '../shared/auth-hero/auth-hero';
import { Icon } from '../shared/icon/icon';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthHero, Icon],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  loading = signal(false);
  error = signal<string | null>(null);
  show = signal(false);

  invalid(name: 'email' | 'password') { const c = this.form.controls[name]; return c.touched && c.invalid; }

  submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => this.router.navigate(['/tableau-de-bord']),
      error: e => { this.error.set(errorMessage(e, 'Connexion impossible.')); this.loading.set(false); },
    });
  }
}
