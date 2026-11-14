import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import {  AuthService  } from '../services/auth.service';
import {  ToastService  } from '../services/toast.service';
import {  RoleName  } from '../models/models';

/** Il faut être connecté, sinon retour à la page de connexion. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/connexion']);
};

/** Pages réservées à un rôle (route.data.role). */
export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService);
  const role = route.data['role'] as RoleName;
  if (auth.hasRole(role)) return true;
  inject(ToastService).error('Cette page est réservée aux administrateurs.');
  return inject(Router).createUrlTree(['/tableau-de-bord']);
};

/** Connexion / inscription : inutiles si on est déjà connecté. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? inject(Router).createUrlTree(['/tableau-de-bord']) : true;
};
