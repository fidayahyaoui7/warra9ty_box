import { Pipe, PipeTransform } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

const nf = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 0, maximumFractionDigits: 3 });

/** 3399 -> "3 399 DT" */
export function dt(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '–';
  return `${nf.format(value).replace(/[\u202f\u00a0]/g, '\u00a0')}\u00a0DT`;
}

/** "2026-09-12" -> Date locale (sans décalage de fuseau) */
export function parseDate(iso: string | null | undefined): Date | null {
  if (!iso) return null;
  const [y, m, d] = iso.slice(0, 10).split('-').map(Number);
  if (!y || !m || !d) return null;
  return new Date(y, m - 1, d);
}

export function frDate(iso: string | null | undefined, style: 'short' | 'long' = 'short'): string {
  const d = parseDate(iso);
  if (!d) return '–';
  return style === 'long'
    ? d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
    : d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function toIso(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function todayIso(): string { return toIso(new Date()); }

/** RG1 (aperçu côté client) : dateAchat + durée en mois */
export function addMonthsIso(iso: string, months: number): string | null {
  const d = parseDate(iso);
  if (!d || !months) return null;
  const day = d.getDate();
  d.setDate(1);
  d.setMonth(d.getMonth() + months);
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(day, last));
  return toIso(d);
}

export function fileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} Ko`;
  return `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
}

export function fullName(u: { prenom?: string | null; nom?: string | null } | null | undefined): string {
  return u ? `${u.prenom ?? ''} ${u.nom ?? ''}`.trim() : '';
}

/** Message lisible à partir d'une erreur HTTP (le back renvoie {status, message}) */
export function errorMessage(e: unknown, fallback = 'Une erreur est survenue. Réessayez.'): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return 'Le serveur ne répond pas. Vérifiez que Spring Boot est démarré.';
    const body = e.error as { message?: string } | string | null;
    if (body && typeof body === 'object' && body.message) return body.message;
    if (typeof body === 'string' && body.length < 200 && body.trim()) return body;
    if (e.status === 413) return 'Fichier trop volumineux.';
  }
  return fallback;
}

@Pipe({ name: 'dt' })
export class DtPipe implements PipeTransform {
  transform(v: number | null | undefined): string { return dt(v); }
}

@Pipe({ name: 'frDate' })
export class FrDatePipe implements PipeTransform {
  transform(v: string | null | undefined, style: 'short' | 'long' = 'short'): string { return frDate(v, style); }
}
