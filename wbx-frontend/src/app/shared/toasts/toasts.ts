import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-toasts',
  imports: [Icon],
  templateUrl: './toasts.html',
  styleUrl: './toasts.css',
})
export class Toasts { toast = inject(ToastService); }
