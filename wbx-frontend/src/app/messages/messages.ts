import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, interval, of } from 'rxjs';
import { Conversation, Message, Reclamation, User } from '../models/models';
import { AuthService } from '../services/auth.service';
import { MessageService } from '../services/message.service';
import { ReclamationService } from '../services/reclamation.service';
import { ToastService } from '../services/toast.service';
import { UserService } from '../services/user.service';
import { frTime, groupByDay, listStamp } from '../utils/chat';
import { errorMessage, fullName } from '../utils/format';
import { Avatar } from '../shared/avatar/avatar';
import { Icon } from '../shared/icon/icon';
import { Modal } from '../shared/modal/modal';

const MAX = 2000;
const REFRESH_MS = 15000;

@Component({
  selector: 'app-messages',
  imports: [Icon, Avatar, Modal],
  templateUrl: './messages.html',
  styleUrl: './messages.css',
})
export class Messages {
  auth = inject(AuthService);
  private api = inject(MessageService);
  private usersApi = inject(UserService);
  private claimApi = inject(ReclamationService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  readonly max = MAX;
  admin = this.auth.isAdmin;

  loading = signal(true);
  error = signal<string | null>(null);

  conversations = signal<Conversation[]>([]);
  clients = signal<User[]>([]);            // admin : tous les comptes clients actifs
  claims = signal<Reclamation[]>([]);
  selected = signal<number | null>(null);  // admin : client de la conversation ouverte
  thread = signal<Message[]>([]);
  threadLoading = signal(false);

  query = signal('');
  draft = signal('');
  about = signal<number | null>(null);     // réclamation jointe au prochain message
  sending = signal(false);

  newOpen = signal(false);
  newClient = signal<number | null>(null);

  private scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  groups = computed(() => groupByDay(this.thread()));
  remaining = computed(() => MAX - this.draft().length);
  canSend = computed(() => this.draft().trim().length > 0 && this.draft().length <= MAX && !this.sending());

  /** Client de la conversation ouverte (admin) : depuis la boîte de réception, sinon depuis la liste des comptes */
  current = computed<{ id: number; prenom: string; nom: string; email: string; photoProfil?: string | null } | null>(() => {
    const id = this.selected();
    if (id === null) return null;
    const c = this.conversations().find(x => x.clientId === id);
    if (c) return { id, prenom: c.prenom, nom: c.nom, email: c.email, photoProfil: c.photoProfil };
    const u = this.clients().find(x => x.id === id);
    return u ? { id, prenom: u.prenom, nom: u.nom, email: u.email, photoProfil: u.photoProfil } : null;
  });

  filteredConversations = computed(() => {
    const q = this.query().trim().toLowerCase();
    return this.conversations().filter(c => !q || `${c.prenom} ${c.nom} ${c.email}`.toLowerCase().includes(q));
  });

  // réclamations proposées dans le sélecteur « À propos de » : celles du client de la conversation
  aboutChoices = computed(() => {
    const id = this.admin() ? this.selected() : this.auth.session()?.id;
    return this.claims().filter(r => r.achat.user.id === id).sort((a, b) => b.id - a.id);
  });

  newChoices = computed(() => this.clients().filter(u => u.enabled));

  constructor() {
    this.load();
    interval(REFRESH_MS).pipe(takeUntilDestroyed()).subscribe(() => { if (!document.hidden) this.refresh(); });
  }

  name = fullName;
  time = frTime;
  stamp = listStamp;
  claimOf(id: number | null | undefined) { return id ? this.claims().find(r => r.id === id) : undefined; }

  load() {
    this.loading.set(true);
    this.error.set(null);
    const admin = this.admin();
    forkJoin({
      claims: this.claimApi.list(),
      conv: admin ? this.api.conversations() : of([] as Conversation[]),
      users: admin ? this.usersApi.list() : of([] as User[]),
    }).subscribe({
      next: x => {
        this.claims.set(x.claims);
        this.conversations.set(x.conv);
        this.clients.set(x.users.filter(u => u.roles.some(r => r.libelle === 'CLIENT')));
        this.loading.set(false);

        const qp = this.route.snapshot.queryParamMap;
        const rec = Number(qp.get('reclamation')) || null;
        const cli = Number(qp.get('client')) || null;
        if (qp.keys.length) this.router.navigate([], { queryParams: {}, replaceUrl: true });
        if (admin) { if (cli) this.select(cli, rec); }
        else { this.loadThread(); if (rec) this.about.set(rec); }
      },
      error: e => { this.error.set(errorMessage(e)); this.loading.set(false); },
    });
  }

  select(clientId: number | null, rec: number | null = null) {
    this.selected.set(clientId);
    this.thread.set([]);
    this.draft.set('');
    this.about.set(rec);
    if (clientId !== null) this.loadThread();
  }

  /** Recharge la conversation ouverte ; silent = rafraîchissement automatique (pas de squelette) */
  private loadThread(silent = false) {
    const id = this.admin() ? this.selected() : this.auth.session()?.id ?? null;
    if (id === null) return;
    if (!silent) this.threadLoading.set(true);
    const req = this.admin() ? this.api.thread(id) : this.api.mine();
    req.subscribe({
      next: list => {
        if (this.admin() && this.selected() !== id) return;     // on a changé de conversation entre-temps
        const grew = list.length !== this.thread().length;
        this.thread.set(list);
        this.threadLoading.set(false);
        if (grew || !silent) this.scrollDown();
        this.markReadIfNeeded(id, list);
      },
      error: e => { this.threadLoading.set(false); if (!silent) this.toast.error(errorMessage(e)); },
    });
  }

  private markReadIfNeeded(id: number, list: Message[]) {
    const incoming = list.some(m => !m.lu && m.deAdmin !== this.admin());
    if (!incoming) return;
    const done = this.admin() ? this.api.markReadAdmin(id) : this.api.markRead();
    done.subscribe({
      next: () => {
        this.thread.update(l => l.map(m => (m.deAdmin !== this.admin() ? { ...m, lu: true } : m)));
        this.conversations.update(l => l.map(c => (c.clientId === id ? { ...c, nonLus: 0 } : c)));
        this.api.refreshUnread();
      },
      error: () => undefined,
    });
  }

  refresh() {
    if (this.loading() || this.sending()) return;
    if (this.admin()) this.api.conversations().subscribe({ next: c => this.conversations.set(c), error: () => undefined });
    if (!this.admin() || this.selected() !== null) this.loadThread(true);
    this.api.refreshUnread();
  }

  send() {
    if (!this.canSend()) return;
    const text = this.draft().trim();
    const id = this.admin() ? this.selected() : this.auth.session()?.id ?? null;
    if (id === null) return;
    this.sending.set(true);
    const req = this.admin() ? this.api.sendTo(id, text, this.about()) : this.api.send(text, this.about());
    req.subscribe({
      next: m => {
        this.thread.update(l => [...l, m]);
        this.draft.set('');
        this.about.set(null);
        this.sending.set(false);
        this.scrollDown();
        if (this.admin()) this.api.conversations().subscribe({ next: c => this.conversations.set(c), error: () => undefined });
      },
      error: e => { this.sending.set(false); this.toast.error(errorMessage(e)); },
    });
  }

  onEnter(e: Event) {
    const k = e as KeyboardEvent;
    if (k.shiftKey || k.isComposing) return;      // Maj+Entrée : retour à la ligne
    k.preventDefault();
    this.send();
  }

  openNew() { this.newClient.set(null); this.newOpen.set(true); }

  startNew() {
    const id = this.newClient();
    if (id === null) return;
    this.newOpen.set(false);
    this.select(id);
  }

  private scrollDown() {
    setTimeout(() => { const el = this.scroller()?.nativeElement; if (el) el.scrollTop = el.scrollHeight; }, 0);
  }
}
