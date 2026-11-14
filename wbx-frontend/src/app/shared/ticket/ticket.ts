import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Achat } from '../../models/models';
import { FileService } from '../../services/file.service';
import { DtPipe, FrDatePipe, fullName, parseDate } from '../../utils/format';
import { Badge } from '../badge/badge';
import { Icon } from '../icon/icon';

/** Un achat sous forme de ticket : infos à gauche, compte à rebours de garantie sur le talon. */
@Component({
  selector: 'app-ticket',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Badge, Icon, DtPipe, FrDatePipe],
  templateUrl: './ticket.html',
  styleUrl: './ticket.css',
})
export class Ticket {
  achat = input.required<Achat>();
  variant = input<'row' | 'hero'>('row');
  showOwner = input(false);

  viewInvoice = output<Achat>();
  download = output<Achat>();
  viewPhoto = output<Achat>();
  claim = output<Achat>();
  edit = output<Achat>();
  remove = output<Achat>();

  private files = inject(FileService);
  img = computed(() => this.files.produitUrl(this.achat().produit.photo));
  owner = computed(() => fullName(this.achat().user));

  count = computed(() => Math.abs(this.achat().joursRestants));
  unit = computed(() => {
    const a = this.achat();
    const n = Math.abs(a.joursRestants);
    const j = n > 1 ? 'jours' : 'jour';
    if (a.statutGarantie === 'EXPIREE') return `${j} depuis la fin`;
    return a.joursRestants === 0 ? 'dernier jour' : `${j} restants`;
  });

  /** Part de la garantie déjà écoulée, en % */
  elapsed = computed(() => {
    const a = this.achat();
    const s = parseDate(a.dateAchat)?.getTime();
    const e = parseDate(a.dateFinGarantie)?.getTime();
    if (!s || !e || e <= s) return 100;
    return Math.round(Math.min(1, Math.max(0, (Date.now() - s) / (e - s))) * 100);
  });
}
