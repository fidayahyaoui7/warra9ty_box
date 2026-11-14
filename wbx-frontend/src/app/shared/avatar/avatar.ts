import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { FileService } from '../../services/file.service';

const TINTS = [
  ['#e8ebfc', '#1f2f9a'], ['#e5f5e5', '#0a650a'], ['#fff2d1', '#7d5200'],
  ['#fbe7e7', '#a02323'], ['#e7f1f8', '#14527a'], ['#efe8f8', '#5a2a8c'],
];

@Component({
  selector: 'app-avatar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './avatar.html',
  styleUrl: './avatar.css',
})
export class Avatar {
  user = input.required<{ prenom?: string | null; nom?: string | null; photoProfil?: string | null } | null>();
  size = input(36);
  private files = inject(FileService);

  photo = computed(() => this.files.profilUrl(this.user()?.photoProfil));
  initials = computed(() => {
    const u = this.user();
    return ((u?.prenom?.[0] ?? '') + (u?.nom?.[0] ?? '')).toUpperCase() || '?';
  });
  tint = computed(() => {
    const u = this.user();
    const s = `${u?.prenom ?? ''}${u?.nom ?? ''}`;
    let h = 0;
    for (const c of s) h = (h * 31 + c.charCodeAt(0)) >>> 0;
    return TINTS[h % TINTS.length];
  });
}
