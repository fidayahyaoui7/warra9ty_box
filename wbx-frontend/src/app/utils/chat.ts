import { Message } from '../models/models';

/** "2026-11-18T23:24:11" -> Date locale */
export function parseDateTime(iso: string): Date {
  return new Date(iso);
}

export function frTime(iso: string): string {
  return parseDateTime(iso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

/** Libellé du séparateur de jour : Aujourd'hui, Hier, ou « mercredi 18 novembre » */
export function dayLabel(iso: string): string {
  const d = parseDateTime(iso);
  const now = new Date();
  const yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
  if (sameDay(d, now)) return 'Aujourd’hui';
  if (sameDay(d, yesterday)) return 'Hier';
  return d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', ...(d.getFullYear() !== now.getFullYear() ? { year: 'numeric' } : {}) });
}

/** Date courte pour la liste des conversations : heure si aujourd'hui, sinon jj/mm */
export function listStamp(iso: string): string {
  const d = parseDateTime(iso);
  return sameDay(d, new Date()) ? frTime(iso) : d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
}

export interface DayGroup { label: string; items: Message[]; }

export function groupByDay(list: Message[]): DayGroup[] {
  const out: DayGroup[] = [];
  for (const m of list) {
    const label = dayLabel(m.dateEnvoi);
    const last = out[out.length - 1];
    if (last && last.label === label) last.items.push(m); else out.push({ label, items: [m] });
  }
  return out;
}
