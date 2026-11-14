import { Component, effect, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { DocViewerService } from '../../services/doc-viewer.service';
import { FileService } from '../../services/file.service';
import { Icon } from '../icon/icon';
import { Modal } from '../modal/modal';

@Component({
  selector: 'app-viewer-host',
  imports: [Modal, Icon],
  templateUrl: './viewer-host.html',
  styleUrl: './viewer-host.css',
})
export class ViewerHost {
  viewer = inject(DocViewerService);
  private files = inject(FileService);
  private sanitizer = inject(DomSanitizer);

  loading = signal(false);
  error = signal<string | null>(null);
  blob = signal<Blob | null>(null);
  raw = signal<string | null>(null);
  safe = signal<SafeResourceUrl | null>(null);

  constructor() {
    effect(onCleanup => {
      const d = this.viewer.doc();
      this.blob.set(null); this.raw.set(null); this.safe.set(null); this.error.set(null);
      if (!d) return;
      this.loading.set(true);
      let url: string | null = null;
      const sub = this.files.blob(d.path).subscribe({
        next: b => {
          url = URL.createObjectURL(b);
          this.blob.set(b);
          this.raw.set(url);
          this.safe.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
          this.loading.set(false);
        },
        error: () => { this.error.set('Impossible d’ouvrir ce fichier.'); this.loading.set(false); },
      });
      onCleanup(() => { sub.unsubscribe(); if (url) URL.revokeObjectURL(url); });
    });
  }

  download() {
    const b = this.blob(), d = this.viewer.doc();
    if (b && d) this.files.save(b, d.filename);
  }
}
