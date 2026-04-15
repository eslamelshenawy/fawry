import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';

import { GatewayApi } from '../../core/api/gateway.api';
import { Gateway } from '../../core/models';

@Component({
  selector: 'app-gateway-list',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  template: `
    <section class="stack">
      <h2>Gateways</h2>
      <p class="error" *ngIf="errorMessage()">{{ errorMessage() }}</p>
      <div class="card">
        <table>
          <thead>
          <tr>
            <th>Code</th><th>Name</th><th>Fixed Fee</th><th>% Fee</th>
            <th>Daily Limit</th><th>Min</th><th>Max</th><th>Processing</th><th>Active</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let g of gateways()">
            <td>{{ g.code }}</td>
            <td>{{ g.name }}</td>
            <td>{{ g.fixedFee | number:'1.2-2' }}</td>
            <td>{{ (g.percentageFee * 100) | number:'1.2-2' }}%</td>
            <td>{{ g.dailyLimit | number:'1.2-2' }}</td>
            <td>{{ g.minTransaction | number:'1.2-2' }}</td>
            <td>{{ g.maxTransaction ? (g.maxTransaction | number:'1.2-2') : '—' }}</td>
            <td>{{ formatProcessing(g.processingTimeMinutes) }}</td>
            <td>{{ g.active ? 'Yes' : 'No' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
  styles: [`h2 { margin: 0 0 0.5rem; }`]
})
export class GatewayListComponent {
  private readonly api = inject(GatewayApi);

  protected readonly gateways = signal<Gateway[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.api.list().subscribe({
      next: (rows) => this.gateways.set(rows),
      error: (err) =>
        this.errorMessage.set(err?.error?.message ?? 'Failed to load gateways')
    });
  }

  protected formatProcessing(minutes: number): string {
    if (minutes === 0) return 'Instant';
    if (minutes % 1440 === 0) return `${minutes / 1440 * 24} Hours`;
    if (minutes % 60 === 0)  return `${minutes / 60} Hours`;
    return `${minutes} Minutes`;
  }
}
