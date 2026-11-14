import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmHost } from './shared/confirm-host/confirm-host';
import { Toasts } from './shared/toasts/toasts';
import { ViewerHost } from './shared/viewer-host/viewer-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toasts, ConfirmHost, ViewerHost],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}
