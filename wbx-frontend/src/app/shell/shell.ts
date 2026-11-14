import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserService } from '../services/user.service';
import { fullName } from '../utils/format';
import { Avatar } from '../shared/avatar/avatar';
import { Icon } from '../shared/icon/icon';

interface Item { path: string; label: string; icon: string; }

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, Avatar],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  auth = inject(AuthService);
  users = inject(UserService);
  private router = inject(Router);

  open = signal(false);
  name = computed(() => fullName(this.auth.session()) || this.auth.session()?.email || '');

  main = computed<Item[]>(() => {
    const admin = this.auth.isAdmin();
    return [
      { path: '/tableau-de-bord', label: 'Tableau de bord', icon: 'dashboard' },
      { path: '/catalogue', label: 'Catalogue', icon: 'box' },
      { path: '/achats', label: admin ? 'Achats' : 'Mes achats', icon: 'receipt' },
      { path: '/reclamations', label: admin ? 'Réclamations' : 'Mes réclamations', icon: 'lifebuoy' },
    ];
  });
  admin = computed<Item[]>(() => [
    { path: '/categories', label: 'Catégories', icon: 'tag' },
    { path: '/utilisateurs', label: 'Utilisateurs', icon: 'users' },
  ]);

  constructor() {
    if (this.auth.isAdmin()) this.users.list().subscribe({ error: () => undefined });
  }

  logout() { this.auth.logout(); }
}
