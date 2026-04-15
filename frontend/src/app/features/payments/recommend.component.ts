import { Component, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PaymentApi } from '../../core/api/payment.api';
import { RecommendResponse, SplitResponse } from '../../core/models';

@Component({
  selector: 'app-recommend',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe],
  template: `
    <section class="stack">
      <h2>Payment Recommendation</h2>

      <form class="card form-grid" [formGroup]="form" (ngSubmit)="recommend()">
        <div>
          <label for="billerId">Biller ID</label>
          <input id="billerId" formControlName="billerId" />
        </div>
        <div>
          <label for="amount">Amount (EGP)</label>
          <input id="amount" type="number" step="0.01" formControlName="amount" />
        </div>
        <div>
          <label for="urgency">Urgency</label>
          <select id="urgency" formControlName="urgency">
            <option value="INSTANT">Instant</option>
            <option value="CAN_WAIT">Can wait</option>
          </select>
        </div>
        <div class="actions">
          <button type="submit" [disabled]="form.invalid || loading()">Recommend</button>
          <button type="button" class="ghost" [disabled]="form.invalid || loading()" (click)="split()">
            Split Payment
          </button>
        </div>
      </form>

      <p class="error" *ngIf="errorMessage()">{{ errorMessage() }}</p>

      <div class="card stack" *ngIf="recommendation() as rec">
        <h3>Recommended Gateway</h3>
        <p>
          <strong>{{ rec.recommendedGateway.name }}</strong> ({{ rec.recommendedGateway.id }})
          — Commission {{ rec.recommendedGateway.estimatedCommission | number:'1.2-2' }} EGP
          — {{ rec.recommendedGateway.processingTime }}
        </p>
        <h4>Alternatives</h4>
        <table>
          <thead><tr><th>Gateway</th><th>Commission</th><th>Processing Time</th></tr></thead>
          <tbody>
          <tr *ngFor="let alt of rec.alternatives">
            <td>{{ alt.name }} ({{ alt.id }})</td>
            <td>{{ alt.estimatedCommission | number:'1.2-2' }}</td>
            <td>{{ alt.processingTime }}</td>
          </tr>
          <tr *ngIf="rec.alternatives.length === 0">
            <td colspan="3" class="muted">No alternatives.</td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="card stack" *ngIf="splitResult() as split">
        <h3>Split Result</h3>
        <p>
          Gateway <strong>{{ split.gatewayName }}</strong> ({{ split.selectedGateway }}).
          {{ split.requiresSplitting ? 'Payment was split into ' + split.splitCount + ' chunks.' : 'Processed as a single transaction.' }}
        </p>
        <ul>
          <li *ngFor="let s of split.splits">{{ s | number:'1.2-2' }} EGP</li>
        </ul>
        <p>Total commission: <strong>{{ split.totalCommission | number:'1.2-2' }} EGP</strong></p>
      </div>
    </section>
  `,
  styles: [`
    .form-grid { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 1rem; align-items: end; }
    .actions { display: flex; gap: 0.5rem; }
    h2 { margin: 0 0 0.5rem; }
    h3 { margin: 0 0 0.5rem; }
    h4 { margin: 0.75rem 0 0.25rem; font-size: 0.9rem; color: var(--color-text-muted); }
  `]
})
export class RecommendComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PaymentApi);

  protected readonly recommendation = signal<RecommendResponse | null>(null);
  protected readonly splitResult = signal<SplitResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    billerId: ['BILL_12345', Validators.required],
    amount: [1000, [Validators.required, Validators.min(0.01)]],
    urgency: ['INSTANT' as 'INSTANT' | 'CAN_WAIT', Validators.required]
  });

  recommend(): void {
    this.execute((req) => this.api.recommend(req).subscribe({
      next: (res) => {
        this.recommendation.set(res);
        this.splitResult.set(null);
        this.loading.set(false);
      },
      error: (err) => this.handleError(err)
    }));
  }

  split(): void {
    this.execute((req) => this.api.split(req).subscribe({
      next: (res) => {
        this.splitResult.set(res);
        this.recommendation.set(null);
        this.loading.set(false);
      },
      error: (err) => this.handleError(err)
    }));
  }

  private execute(action: (request: { billerId: string; amount: number; urgency: 'INSTANT' | 'CAN_WAIT' }) => void): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    action(this.form.getRawValue());
  }

  private handleError(err: unknown): void {
    const message = (err as { error?: { message?: string } })?.error?.message ?? 'Request failed';
    this.errorMessage.set(message);
    this.loading.set(false);
  }
}
