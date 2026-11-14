import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { FileService } from '../../services/file.service';
import { Icon } from '../icon/icon';

/** Image protégée par le JWT : téléchargée avec le token puis affichée via un blob. */
@Component({
  selector: 'app-secure-img',
  imports: [Icon],
  host: { style: 'display:contents' },
  templateUrl: './secure-img.html',
  styleUrl: './secure-img.css',
})
export class SecureImg {
  path = input<string | null>(null);
  alt = input('');
  url = signal<string | null>(null);
  private files = inject(FileService);

  constructor() {
    effect(onCleanup => {
      const p = this.path();
      this.url.set(null);
      if (!p) return;
      let objectUrl: string | null = null;
      const sub = this.files.blob(p).subscribe({
        next: b => { objectUrl = URL.createObjectURL(b); this.url.set(objectUrl); },
        error: () => this.url.set(null),
      });
      onCleanup(() => { sub.unsubscribe(); if (objectUrl) URL.revokeObjectURL(objectUrl); });
    });
    inject(DestroyRef).onDestroy(() => this.url.set(null));
  }
}
