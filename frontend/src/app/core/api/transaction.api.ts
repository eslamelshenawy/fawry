import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TransactionHistory } from '../models';

@Injectable({ providedIn: 'root' })
export class TransactionApi {
  private readonly http = inject(HttpClient);

  history(billerId: string, date?: string): Observable<TransactionHistory> {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }
    return this.http.get<TransactionHistory>(
      `${environment.apiBaseUrl}/billers/${billerId}/transactions`,
      { params }
    );
  }
}
