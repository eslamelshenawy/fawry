import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { TransactionApi } from '../../core/api/transaction.api';
import { AuthService } from '../../core/auth/auth.service';
import { TransactionHistory, TransactionView } from '../../core/models';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, CurrencyPipe, DatePipe, DecimalPipe],
  template: `
    <section class="stack">
      <h2>Transaction History</h2>

      <form class="card filters" [formGroup]="filterForm" (ngSubmit)="load()">
        <div>
          <label for="billerId">Biller ID</label>
          <input id="billerId" formControlName="billerId" placeholder="BILL_12345" />
        </div>
        <div>
          <label for="date">Date</label>
          <input id="date" type="date" formControlName="date" />
        </div>
        <div>
          <label for="gateway">Gateway</label>
          <select id="gateway" formControlName="gateway">
            <option value="">All gateways</option>
            <option *ngFor="let g of gatewayOptions()" [value]="g">{{ g }}</option>
          </select>
        </div>
        <button type="submit" [disabled]="loading()">
          {{ loading() ? 'Loading...' : 'Apply' }}
        </button>
      </form>

      <p class="error" *ngIf="errorMessage()">{{ errorMessage() }}</p>

      <ng-container *ngIf="history() as data">
        <div class="grid-3">
          <div class="stat">
            <div class="label">Total Amount</div>
            <div class="value">{{ data.totalAmount | currency:'EGP':'symbol':'1.2-2' }}</div>
          </div>
          <div class="stat">
            <div class="label">Total Commission</div>
            <div class="value">{{ data.totalCommission | currency:'EGP':'symbol':'1.2-2' }}</div>
          </div>
          <div class="stat">
            <div class="label">Transactions</div>
            <div class="value">{{ data.totalTransactions }}</div>
          </div>
        </div>

        <div class="card stack">
          <h3>Per-Gateway Breakdown</h3>
          <table>
            <thead>
            <tr>
              <th>Gateway</th><th>Txns</th><th>Amount</th><th>Commission</th>
              <th>Daily Limit</th><th>Quota Used</th><th>Quota Remaining</th>
            </tr>
            </thead>
            <tbody>
            <tr *ngFor="let row of data.breakdown">
              <td>{{ row.gatewayName }} ({{ row.gatewayCode }})</td>
              <td>{{ row.transactionCount }}</td>
              <td>{{ row.totalAmount | number:'1.2-2' }}</td>
              <td>{{ row.totalCommission | number:'1.2-2' }}</td>
              <td>{{ row.dailyLimit | number:'1.2-2' }}</td>
              <td>{{ row.quotaUsed | number:'1.2-2' }}</td>
              <td>{{ row.quotaRemaining | number:'1.2-2' }}</td>
            </tr>
            <tr *ngIf="data.breakdown.length === 0">
              <td colspan="7" class="muted">No transactions for the selected filters.</td>
            </tr>
            </tbody>
          </table>
        </div>

        <div class="card stack">
          <h3>Transactions</h3>
          <table>
            <thead>
            <tr>
              <th>Time</th><th>Gateway</th><th>Amount</th><th>Commission</th>
              <th>Urgency</th><th>Status</th><th>Split Group</th>
            </tr>
            </thead>
            <tbody>
            <tr *ngFor="let tx of filteredTransactions()">
              <td>{{ tx.createdAt | date:'short' }}</td>
              <td>{{ tx.gatewayName }} ({{ tx.gatewayCode }})</td>
              <td>{{ tx.amount | number:'1.2-2' }}</td>
              <td>{{ tx.commission | number:'1.2-2' }}</td>
              <td>{{ tx.urgency }}</td>
              <td>{{ tx.status }}</td>
              <td class="muted">{{ tx.splitGroupId ?? '—' }}</td>
            </tr>
            <tr *ngIf="filteredTransactions().length === 0">
              <td colspan="7" class="muted">No transactions.</td>
            </tr>
            </tbody>
          </table>
        </div>
      </ng-container>
    </section>
  `,
  styles: [`
    .filters {
      display: grid;
      grid-template-columns: 2fr 1fr 1fr auto;
      gap: 1rem;
      align-items: end;
    }
    h2 { margin: 0 0 0.5rem; }
    h3 { margin: 0 0 0.75rem; font-size: 1rem; }
  `]
})
export class TransactionHistoryComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(TransactionApi);
  private readonly auth = inject(AuthService);

  protected readonly history = signal<TransactionHistory | null>(null);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly filterForm = this.fb.nonNullable.group({
    billerId: [this.auth.currentUser()?.billerCode ?? 'BILL_12345'],
    date: [new Date().toISOString().substring(0, 10)],
    gateway: ['']
  });

  protected readonly gatewayOptions = computed(() => {
    const data = this.history();
    return data ? data.breakdown.map((b) => b.gatewayCode) : [];
  });

  protected readonly filteredTransactions = computed<TransactionView[]>(() => {
    const data = this.history();
    if (!data) {
      return [];
    }
    const selected = this.filterForm.controls.gateway.value;
    if (!selected) {
      return data.transactions;
    }
    return data.transactions.filter((tx) => tx.gatewayCode === selected);
  });

  constructor() {
    this.load();
  }

  load(): void {
    const { billerId, date } = this.filterForm.getRawValue();
    if (!billerId) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set(null);
    this.api.history(billerId, date || undefined).subscribe({
      next: (data) => {
        this.history.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message ?? 'Failed to load transactions');
        this.loading.set(false);
      }
    });
  }
}
