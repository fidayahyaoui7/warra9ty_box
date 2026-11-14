import { Component, DestroyRef, HostListener, inject, input, output } from '@angular/core';
import { Icon } from '../icon/icon';

let openCount = 0;
const stack: object[] = [];

@Component({
  selector: 'app-modal',
  imports: [Icon],
  templateUrl: './modal.html',
  styleUrl: './modal.css',
})
export class Modal {
  title = input.required<string>();
  size = input<'md' | 'wide' | 'xl'>('md');
  closed = output<void>();

  constructor() {
    openCount++;
    stack.push(this);
    document.body.classList.add('is-locked');
    inject(DestroyRef).onDestroy(() => {
      openCount--;
      stack.splice(stack.indexOf(this), 1);
      if (openCount <= 0) document.body.classList.remove('is-locked');
    });
  }

  // Échap ne ferme que la modale du dessus
  @HostListener('document:keydown.escape')
  onEscape() { if (stack[stack.length - 1] === this) this.closed.emit(); }

  onBackdrop(e: MouseEvent) { if (e.target === e.currentTarget) this.closed.emit(); }
}
