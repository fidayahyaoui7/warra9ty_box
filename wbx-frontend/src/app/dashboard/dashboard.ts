import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { AchatService } from '../services/achat.service';
import { ReclamationService } from '../services/reclamation.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';
import { DocViewerService } from '../services/doc-viewer.service';
import { FileService } from '../services/file.service';
import { FrDatePipe, dt, errorMessage, parseDate } from '../utils/format';
import { Achat, Reclamation } from '../models/models';
import { Badge } from '../shared/badge/badge';
import { Icon } from '../shared/icon/icon';
import { Ticket } from '../shared/ticket/ticket';

const DAY = 86_400_000;

interface Row { a: Achat; left: number; width: number; elapsed: number; tone: 'ok' | 'warn' | 'bad'; }

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, Icon, Badge, Ticket, FrDatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  auth = inject(AuthService);
  users = inject(UserService);
  docs = inject(DocViewerService);
  private achatApi = inject(AchatService);
  private claimApi = inject(ReclamationService);
  private files = inject(FileService);
  private router = inject(Router);

  loading = signal(true);
  error = signal<string | null>(null);
  achats = signal<Achat[]>([]);
  reclamations = signal<Reclamation[]>([]);

  money = dt;

  stats = computed(() => {
    const l = this.achats();
    const by = (s: string) => l.filter(a => a.statutGarantie === s).length;
    return { total: l.length, active: by('ACTIVE'), soon: by('BIENTOT_EXPIREE'), expired: by('EXPIREE'), spent: l.reduce((n, a) => n + a.prixPaye, 0) };
  });
  openClaims = computed(() => this.reclamations().filter(r => r.statut === 'OUVERTE' || r.statut === 'EN_COURS').length);
  inProgress = computed(() => this.reclamations().filter(r => r.statut === 'EN_COURS').length);
  recentClaims = computed(() => [...this.reclamations()].sort((a, b) => b.id - a.id).slice(0, 4));

  next = computed(() => {
    const live = this.achats().filter(a => a.statutGarantie !== 'EXPIREE' && a.statutGarantie !== 'INCONNU');
    return live.sort((a, b) => a.joursRestants - b.joursRestants)[0] ?? null;
  });

  summary = computed(() => {
    const s = this.stats();
    if (this.loading()) return 'Chargement de vos garanties…';
    if (s.total === 0) return 'Ajoutez votre premier achat pour suivre sa garantie.';
    if (s.soon > 0) return `${s.soon} ${s.soon > 1 ? 'garanties se terminent' : 'garantie se termine'} dans les 30 prochains jours.`;
    if (s.active === 0) return 'Toutes vos garanties sont arrivées à échéance.';
    return this.auth.isAdmin() ? 'Vue d’ensemble de tous les achats.' : 'Toutes vos garanties sont en règle.';
  });

  timeline = computed(() => {
    const list = this.achats().filter(a => parseDate(a.dateAchat) && parseDate(a.dateFinGarantie));
    const byEnd = (a: Achat) => parseDate(a.dateFinGarantie)!.getTime();
    const live = list.filter(a => a.statutGarantie !== 'EXPIREE').sort((a, b) => byEnd(a) - byEnd(b));
    const dead = list.filter(a => a.statutGarantie === 'EXPIREE').sort((a, b) => byEnd(b) - byEnd(a));
    const picked = [...live, ...dead].slice(0, 8);
    if (!picked.length) return null;

    const now = Date.now();
    const starts = picked.map(a => parseDate(a.dateAchat)!.getTime());
    const ends = picked.map(byEnd);
    const t0 = Math.min(...starts, now), t1 = Math.max(...ends, now);
    const pad = Math.max((t1 - t0) * 0.03, DAY);
    const min = t0 - pad, span = t1 + pad - min;
    const pct = (t: number) => ((t - min) / span) * 100;

    const rows: Row[] = picked.map(a => {
      const s = parseDate(a.dateAchat)!.getTime(), e = byEnd(a);
      return {
        a,
        left: pct(s),
        width: Math.max(pct(e) - pct(s), 1.5),
        elapsed: Math.round(Math.min(1, Math.max(0, (now - s) / (e - s))) * 100),
        tone: a.statutGarantie === 'EXPIREE' ? 'bad' : a.statutGarantie === 'BIENTOT_EXPIREE' ? 'warn' : 'ok',
      };
    });
    const today = pct(now);
    const ticks = [0, 1, 2, 3, 4]
      .map(i => ({ left: (i / 4) * 100, label: new Date(min + (span * i) / 4).toLocaleDateString('fr-FR', { month: 'short', year: '2-digit' }) }))
      .filter(k => Math.abs(k.left - today) > 14);   // évite le chevauchement avec « Aujourd’hui »
    return { rows, ticks, today };
  });

  constructor() { this.load(); }

  photo(a: Achat) { return this.files.produitUrl(a.produit.photo); }

  load() {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      achats: this.achatApi.list(),
      reclamations: this.claimApi.list(),
      users: this.auth.isAdmin() ? this.users.list().pipe(catchError(() => of([]))) : of([]),
    }).subscribe({
      next: r => { this.achats.set(r.achats); this.reclamations.set(r.reclamations); this.loading.set(false); },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  claim(a: Achat) { this.router.navigate(['/reclamations'], { queryParams: { achat: a.id } }); }
}
