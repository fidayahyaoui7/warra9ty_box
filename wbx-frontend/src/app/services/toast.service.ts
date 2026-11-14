import { Injectable, signal } from '@angular/core';

export interface Toast { id: number; kind: 'success' | 'error' | 'info'; text: string; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private seq = 0;

  success(text: string) { this.push('success', text); }
  error(text: string) { this.push('error', text, 7000); }
  info(text: string) { this.push('info', text); }

  dismiss(id: number) { this.toasts.update(l => l.filter(t => t.id !== id)); }

  private push(kind: Toast['kind'], text: string, ms = 4500) {
    const id = ++this.seq;
    this.toasts.update(l => [...l.slice(-3), { id, kind, text }]);
    setTimeout(() => this.dismiss(id), ms);
  }
}
