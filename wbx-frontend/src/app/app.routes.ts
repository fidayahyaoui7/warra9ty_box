import { Routes } from '@angular/router';
import {  authGuard, guestGuard, roleGuard  } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'connexion', canActivate: [guestGuard], loadComponent: () => import('./login/login').then(m => m.Login), title: 'Connexion · Warra9tyBox' },
  { path: 'inscription', canActivate: [guestGuard], loadComponent: () => import('./register/register').then(m => m.Register), title: 'Créer un compte · Warra9tyBox' },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shell/shell').then(m => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'tableau-de-bord' },
      { path: 'tableau-de-bord', loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard), title: 'Tableau de bord · Warra9tyBox' },
      { path: 'catalogue', loadComponent: () => import('./catalogue/catalogue').then(m => m.Catalogue), title: 'Catalogue · Warra9tyBox' },
      { path: 'achats', loadComponent: () => import('./achats/achats').then(m => m.Achats), title: 'Achats · Warra9tyBox' },
      { path: 'reclamations', loadComponent: () => import('./reclamations/reclamations').then(m => m.Reclamations), title: 'Réclamations · Warra9tyBox' },
      { path: 'profil', loadComponent: () => import('./profil/profil').then(m => m.Profil), title: 'Mon profil · Warra9tyBox' },
      { path: 'categories', canActivate: [roleGuard], data: { role: 'ADMIN' }, loadComponent: () => import('./categories/categories').then(m => m.Categories), title: 'Catégories · Warra9tyBox' },
      { path: 'utilisateurs', canActivate: [roleGuard], data: { role: 'ADMIN' }, loadComponent: () => import('./utilisateurs/utilisateurs').then(m => m.Utilisateurs), title: 'Utilisateurs · Warra9tyBox' },
    ],
  },
  { path: '**', redirectTo: 'tableau-de-bord' },
];
