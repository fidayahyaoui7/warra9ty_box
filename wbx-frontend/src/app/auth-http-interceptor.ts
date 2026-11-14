import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import {  environment  } from '../environments/environment';
import {  AuthService  } from './services/auth.service';
import {  ToastService  } from './services/toast.service';

/**
 * - ajoute le JWT à chaque requête vers l'API (sauf /auth/**)
 * - 401 : session expirée -> déconnexion
 * - 403 : accès refusé -> message
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const toast = inject(ToastService);
  const isApi = req.url.startsWith(environment.apiUrl);
  const isAuthCall = req.url.startsWith(`${environment.apiUrl}/auth/`);

  let request = req;
  const token = auth.token();
  if (isApi && !isAuthCall && token) {
    request = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      if (isApi && !isAuthCall) {
        if (err.status === 401 && auth.isLoggedIn()) {
          auth.logout('Votre session a expiré. Reconnectez-vous.');
        } else if (err.status === 403) {
          toast.error('Accès refusé : vous n’avez pas les droits pour cette action.');
        }
      }
      return throwError(() => err);
    }));
};
