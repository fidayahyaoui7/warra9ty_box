import { Injectable, signal } from '@angular/core';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly current = signal<(ConfirmOptions & { resolve: (ok: boolean) => void }) | null>(null);

  ask(opts: ConfirmOptions): Promise<boolean> {
    return new Promise(resolve => this.current.set({ ...opts, resolve }));
  }

  answer(ok: boolean) {
    const c = this.current();
    this.current.set(null);
    c?.resolve(ok);
  }
}
