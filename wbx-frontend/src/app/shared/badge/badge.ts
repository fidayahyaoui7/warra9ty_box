import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type Tone = 'ok' | 'warn' | 'bad' | 'info' | 'neutral';

const GARANTIE: Record<string, [string, Tone]> = {
  ACTIVE: ['Active', 'ok'],
  BIENTOT_EXPIREE: ['Bientôt expirée', 'warn'],
  EXPIREE: ['Expirée', 'bad'],
  INCONNU: ['Inconnue', 'neutral'],
};
const RECLAMATION: Record<string, [string, Tone]> = {
  OUVERTE: ['Ouverte', 'info'],
  EN_COURS: ['En cours', 'warn'],
  RESOLUE: ['Résolue', 'ok'],
  REFUSEE: ['Refusée', 'bad'],
};

/** Pastille de statut : couleur + libellé (jamais la couleur seule). */
@Component({
  selector: 'app-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './badge.html',
  styleUrl: './badge.css',
})
export class Badge {
  kind = input<'garantie' | 'reclamation'>('garantie');
  value = input.required<string>();
  info = computed<[string, Tone]>(() => (this.kind() === 'garantie' ? GARANTIE : RECLAMATION)[this.value()] ?? [this.value(), 'neutral']);
}
