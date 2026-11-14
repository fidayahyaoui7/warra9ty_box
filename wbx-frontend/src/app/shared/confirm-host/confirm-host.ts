import { Component, inject } from '@angular/core';
import { ConfirmService } from '../../services/confirm.service';
import { Modal } from '../modal/modal';

@Component({
  selector: 'app-confirm-host',
  imports: [Modal],
  templateUrl: './confirm-host.html',
  styleUrl: './confirm-host.css',
})
export class ConfirmHost { confirm = inject(ConfirmService); }
