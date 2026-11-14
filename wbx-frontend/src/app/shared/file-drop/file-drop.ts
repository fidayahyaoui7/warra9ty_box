import { Component, computed, input, model, signal } from '@angular/core';
import { fileSize } from '../../utils/format';
import { Icon } from '../icon/icon';

const RULES = {
  pdf: { mimes: ['application/pdf'], accept: 'application/pdf,.pdf', hint: 'PDF' },
  image: { mimes: ['image/jpeg', 'image/png'], accept: 'image/jpeg,image/png', hint: 'JPG ou PNG' },
};

/** Zone de dépôt : vérifie le type et la taille avant l'envoi (le serveur les revérifie). */
@Component({
  selector: 'app-file-drop',
  imports: [Icon],
  templateUrl: './file-drop.html',
  styleUrl: './file-drop.css',
})
export class FileDrop {
  label = input.required<string>();
  kind = input.required<'pdf' | 'image'>();
  maxMb = input.required<number>();
  required = input(false);
  existing = input<string | null>(null);
  file = model<File | null>(null);

  over = signal(false);
  error = signal<string | null>(null);
  preview = signal<string | null>(null);
  rule = computed(() => RULES[this.kind()]);
  size = fileSize;

  onOver(e: DragEvent) { e.preventDefault(); this.over.set(true); }
  onDrop(e: DragEvent) { e.preventDefault(); this.over.set(false); this.take(e.dataTransfer?.files?.[0] ?? null); }
  onPick(e: Event) {
    const input = e.target as HTMLInputElement;
    this.take(input.files?.[0] ?? null);
    input.value = '';
  }

  clear(e: Event) { e.preventDefault(); e.stopPropagation(); this.setFile(null); this.error.set(null); }

  private take(f: File | null) {
    if (!f) return;
    if (!this.rule().mimes.includes(f.type)) { this.error.set(`Format refusé : ${this.rule().hint} uniquement.`); return; }
    if (f.size > this.maxMb() * 1024 * 1024) { this.error.set(`Fichier trop volumineux (${fileSize(f.size)}). Maximum : ${this.maxMb()} Mo.`); return; }
    this.error.set(null);
    this.setFile(f);
  }

  private setFile(f: File | null) {
    const old = this.preview();
    if (old) URL.revokeObjectURL(old);
    this.preview.set(f && this.kind() === 'image' ? URL.createObjectURL(f) : null);
    this.file.set(f);
  }
}
